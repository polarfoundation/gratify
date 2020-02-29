package foundation.polar.gratify.annotations.utils;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import foundation.polar.gratify.annotations.checker.AnnotatedFor;
import foundation.polar.gratify.annotations.checker.SupportedLintOptions;
import foundation.polar.gratify.annotations.checker.SupportedOptions;
import foundation.polar.gratify.annotations.checker.SuppressWarningsKeys;
import foundation.polar.gratify.annotations.checker.compilermsgs.CompilerMessageKey;
import foundation.polar.gratify.lang.Nullable;
import foundation.polar.gratify.utils.compiler.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;

@SupportedOptions({
   // When adding a new standard option:
   // 1. Add a brief blurb here about the use case
   //    and a pointer to one prominent use of the option.
   // 2. Update the Checker Framework manual:
   //     * docs/manual/introduction.tex contains a list of all options,
   //       which should be in the same order as this source code file.
   //     * a specific section should contain a detailed discussion.

   ///
   /// Unsound checking: ignore some errors
   ///

   // A comma-separated list of warnings to suppress
   // foundation.polar.gratify.annotations.utils.SourceChecker.createSuppressWarnings
   "suppressWarnings",

   // Set inclusion/exclusion of type uses or definitions
   // foundation.polar.gratify.annotations.utils.SourceChecker.shouldSkipUses and similar
   "skipUses",
   "onlyUses",
   "skipDefs",
   "onlyDefs",

   // Unsoundly assume all methods have no side effects, are deterministic, or both.
   "assumeSideEffectFree",
   "assumeDeterministic",
   "assumePure"
})
public abstract class SourceChecker extends AbstractTypeProcessor
   implements CheckContext, OptionConfiguration {
   private static boolean gitPropertiesPrinted = false;
   public static final String SUPPRESS_ALL_KEY = "all";
   public static final @CompilerMessageKey String UNNEEDED_SUPPRESSION_KEY =
      "unneeded.suppression";
   protected static final String MSGS_FILE = "messages.properties";
   protected Properties messages;
   protected Messager messager;
   protected Trees trees;
   protected CompilationUnitTree currentRoot;
   private CompilationUnitTree previousErrorCompilationUnit;
   protected SourceVisitor<?, ?> visitor;
   private String @Nullable [] suppressWarnings;
   private Pattern skipUsesPattern;
   private Pattern onlyUsesPattern;
   private Pattern skipDefsPattern;
   private Pattern onlyDefsPattern;
   private Set<String> supportedLints;
   private Set<String> activeLints;
   private Map<String, String> activeOptions;
   private static final String OPTION_SEPARATOR = "_";
   private static final String LINE_SEPARATOR = System.lineSeparator().intern();
   protected SourceChecker parentChecker;
   protected List<String> upstreamCheckerNames;

   @Override
   public final synchronized void init(ProcessingEnvironment env) {
      super.init(env);
      // The processingEnvironment field will also be set by the superclass' init method.
      // This is used to trigger AggregateChecker's setProcessingEnvironment.
      setProcessingEnvironment(env);

      // Keep in sync with check in checker-framework/build.gradle and text in installation
      // section of manual.
      int jreVersion = PluginUtil.getJreVersion();
      if (jreVersion < 8) {
         throw new UserError(
            "The Checker Framework must be run under at least JDK 8.  You are using version %d.  Please use JDK 8 or JDK 11.",
            jreVersion);
      } else if (jreVersion > 12) {
         throw new UserError(
            String.format(
               "The Checker Framework cannot be run with JDK 13+.  You are using version %d. Please use JDK 8 or JDK 11.",
               jreVersion));
      } else if (jreVersion != 8 && jreVersion != 11) {
         message(
            Diagnostic.Kind.WARNING,
            "The Checker Framework is only tested with JDK 8 and JDK 11. You are using version %d. Please use JDK 8 or JDK 11.",
            jreVersion);
      }

      if (hasOption("printGitProperties")) {
         printGitProperties();
      }
   }

   @Override
   public ProcessingEnvironment getProcessingEnvironment() {
      return this.processingEnv;
   }

   protected void setProcessingEnvironment(ProcessingEnvironment env) {
      this.processingEnv = env;
   }

   protected void setParentChecker(SourceChecker parentChecker) {
      this.parentChecker = parentChecker;
   }

   protected void setRoot(CompilationUnitTree newRoot) {
      this.currentRoot = newRoot;
      visitor.setRoot(currentRoot);
   }

   public List<String> getUpstreamCheckerNames() {
      if (upstreamCheckerNames == null) {
         upstreamCheckerNames = new ArrayList<>();

         SourceChecker checker = this;

         while (checker != null) {
            upstreamCheckerNames.add(checker.getClass().getName());
            checker = checker.parentChecker;
         }
      }

      return upstreamCheckerNames;
   }

   public ProcessContext getContext() {
      return this;
   }

   @Override
   public SourceChecker getChecker() {
      return this;
   }

   @Override
   public OptionConfiguration getOptionConfiguration() {
      return this;
   }

   @Override
   public Elements getElementUtils() {
      return getProcessingEnvironment().getElementUtils();
   }

   @Override
   public Types getTypeUtils() {
      return getProcessingEnvironment().getTypeUtils();
   }

   @Override
   public Trees getTreeUtils() {
      return Trees.instance(getProcessingEnvironment());
   }

   @Override
   public SourceVisitor<?, ?> getVisitor() {
      return this.visitor;
   }

   /**
    * Provides the {@link SourceVisitor} that the checker should use to scan input source trees.
    *
    * @return a {@link SourceVisitor} to use to scan source trees
    */
   protected abstract SourceVisitor<?, ?> createSourceVisitor();

   @Override
   public AnnotationProvider getAnnotationProvider() {
      throw new UnsupportedOperationException(
         "getAnnotationProvider is not implemented for this class.");
   }

   public Properties getMessages() {
      if (this.messages != null) {
         return this.messages;
      }

      this.messages = new Properties();
      ArrayDeque<Class<?>> checkers = new ArrayDeque<>();

      Class<?> currClass = this.getClass();
      while (currClass != SourceChecker.class) {
         checkers.addFirst(currClass);
         currClass = currClass.getSuperclass();
      }
      checkers.addFirst(SourceChecker.class);

      while (!checkers.isEmpty()) {
         messages.putAll(getProperties(checkers.removeFirst(), MSGS_FILE));
      }
      return this.messages;
   }

   private Pattern getSkipPattern(String patternName, Map<String, String> options) {
      // Default is an illegal Java identifier substring
      // so that it won't match anything.
      // Note that AnnotatedType's toString output format contains characters such as "():{}".
      return getPattern(patternName, options, "\\]'\"\\]");
   }

   private Pattern getOnlyPattern(String patternName, Map<String, String> options) {
      // default matches everything
      return getPattern(patternName, options, ".");
   }

   private Pattern getPattern(
      String patternName, Map<String, String> options, String defaultPattern) {
      String pattern = "";

      if (options.containsKey(patternName)) {
         pattern = options.get(patternName);
         if (pattern == null) {
            message(
               Diagnostic.Kind.WARNING,
               "The " + patternName + " property is empty; please fix your command line");
            pattern = "";
         }
      } else if (System.getProperty("checkers." + patternName) != null) {
         pattern = System.getProperty("checkers." + patternName);
      } else if (System.getenv(patternName) != null) {
         pattern = System.getenv(patternName);
      }

      if (pattern.indexOf("/") != -1) {
         message(
            Diagnostic.Kind.WARNING,
            "The "
               + patternName
               + " property contains \"/\", which will never match a class name: "
               + pattern);
      }

      if (pattern.equals("")) {
         pattern = defaultPattern;
      }

      return Pattern.compile(pattern);
   }

   private Pattern getSkipUsesPattern(Map<String, String> options) {
      return getSkipPattern("skipUses", options);
   }

   private Pattern getOnlyUsesPattern(Map<String, String> options) {
      return getOnlyPattern("onlyUses", options);
   }

   private Pattern getSkipDefsPattern(Map<String, String> options) {
      return getSkipPattern("skipDefs", options);
   }

   private Pattern getOnlyDefsPattern(Map<String, String> options) {
      return getOnlyPattern("onlyDefs", options);
   }

   // TODO: do we want this?
   // Cache the keys that we already warned about to prevent repetitions.
   // private Set<String> warnedOnLint = new HashSet<>();

   private Set<String> createActiveLints(Map<String, String> options) {
      if (!options.containsKey("lint")) {
         return Collections.emptySet();
      }

      String lintString = options.get("lint");
      if (lintString == null) {
         return Collections.singleton("all");
      }

      Set<String> activeLint = new HashSet<>();
      for (String s : lintString.split(",")) {
         if (!this.getSupportedLintOptions().contains(s)
            && !(s.charAt(0) == '-'
            && this.getSupportedLintOptions().contains(s.substring(1)))
            && !s.equals("all")
            && !s.equals("none") /*&&
                    !warnedOnLint.contains(s)*/) {
            this.messager.printMessage(
               javax.tools.Diagnostic.Kind.WARNING,
               "Unsupported lint option: "
                  + s
                  + "; All options: "
                  + this.getSupportedLintOptions());
            // warnedOnLint.add(s);
         }

         activeLint.add(s);
         if (s.equals("none")) {
            activeLint.add("-all");
         }
      }

      return Collections.unmodifiableSet(activeLint);
   }

   private Map<String, String> createActiveOptions(Map<String, String> options) {
      if (options.isEmpty()) {
         return Collections.emptyMap();
      }

      Map<String, String> activeOpts = new HashMap<>();

      for (Map.Entry<String, String> opt : options.entrySet()) {
         String key = opt.getKey();
         String value = opt.getValue();

         String[] split = key.split(OPTION_SEPARATOR);

         switch (split.length) {
            case 1:
               // No separator, option always active
               activeOpts.put(key, value);
               break;
            case 2:
               // Valid class-option pair
               Class<?> clazz = this.getClass();

               do {
                  if (clazz.getCanonicalName().equals(split[0])
                     || clazz.getSimpleName().equals(split[0])) {
                     activeOpts.put(split[1], value);
                  }

                  clazz = clazz.getSuperclass();
               } while (clazz != null
                  && !clazz.getName()
                  .equals(AbstractTypeProcessor.class.getCanonicalName()));
               break;
            default:
               throw new UserError(
                  "Invalid option name: "
                     + key
                     + " At most one separator "
                     + OPTION_SEPARATOR
                     + " expected, but found "
                     + split.length
                     + ".");
         }
      }
      return Collections.unmodifiableMap(activeOpts);
   }

   /** Only ever called once; the value is cached in field {@link #suppressWarnings}. */
   private String @Nullable [] createSuppressWarnings(Map<String, String> options) {
      if (!options.containsKey("suppressWarnings")) {
         return null;
      }

      String swString = options.get("suppressWarnings");
      if (swString == null) {
         return null;
      }

      return arrayToLowerCase(swString.split(","));
   }

   private static String[] arrayToLowerCase(String[] a) {
      for (int i = 0; i < a.length; i++) {
         a[i] = a[i].toLowerCase();
      }
      return a;
   }

   private void logUserError(UserError ce) {
      String msg = ce.getMessage();
      printMessage(msg);
   }

   private void logBugInCF(RuntimeException ce) {
      StringJoiner msg = new StringJoiner(LINE_SEPARATOR);
      msg.add(ce.getMessage());
      boolean noPrintErrorStack =
         (processingEnv != null
            && processingEnv.getOptions() != null
            && processingEnv.getOptions().containsKey("noPrintErrorStack"));

      msg.add("; The Checker Framework crashed.  Please report the crash.");
      if (noPrintErrorStack) {
         msg.add(
            " To see the full stack trace, don't invoke the compiler with -AnoPrintErrorStack");
      } else {
         if (this.currentRoot != null && this.currentRoot.getSourceFile() != null) {
            msg.add("Compilation unit: " + this.currentRoot.getSourceFile().getName());
         }

         if (this.visitor != null) {
            JCDiagnostic.DiagnosticPosition pos = (JCDiagnostic.DiagnosticPosition) this.visitor.lastVisited;
            if (pos != null) {
               DiagnosticSource source =
                  new DiagnosticSource(this.currentRoot.getSourceFile(), null);
               int linenr = source.getLineNumber(pos.getStartPosition());
               int col = source.getColumnNumber(pos.getStartPosition(), true);
               String line = source.getLine(pos.getStartPosition());

               msg.add("Last visited tree at line " + linenr + " column " + col + ":");
               msg.add(line);
            }
         }

         msg.add(
            "Exception: "
               + ce.getCause()
               + "; "
               + formatStackTrace(ce.getCause().getStackTrace()));
         boolean printClasspath = ce.getCause() instanceof NoClassDefFoundError;
         Throwable cause = ce.getCause().getCause();
         while (cause != null) {
            msg.add(
               "Underlying Exception: "
                  + cause
                  + "; "
                  + formatStackTrace(cause.getStackTrace()));
            printClasspath |= cause instanceof NoClassDefFoundError;
            cause = cause.getCause();
         }

         if (printClasspath) {
            msg.add("Classpath:");
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            URL[] urls = ((URLClassLoader) cl).getURLs();
            for (URL url : urls) {
               msg.add(url.getFile());
            }
         }
      }

      printMessage(msg.toString());
   }

   private void printMessage(String msg) {
      if (messager == null) {
         messager = processingEnv.getMessager();
      }
      messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, msg);
   }

   @Override
   public void typeProcessingStart() {
      try {
         super.typeProcessingStart();
         initChecker();
         if (this.messager == null) {
            messager = processingEnv.getMessager();
            messager.printMessage(
               javax.tools.Diagnostic.Kind.WARNING,
               "You have forgotten to call super.initChecker in your "
                  + "subclass of SourceChecker, "
                  + this.getClass()
                  + "! Please ensure your checker is properly initialized.");
         }
         if (shouldAddShutdownHook()) {
            Runtime.getRuntime()
               .addShutdownHook(
                  new Thread() {
                     @Override
                     public void run() {
                        shutdownHook();
                     }
                  });
         }
      } catch (UserError ce) {
         logUserError(ce);
      } catch (RuntimeException ce) {
         logBugInCF(ce);
      } catch (Throwable t) {
         logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcessingStart", t, null));
      }
   }

   /**
    * Initialize the checker.
    *
    * @see AbstractProcessor#init(ProcessingEnvironment)
    */
   public void initChecker() {
      // Grab the Trees and Messager instances now; other utilities
      // (like Types and Elements) can be retrieved by subclasses.
      @Nullable Trees trees = Trees.instance(processingEnv);
      assert trees != null; /*nninvariant*/
      this.trees = trees;

      this.messager = processingEnv.getMessager();
      this.messages = getMessages();

      this.visitor = createSourceVisitor();
   }

   protected boolean shouldAddShutdownHook() {
      return hasOption("resourceStats");
   }

   protected void shutdownHook() {
      if (hasOption("resourceStats")) {
         // Check for the "resourceStats" option and don't call shouldAddShutdownHook
         // to allow subclasses to override shouldXXX and shutdownHook and simply
         // call the super implementations.
         printStats();
      }
   }

   protected void printStats() {
      List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
      for (MemoryPoolMXBean memoryPool : memoryPools) {
         System.out.println("Memory pool " + memoryPool.getName() + " statistics");
         System.out.println("  Pool type: " + memoryPool.getType());
         System.out.println("  Peak usage: " + memoryPool.getPeakUsage());
      }
   }

   private boolean warnedAboutSourceLevel = false;

   protected int errsOnLastExit = 0;

   @Override
   public void typeProcess(TypeElement e, TreePath p) {
      if (e == null) {
         messager.printMessage(
            javax.tools.Diagnostic.Kind.ERROR, "Refusing to process empty TypeElement");
         return;
      }
      if (p == null) {
         messager.printMessage(
            javax.tools.Diagnostic.Kind.ERROR,
            "Refusing to process empty TreePath in TypeElement: " + e);
         return;
      }

      Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
      Source source = Source.instance(context);
      // Don't use source.allowTypeAnnotations() because that API changed after 9.
      // Also the enum constant Source.JDK1_8 was renamed at some point...
      if (!warnedAboutSourceLevel && source.compareTo(Source.lookup("8")) < 0) {
         messager.printMessage(
            javax.tools.Diagnostic.Kind.WARNING,
            "-source " + source.name + " does not support type annotations");
         warnedAboutSourceLevel = true;
      }

      Log log = Log.instance(context);
      if (log.nerrors > this.errsOnLastExit) {
         this.errsOnLastExit = log.nerrors;
         previousErrorCompilationUnit = p.getCompilationUnit();
         return;
      }
      if (p.getCompilationUnit() == previousErrorCompilationUnit) {
         // If the same compilation unit was seen with an error before,
         // skip it. This is in particular necessary for Java errors, which
         // show up once, but further calls to typeProcess will happen.
         // See Issue 346.
         return;
      } else {
         previousErrorCompilationUnit = null;
      }
      if (visitor == null) {
         // typeProcessingStart invokes initChecker, which should
         // have set the visitor. If the field is still null, an
         // exception occurred during initialization, which was already
         // logged there. Don't also cause a NPE here.
         return;
      }
      if (p.getCompilationUnit() != currentRoot) {
         setRoot(p.getCompilationUnit());
         if (hasOption("filenames")) {
            // Add timestamp to indicate how long operations are taking
            message(Diagnostic.Kind.NOTE, new java.util.Date().toString());
            message(
               Diagnostic.Kind.NOTE,
               "%s is type-checking %s",
               (Object) this.getClass().getSimpleName(),
               currentRoot.getSourceFile().getName());
         }
      }

      // Visit the attributed tree.
      try {
         visitor.visit(p);
         warnUnneededSuppressions();
      } catch (UserError ce) {
         logUserError(ce);
      } catch (RuntimeException ce) {
         logBugInCF(ce);
      } catch (Throwable t) {
         logBugInCF(wrapThrowableAsBugInCF("SourceChecker.typeProcess", t, p));
      } finally {
         // Also add possibly deferred diagnostics, which will get published back in
         // AbstractTypeProcessor.
         this.errsOnLastExit = log.nerrors;
      }
   }

   protected void warnUnneededSuppressions() {
      if (!hasOption("warnUnneededSuppressions")) {
         return;
      }

      Set<Element> elementsSuppress = new HashSet<>(this.elementsWithSuppressedWarnings);
      this.elementsWithSuppressedWarnings.clear();
      Set<String> checkerKeys = new HashSet<>(getSuppressWarningsKeys());
      Set<String> errorKeys = new HashSet<>(messages.stringPropertyNames());
      warnUnneedSuppressions(elementsSuppress, checkerKeys, errorKeys);
      getVisitor().treesWithSuppressWarnings.clear();
   }

   protected void warnUnneedSuppressions(
      Set<Element> elementsSuppress, Set<String> checkerKeys, Set<String> errorKeys) {
      // It's not clear for which checker "all" is intended, so never report it as unused.
      checkerKeys.remove(SourceChecker.SUPPRESS_ALL_KEY);

      // Is the name of the checker required to suppress a warning?
      boolean requirePrefix = hasOption("requirePrefixInWarningSuppressions");

      for (Tree tree : getVisitor().treesWithSuppressWarnings) {
         Element elt = SourceTreeUtils.elementFromTree(tree);
         // TODO: This test is too coarse.  The fact that this @SuppressWarnings suppressed
         // *some* warning doesn't mean that every value in it did so.
         if (elementsSuppress.contains(elt)) {
            continue;
         }
         SuppressWarnings suppressAnno = elt.getAnnotation(SuppressWarnings.class);
         // Check each value of the user-written @SuppressWarnings annotation.
         for (String userKey : suppressAnno.value()) {
            String fullUserKey = userKey;
            int colonPos = userKey.indexOf(":");
            if (colonPos == -1) {
               // User-written error key contains no ":".
               if (checkerKeys.contains(userKey)) {
                  reportUnneededSuppression(tree, userKey);
               }
               if (requirePrefix) {
                  // This user-written key is not for the Checker Framework
                  continue;
               }
            } else {
               // User-written error key contains ":".
               String userCheckerKey = userKey.substring(0, colonPos);
               if (userCheckerKey.equals(SourceChecker.SUPPRESS_ALL_KEY)
                  || !checkerKeys.contains(userCheckerKey)) {
                  // This user-written key is for some other checker
                  continue;
               }
               userKey = userKey.substring(colonPos + 1);
            }
            for (String errorKey : errorKeys) {
               // The userKey may only be a part of an error key.
               // For example, @SuppressWarnings("purity") suppresses errors with keys:
               // purity.deterministic.void.method, purity.deterministic.constructor, etc.
               if (errorKey.contains(userKey)) {
                  reportUnneededSuppression(tree, fullUserKey);
               }
            }
         }
      }
   }

   private final String suppressWarningsClassName = SuppressWarnings.class.getCanonicalName();

   private void reportUnneededSuppression(Tree tree, String key) {
      Tree swTree = findSuppressWarningsTree(tree);
      report(
         Result.warning(
            SourceChecker.UNNEEDED_SUPPRESSION_KEY,
            "\"" + key + "\"",
            getClass().getSimpleName()),
         swTree);
   }

   private Tree findSuppressWarningsTree(Tree tree) {
      List<? extends AnnotationTree> annotations;
      if (SourceTreeUtils.isClassTree(tree)) {
         annotations = ((ClassTree) tree).getModifiers().getAnnotations();
      } else if (tree.getKind() == Tree.Kind.METHOD) {
         annotations = ((MethodTree) tree).getModifiers().getAnnotations();
      } else {
         annotations = ((VariableTree) tree).getModifiers().getAnnotations();
      }

      for (AnnotationTree annotationTree : annotations) {
         if (AnnotationUtils.areSameByName(
            SourceTreeUtils.annotationFromAnnotationTree(annotationTree),
            suppressWarningsClassName)) {
            return annotationTree;
         }
      }
      throw new RuntimeException("Did not find @SuppressWarnings: " + tree);
   }

   private RuntimeException wrapThrowableAsBugInCF(String where, Throwable t, @Nullable TreePath p) {
      return new RuntimeException(
         where
            + ": unexpected Throwable ("
            + t.getClass().getSimpleName()
            + ")"
            + ((p == null)
            ? ""
            : " while processing "
            + p.getCompilationUnit().getSourceFile().getName())
            + (t.getMessage() == null ? "" : "; message: " + t.getMessage()),
         t);
   }

   protected String formatStackTrace(StackTraceElement[] stackTrace) {
      StringJoiner sb = new StringJoiner(LINE_SEPARATOR);
      if (stackTrace.length == 0) {
         sb.add("no stack trace available.");
      } else {
         sb.add("Stack trace: ");
      }
      for (StackTraceElement ste : stackTrace) {
         sb.add(ste.toString());
      }
      return sb.toString();
   }

   protected String fullMessageOf(String messageKey, String defValue) {
      String key = messageKey;

      do {
         if (messages.containsKey(key)) {
            return messages.getProperty(key);
         }

         int dot = key.indexOf('.');
         if (dot < 0) {
            return defValue;
         }
         key = key.substring(dot + 1);
      } while (true);
   }

   private void message(
      Diagnostic.Kind kind,
      Object source,
      @CompilerMessageKey String msgKey,
      Object... args) {

      assert messages != null : "null messages";

      if (args != null) {
         for (int i = 0; i < args.length; ++i) {
            if (args[i] == null) {
               continue;
            }

            // Try to process the arguments
            args[i] = processArg(args[i]);
         }
      }

      if (kind == Diagnostic.Kind.NOTE) {
         System.err.println("(NOTE) " + String.format(msgKey, args));
         return;
      }

      final String defaultFormat = String.format("(%s)", msgKey);
      String fmtString;
      if (this.processingEnv.getOptions() != null /*nnbug*/
         && this.processingEnv.getOptions().containsKey("nomsgtext")) {
         fmtString = defaultFormat;
      } else if (this.processingEnv.getOptions() != null /*nnbug*/
         && this.processingEnv.getOptions().containsKey("detailedmsgtext")) {
         // The -Adetailedmsgtext command-line option was given, so output
         // a stylized error message for easy parsing by a tool.

         StringBuilder sb = new StringBuilder();

         // The parts, separated by " $$ " (DETAILS_SEPARATOR), are:

         // (1) error key
         // TODO: should we also have some type system identifier here?
         // E.g. Which subclass of SourceChecker we are? Or also the SuppressWarnings keys?
         sb.append(defaultFormat);
         sb.append(DETAILS_SEPARATOR);

         // (2) number of additional tokens, and those tokens; this
         // depends on the error message, and an example is the found
         // and expected types
         if (args != null) {
            sb.append(args.length);
            sb.append(DETAILS_SEPARATOR);
            for (Object arg : args) {
               sb.append(arg);
               sb.append(DETAILS_SEPARATOR);
            }
         } else {
            // Output 0 for null arguments.
            sb.append(0);
            sb.append(DETAILS_SEPARATOR);
         }

         // (3) The error position, as starting and ending characters in
         // the source file.
         final Tree tree;
         if (source instanceof Element) {
            tree = trees.getTree((Element) source);
         } else if (source instanceof Tree) {
            tree = (Tree) source;
         } else {
            tree = null;
         }
         sb.append(treeToFilePositionString(tree, currentRoot, processingEnv));
         sb.append(DETAILS_SEPARATOR);

         // (4) The human-readable error message.
         sb.append(fullMessageOf(msgKey, defaultFormat));

         fmtString = sb.toString();

      } else {
         // The key for the warning/error being printed, in brackets; prefixes the error message.
         final String suppressing;
         if (this.processingEnv.getOptions().containsKey("showSuppressWarningKeys")) {
            suppressing = String.format("[%s:%s] ", this.getSuppressWarningsKeys(), msgKey);
         } else if (this.processingEnv
            .getOptions()
            .containsKey("requirePrefixInWarningSuppressions")) {
            // If the warning key must be prefixed with a checker key, then add that to the
            // warning key that is printed.
            String defaultKey = getDefaultWarningSuppressionKey();
            Collection<String> keys = getSuppressWarningsKeys();
            if (keys.contains(defaultKey)) {
               suppressing = String.format("[%s:%s] ", defaultKey, msgKey);
            } else if (keys.isEmpty()) {
               keys.remove(SUPPRESS_ALL_KEY);
               if (keys.isEmpty()) {
                  suppressing = String.format("[%s:%s] ", SUPPRESS_ALL_KEY, msgKey);
               } else {
                  String firstKey = keys.iterator().next();
                  suppressing = String.format("[%s:%s] ", firstKey, msgKey);
               }
            } else {
               suppressing = String.format("[%s] ", msgKey);
            }
         } else {
            suppressing = String.format("[%s] ", msgKey);
         }
         fmtString = suppressing + fullMessageOf(msgKey, defaultFormat);
      }
      String messageText;
      try {
         messageText = String.format(fmtString, args);
      } catch (Exception e) {
         messageText =
            "Invalid format string: \"" + fmtString + "\" args: " + Arrays.toString(args);
      }

      if (source instanceof Element) {
         messager.printMessage(kind, messageText, (Element) source);
      } else if (source instanceof Tree) {
         printMessage(kind, messageText, (Tree) source, currentRoot);
      } else {
         throw new RuntimeException("invalid position source: " + source.getClass().getName());
      }
   }

   protected void printMessage(
      Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root) {
      Trees.instance(processingEnv).printMessage(kind, message, source, root);
   }

   protected Object processArg(Object arg) {
      // Check to see if the argument itself is a property to be expanded
      return messages.getProperty(arg.toString(), arg.toString());
   }

   public void message(Diagnostic.Kind kind, String msg, Object... args) {
      String ftdmsg = String.format(msg, args);
      if (messager != null) {
         messager.printMessage(kind, ftdmsg);
      } else {
         System.err.println(kind + ": " + ftdmsg);
      }
   }

   public String treeToFilePositionString(
      Tree tree, CompilationUnitTree currentRoot, ProcessingEnvironment processingEnv) {
      if (tree == null) {
         return null;
      }

      SourcePositions sourcePositions = trees.getSourcePositions();
      long start = sourcePositions.getStartPosition(currentRoot, tree);
      long end = sourcePositions.getEndPosition(currentRoot, tree);

      return "( " + start + ", " + end + " )";
   }

   public static final String DETAILS_SEPARATOR = " $$ ";

   private boolean checkSuppressWarnings(@Nullable SuppressWarnings anno, String errKey) {

      // Don't suppress warnings if this checker provides no key to do so.
      Collection<String> checkerSwKeys = this.getSuppressWarningsKeys();
      if (checkerSwKeys.isEmpty()) {
         return false;
      }

      if (this.suppressWarnings == null) {
         this.suppressWarnings = createSuppressWarnings(getOptions());
      }
      String[] cmdLineSwKeys = this.suppressWarnings;
      if (checkSuppressWarnings(cmdLineSwKeys, errKey)) {
         return true;
      }

      if (anno != null) {
         String[] userSwKeys = arrayToLowerCase(anno.value());
         if (checkSuppressWarnings(userSwKeys, errKey)) {
            return true;
         }
      }

      return false;
   }

   private boolean checkSuppressWarnings(String @Nullable [] userSwKeys, String errKey) {
      if (userSwKeys == null) {
         return false;
      }
      // Is the name of the checker required to suppress a warning?
      boolean requirePrefix = hasOption("requirePrefixInWarningSuppressions");

      Collection<String> checkerKeys = this.getSuppressWarningsKeys();

      // Check each value of the user-written @SuppressWarnings annotation.
      for (String userKey : userSwKeys) {
         int colonPos = userKey.indexOf(":");
         if (colonPos == -1) {
            // User-written error key contains no ":".
            if (checkerKeys.contains(userKey)) {
               // Emitted error is exactly a @SuppressWarnings key: "nullness", for example.
               return true;
            }
            if (requirePrefix) {
               continue;
            }
         } else {
            // User-written error key contains ":".
            String userCheckerKey = userKey.substring(0, colonPos);
            if (!checkerKeys.contains(userCheckerKey)) {
               continue;
            }
            userKey = userKey.substring(colonPos + 1);
         }
         if (errKey.contains(userKey)) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldSuppressWarnings(Tree tree, String errKey) {
      // Don't suppress warnings if this checker provides no key to do so.
      Collection<String> checkerKeys = this.getSuppressWarningsKeys();
      if (checkerKeys.isEmpty()) {
         return false;
      }

      // trees.getPath might be slow, but this is only used in error reporting
      // TODO: #1586 this might return null within a cloned finally block and
      // then a warning that should be suppressed isn't. Fix this when fixing #1586.
      @Nullable TreePath path = trees.getPath(this.currentRoot, tree);
      if (path == null) {
         return false;
      }

      @Nullable VariableTree var = SourceTreeUtils.enclosingVariable(path);
      if (var != null && shouldSuppressWarnings(SourceTreeUtils.elementFromTree(var), errKey)) {
         return true;
      }

      @Nullable MethodTree method = SourceTreeUtils.enclosingMethod(path);
      if (method != null) {
         @Nullable Element elt = SourceTreeUtils.elementFromTree(method);

         if (shouldSuppressWarnings(elt, errKey)) {
            return true;
         }

         if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
            // Return false immediately. Do NOT check for AnnotatedFor in
            // the enclosing elements, because they may not have an
            // @AnnotatedFor.
            return false;
         }
      }

      @Nullable ClassTree cls = SourceTreeUtils.enclosingClass(path);
      if (cls != null) {
         @Nullable Element elt = SourceTreeUtils.elementFromTree(cls);

         if (shouldSuppressWarnings(elt, errKey)) {
            return true;
         }

         if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
            // Return false immediately. Do NOT check for AnnotatedFor in
            // the enclosing elements, because they may not have an
            // @AnnotatedFor.
            return false;
         }
      }

      if (useUncheckedCodeDefault("source")) {
         // If we got this far without hitting an @AnnotatedFor and returning
         // false, we DO suppress the warning.
         return true;
      }

      return false;
   }

   public boolean useUncheckedCodeDefault(String kindOfCode) {
      final boolean useUncheckedDefaultsForSource = false;
      final boolean useUncheckedDefaultsForByteCode = false;
      String option = this.getOption("useDefaultsForUncheckedCode");

      String[] args = option != null ? option.split(",") : new String[0];
      for (String arg : args) {
         boolean value = arg.indexOf("-") != 0;
         arg = value ? arg : arg.substring(1);
         if (arg.equals(kindOfCode)) {
            return value;
         }
      }
      if (kindOfCode.equals("source")) {
         return useUncheckedDefaultsForSource;
      } else if (kindOfCode.equals("bytecode")) {
         return useUncheckedDefaultsForByteCode;
      } else {
         throw new UserError(
            "SourceChecker: unexpected argument to useUncheckedCodeDefault: " + kindOfCode);
      }
   }

   protected final Set<Element> elementsWithSuppressedWarnings = new HashSet<>();

   public boolean shouldSuppressWarnings(@Nullable Element elt, String errKey) {
      if (UNNEEDED_SUPPRESSION_KEY.equals(errKey)) {
         // Never suppress an unneeded suppression key warning.
         // TODO: This choice is questionable, because these warnings should be suppressable just
         // like any others.  The reason for the choice is that if a user writes
         // `@SuppressWarnings("nullness")` that isn't needed, then that annotation would
         // suppress the unneeded suppression warning.  It would take extra work to permit more
         // desirable behavior in that case.
         return false;
      }

      if (elt == null) {
         return false;
      }

      if (checkSuppressWarnings(elt.getAnnotation(SuppressWarnings.class), errKey)) {
         if (hasOption("warnUnneededSuppressions")) {
            elementsWithSuppressedWarnings.add(elt);
         }
         return true;
      }

      if (isAnnotatedForThisCheckerOrUpstreamChecker(elt)) {
         // Return false immediately. Do NOT check for AnnotatedFor in the
         // enclosing elements, because they may not have an @AnnotatedFor.
         return false;
      }

      return shouldSuppressWarnings(elt.getEnclosingElement(), errKey);
   }

   private boolean isAnnotatedForThisCheckerOrUpstreamChecker(@Nullable Element elt) {

      if (elt == null || !useUncheckedCodeDefault("source")) {
         return false;
      }

      @Nullable AnnotatedFor anno = elt.getAnnotation(AnnotatedFor.class);

      String[] userAnnotatedFors = (anno == null ? null : anno.value());

      if (userAnnotatedFors != null) {
         List<String> upstreamCheckerNames = getUpstreamCheckerNames();

         for (String userAnnotatedFor : userAnnotatedFors) {
            if (CheckerMain.matchesCheckerOrSubcheckerFromList(
               userAnnotatedFor, upstreamCheckerNames)) {
               return true;
            }
         }
      }

      return false;
   }

   public void report(final Result r, final Object src) {
      if (r.isSuccess()) {
         return;
      }

      String errKey = r.getMessageKeys().iterator().next();
      if (src instanceof Tree && shouldSuppressWarnings((Tree) src, errKey)) {
         return;
      }
      if (src instanceof Element && shouldSuppressWarnings((Element) src, errKey)) {
         return;
      }

      for (Result.DiagMessage msg : r.getDiagMessages()) {
         if (r.isFailure()) {
            this.message(
               hasOption("warns")
                  ? Diagnostic.Kind.MANDATORY_WARNING
                  : Diagnostic.Kind.ERROR,
               src,
               msg.getMessageKey(),
               msg.getArgs());
         } else if (r.isWarning()) {
            this.message(
               Diagnostic.Kind.MANDATORY_WARNING, src, msg.getMessageKey(), msg.getArgs());
         } else {
            this.message(Diagnostic.Kind.NOTE, src, msg.getMessageKey(), msg.getArgs());
         }
      }
   }

   public final boolean getLintOption(String name) {
      return getLintOption(name, false);
   }

   public final boolean getLintOption(String name, boolean def) {

      if (!this.getSupportedLintOptions().contains(name)) {
         throw new UserError("Illegal lint option: " + name);
      }

      if (activeLints == null) {
         activeLints = createActiveLints(processingEnv.getOptions());
      }

      if (activeLints.isEmpty()) {
         return def;
      }

      String tofind = name;
      while (tofind != null) {
         if (activeLints.contains(tofind)) {
            return true;
         } else if (activeLints.contains(String.format("-%s", tofind))) {
            return false;
         }

         tofind = parentOfOption(tofind);
      }

      return def;
   }

   protected final void setLintOption(String name, boolean val) {
      if (!this.getSupportedLintOptions().contains(name)) {
         throw new UserError("Illegal lint option: " + name);
      }

        /* TODO: warn if the option is also provided on the command line(?)
        boolean exists = false;
        if (!activeLints.isEmpty()) {
            String tofind = name;
            while (tofind != null) {
                if (activeLints.contains(tofind) || // direct
                        activeLints.contains(String.format("-%s", tofind)) || // negation
                        activeLints.contains(tofind.substring(1))) { // name was negation
                    exists = true;
                }
                tofind = parentOfOption(tofind);
            }
        }

        if (exists) {
            // TODO: Issue warning?
        }
        TODO: assert that name doesn't start with '-'
        */

      Set<String> newlints = new HashSet<>();
      newlints.addAll(activeLints);
      if (val) {
         newlints.add(name);
      } else {
         newlints.add(String.format("-%s", name));
      }
      activeLints = Collections.unmodifiableSet(newlints);
   }

   private String parentOfOption(String name) {
      if (name.equals("all")) {
         return null;
      } else if (name.contains(":")) {
         return name.substring(0, name.lastIndexOf(':'));
      } else {
         return "all";
      }
   }

   public Set<String> getSupportedLintOptions() {
      if (supportedLints == null) {
         supportedLints = createSupportedLintOptions();
      }
      return supportedLints;
   }

   protected Set<String> createSupportedLintOptions() {
      @Nullable SupportedLintOptions sl = this.getClass().getAnnotation(SupportedLintOptions.class);

      if (sl == null) {
         return Collections.emptySet();
      }

      @Nullable String @Nullable [] slValue = sl.value();
      assert slValue != null; /*nninvariant*/

      @Nullable String[] lintArray = slValue;
      Set<String> lintSet = new HashSet<>(lintArray.length);
      for (String s : lintArray) {
         lintSet.add(s);
      }
      return Collections.unmodifiableSet(lintSet);
   }

   protected void setSupportedLintOptions(Set<String> newlints) {
      supportedLints = newlints;
   }

   protected void addOptions(Map<String, String> moreopts) {
      Map<String, String> activeOpts = new HashMap<>(getOptions());
      activeOpts.putAll(moreopts);
      activeOptions = Collections.unmodifiableMap(activeOpts);
   }

   @Override
   public final boolean hasOption(String name) {
      return getOptions().containsKey(name);
   }

   @Override
   public final String getOption(String name) {
      return getOption(name, null);
   }

   @Override
   public final boolean getBooleanOption(String name) {
      return getBooleanOption(name, false);
   }

   @Override
   public final boolean getBooleanOption(String name, boolean defaultValue) throws UserError {
      String value = getOption(name);
      if (value == null) {
         return defaultValue;
      }
      if (value.equals("true")) {
         return true;
      }
      if (value.equals("false")) {
         return false;
      }
      throw new UserError(
         String.format(
            "Value of %s option should be a boolean, but is \"%s\".", name, value));
   }

   @Override
   public Map<String, String> getOptions() {
      if (activeOptions == null) {
         activeOptions = createActiveOptions(processingEnv.getOptions());
      }
      return activeOptions;
   }

   @Override
   public final String getOption(String name, String defaultValue) {

      if (!this.getSupportedOptions().contains(name)) {
         throw new UserError("Illegal option: " + name);
      }

      if (activeOptions == null) {
         activeOptions = createActiveOptions(processingEnv.getOptions());
      }

      if (activeOptions.isEmpty()) {
         return defaultValue;
      }

      if (activeOptions.containsKey(name)) {
         return activeOptions.get(name);
      } else {
         return defaultValue;
      }
   }

   @Override
   public Set<String> getSupportedOptions() {
      Set<String> options = new HashSet<>();

      // Support all options provided with the standard
      // {@link javax.annotation.processing.SupportedOptions}
      // annotation.
      options.addAll(super.getSupportedOptions());

      // For the Checker Framework annotation
      // {@link org.checkerframework.framework.source.SupportedOptions}
      // we additionally add
      Class<?> clazz = this.getClass();
      List<Class<?>> clazzPrefixes = new ArrayList<>();

      do {
         clazzPrefixes.add(clazz);

         SupportedOptions so = clazz.getAnnotation(SupportedOptions.class);
         if (so != null) {
            options.addAll(expandCFOptions(clazzPrefixes, so.value()));
         }
         clazz = clazz.getSuperclass();
      } while (clazz != null
         && !clazz.getName().equals(AbstractTypeProcessor.class.getCanonicalName()));

      return Collections.unmodifiableSet(options);
   }

   protected Collection<String> expandCFOptions(
      List<? extends Class<?>> clazzPrefixes, String[] options) {
      Set<String> res = new HashSet<>();

      for (String option : options) {
         res.add(option);
         for (Class<?> clazz : clazzPrefixes) {
            res.add(clazz.getCanonicalName() + OPTION_SEPARATOR + option);
            res.add(clazz.getSimpleName() + OPTION_SEPARATOR + option);
         }
      }
      return res;
   }

   @Override
   public final Set<String> getSupportedAnnotationTypes() {

      SupportedAnnotationTypes supported =
         this.getClass().getAnnotation(SupportedAnnotationTypes.class);
      if (supported != null) {
         throw new RuntimeException(
            "@SupportedAnnotationTypes should not be written on any checker;"
               + " supported annotation types are inherited from SourceChecker.");
      }
      return Collections.singleton("*");
   }

   public Collection<String> getSuppressWarningsKeys() {
      return getStandardSuppressWarningsKeys();
   }

   protected final Collection<String> getStandardSuppressWarningsKeys() {
      // TreeSet ensures keys are returned in a consistent order.
      Set<String> result = new TreeSet<>();
      result.add(SUPPRESS_ALL_KEY);

      SuppressWarningsKeys annotation = this.getClass().getAnnotation(SuppressWarningsKeys.class);
      if (annotation != null) {
         // Add from annotation
         for (String key : annotation.value()) {
            result.add(key.toLowerCase());
         }

      } else {
         // No @SuppressWarningsKeys annotation, by default infer key from class name
         String key = getDefaultWarningSuppressionKey();
         result.add(key);
      }

      return result;
   }

   private String getDefaultWarningSuppressionKey() {
      String className = this.getClass().getSimpleName();
      int indexOfChecker = className.lastIndexOf("Checker");
      if (indexOfChecker == -1) {
         indexOfChecker = className.lastIndexOf("Subchecker");
      }
      String result = (indexOfChecker == -1) ? className : className.substring(0, indexOfChecker);
      return result.toLowerCase();
   }

   public final boolean shouldSkipUses(Element element) {
      if (element == null) {
         return false;
      }
      TypeElement typeElement = ElementUtils.enclosingClass(element);
      String name = typeElement.toString();
      return shouldSkipUses(name);
   }

   public boolean shouldSkipUses(String typeName) {
      // System.out.printf("shouldSkipUses(%s) %s%nskipUses %s%nonlyUses %s%nresult %s%n",
      //                   element,
      //                   name,
      //                   skipUsesPattern.matcher(name).find(),
      //                   onlyUsesPattern.matcher(name).find(),
      //                   (skipUsesPattern.matcher(name).find()
      //                    || ! onlyUsesPattern.matcher(name).find()));
      // StackTraceElement[] stea = new Throwable().getStackTrace();
      // for (int i=0; i<3; i++) {
      //     System.out.println("  " + stea[i]);
      // }
      // System.out.println();
      if (skipUsesPattern == null) {
         skipUsesPattern = getSkipUsesPattern(getOptions());
      }
      if (onlyUsesPattern == null) {
         onlyUsesPattern = getOnlyUsesPattern(getOptions());
      }
      return skipUsesPattern.matcher(typeName).find()
         || !onlyUsesPattern.matcher(typeName).find();
   }

   public final boolean shouldSkipDefs(ClassTree node) {
      String qualifiedName = SourceTreeUtils.typeOf(node).toString();
      // System.out.printf("shouldSkipDefs(%s) %s%nskipDefs %s%nonlyDefs %s%nresult %s%n%n",
      //                   node,
      //                   qualifiedName,
      //                   skipDefsPattern.matcher(qualifiedName).find(),
      //                   onlyDefsPattern.matcher(qualifiedName).find(),
      //                   (skipDefsPattern.matcher(qualifiedName).find()
      //                    || ! onlyDefsPattern.matcher(qualifiedName).find()));
      if (skipDefsPattern == null) {
         skipDefsPattern = getSkipDefsPattern(getOptions());
      }
      if (onlyDefsPattern == null) {
         onlyDefsPattern = getOnlyDefsPattern(getOptions());
      }

      return skipDefsPattern.matcher(qualifiedName).find()
         || !onlyDefsPattern.matcher(qualifiedName).find();
   }

   public final boolean shouldSkipDefs(ClassTree cls, MethodTree meth) {
      return shouldSkipDefs(cls);
   }

   protected Properties getProperties(Class<?> cls, String filePath) {
      Properties prop = new Properties();
      try {
         InputStream base = cls.getResourceAsStream(filePath);

         if (base == null) {
            // No message customization file was given
            return prop;
         }

         prop.load(base);
      } catch (IOException e) {
         message(Diagnostic.Kind.WARNING, "Couldn't parse properties file: " + filePath);
         // e.printStackTrace();
         // ignore the possible customization file
      }
      return prop;
   }

   @Override
   public final SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latest();
   }

   void printGitProperties() {
      if (gitPropertiesPrinted) {
         return;
      }
      gitPropertiesPrinted = true;

      try (InputStream in = getClass().getResourceAsStream("/git.properties");
           BufferedReader reader = new BufferedReader(new InputStreamReader(in)); ) {
         String line;
         while ((line = reader.readLine()) != null) {
            System.out.println(line);
         }
      } catch (IOException e) {
         System.out.println("IOException while reading git.properties: " + e.getMessage());
      }
   }
}
