package foundation.polar.gratify.annotations.utils;

import foundation.polar.gratify.utils.compiler.PluginUtil;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class CheckerMain {

   public static void main(String[] args) {
      final File pathToThisJar = new File(findPathTo(CheckerMain.class, false));
      ArrayList<String> alargs = new ArrayList<>(args.length);
      alargs.addAll(Arrays.asList(args));
      final CheckerMain program = new CheckerMain(pathToThisJar, alargs);
      final int exitStatus = program.invokeCompiler();
      System.exit(exitStatus);
   }

   protected final File jdkJar;
   protected final File javacJar;
   protected final File checkerJar;
   protected final File checkerQualJar;
   private final List<String> compilationBootclasspath;
   private final List<String> runtimeClasspath;
   private final List<String> jvmOpts;
   private final List<String> cpOpts;
   private final List<String> ppOpts;
   private final List<String> toolOpts;
   private final List<File> argListFiles;

   public CheckerMain(final File checkerJar, final List<String> args) {

      this.checkerJar = checkerJar;
      final File searchPath = checkerJar.getParentFile();

      replaceShorthandProcessor(args);
      argListFiles = collectArgFiles(args);

      this.checkerQualJar =
         extractFileArg(
            PluginUtil.CHECKER_QUAL_PATH_OPT,
            new File(searchPath, "checker-qual.jar"),
            args);

      this.javacJar =
         extractFileArg(PluginUtil.JAVAC_PATH_OPT, new File(searchPath, "javac.jar"), args);

      final String jdkJarName = PluginUtil.getJdkJarName();
      this.jdkJar =
         extractFileArg(PluginUtil.JDK_PATH_OPT, new File(searchPath, jdkJarName), args);

      this.compilationBootclasspath = createCompilationBootclasspath(args);
      this.runtimeClasspath = createRuntimeClasspath(args);
      this.jvmOpts = extractJvmOpts(args);

      this.cpOpts = createCpOpts(args);
      this.ppOpts = createPpOpts(args);
      this.toolOpts = args;

      assertValidState();
   }

   protected void assertValidState() {
      if (PluginUtil.getJreVersion() < 9) {
         assertFilesExist(Arrays.asList(javacJar, jdkJar, checkerJar, checkerQualJar));
      } else {
         // TODO: once the jdk11 jars exist, check for them.
         assertFilesExist(Arrays.asList(checkerJar, checkerQualJar));
      }
   }

   public void addToClasspath(List<String> cpOpts) {
      this.cpOpts.addAll(cpOpts);
   }
   public void addToProcessorpath(List<String> ppOpts) {
      this.ppOpts.addAll(ppOpts);
   }
   public void addToRuntimeClasspath(List<String> runtimeClasspathOpts) {
      this.runtimeClasspath.addAll(runtimeClasspathOpts);
   }

   protected List<String> createRuntimeClasspath(final List<String> argsList) {
      return new ArrayList<>(Arrays.asList(javacJar.getAbsolutePath()));
   }

   protected List<String> createCompilationBootclasspath(final List<String> argsList) {
      final List<String> extractedBcp = extractBootClassPath(argsList);
      if (PluginUtil.getJreVersion() == 8) {
         extractedBcp.add(0, jdkJar.getAbsolutePath());
      }

      return extractedBcp;
   }

   protected List<String> createCpOpts(final List<String> argsList) {
      final List<String> extractedOpts = extractCpOpts(argsList);
      extractedOpts.add(0, this.checkerQualJar.getAbsolutePath());
      return extractedOpts;
   }

   // Assumes that createCpOpts has already been run.
   protected List<String> createPpOpts(final List<String> argsList) {
      final List<String> extractedOpts = extractPpOpts(argsList);
      if (extractedOpts.isEmpty()) {
         // If processorpath is not provided, then javac uses the classpath.
         // CheckerMain always supplies a processorpath, so if the user
         // didn't specify a processorpath, then use the classpath.
         extractedOpts.addAll(this.cpOpts);
      }
      extractedOpts.add(0, this.checkerJar.getAbsolutePath());
      return extractedOpts;
   }

   protected List<File> collectArgFiles(final List<String> args) {
      final List<File> argListFiles = new ArrayList<>();
      for (final String arg : args) {
         if (arg.startsWith("@")) {
            argListFiles.add(new File(arg.substring(1)));
         }
      }

      return argListFiles;
   }

   protected static String extractArg(
      final String argumentName, final String alternative, final List<String> args) {
      int i = args.indexOf(argumentName);
      if (i == -1) {
         return alternative;
      } else if (i == args.size() - 1) {
         throw new RuntimeException(
            "Command line contains " + argumentName + " but no value following it");
      } else {
         args.remove(i);
         return args.remove(i);
      }
   }

   protected static File extractFileArg(
      final String argumentName, final File alternative, final List<String> args) {
      final String filePath = extractArg(argumentName, null, args);
      if (filePath == null) {
         return alternative;
      } else {
         return new File(filePath);
      }
   }

   protected static List<String> extractOptWithPattern(
      final Pattern pattern, boolean allowEmpties, final List<String> args) {
      final List<String> matchedArgs = new ArrayList<>();

      int i = 0;
      while (i < args.size()) {
         final Matcher matcher = pattern.matcher(args.get(i));
         if (matcher.matches()) {
            final String arg = matcher.group(1).trim();

            if (!arg.isEmpty() || allowEmpties) {
               matchedArgs.add(arg);
            }

            args.remove(i);
         } else {
            i++;
         }
      }

      return matchedArgs;
   }

   protected static final Pattern BOOT_CLASS_PATH_REGEX =
      Pattern.compile("^(?:-J)?-Xbootclasspath/p:(.*)$");

   protected static List<String> extractBootClassPath(final List<String> args) {
      return extractOptWithPattern(BOOT_CLASS_PATH_REGEX, false, args);
   }

   protected static final Pattern JVM_OPTS_REGEX = Pattern.compile("^(?:-J)(.*)$");

   protected static List<String> extractJvmOpts(final List<String> args) {
      return extractOptWithPattern(JVM_OPTS_REGEX, false, args);
   }

   protected static List<String> extractCpOpts(final List<String> args) {
      List<String> actualArgs = new ArrayList<>();

      String lastCpArg = null;

      for (int i = 0; i < args.size(); i++) {
         if ((args.get(i).equals("-cp") || args.get(i).equals("-classpath"))
            && (i + 1 < args.size())) {
            args.remove(i);
            // Every classpath entry overrides the one before it.
            lastCpArg = args.remove(i);
            // re-process whatever is currently at element i
            i--;
         }
      }

      // The logic below is exactly what the javac script does.  If no command-line classpath is
      // specified, use the "CLASSPATH" environment variable followed by the current directory.
      if (lastCpArg == null) {
         final String systemClassPath = System.getenv("CLASSPATH");
         if (systemClassPath != null && !systemClassPath.trim().isEmpty()) {
            actualArgs.add(systemClassPath.trim());
         }

         actualArgs.add(".");
      } else {
         actualArgs.add(lastCpArg);
      }

      return actualArgs;
   }

   protected static List<String> extractPpOpts(final List<String> args) {
      List<String> actualArgs = new ArrayList<>();

      String path = null;

      for (int i = 0; i < args.size(); i++) {
         if (args.get(i).equals("-processorpath") && (i + 1 < args.size())) {
            args.remove(i);
            path = args.remove(i);
            // re-process whatever is currently at element i
            i--;
         }
      }

      if (path != null) {
         actualArgs.add(path);
      }

      return actualArgs;
   }

   protected void addMainToArgs(final List<String> args) {
      args.add("com.sun.tools.javac.Main");
   }

   public List<String> getExecArguments() {
      List<String> args = new ArrayList<>(jvmOpts.size() + cpOpts.size() + toolOpts.size() + 7);

      // TODO: do we need java.exe on Windows?
      final String java =
         "java"; // PluginUtil.getJavaCommand(System.getProperty("java.home"), System.out);
      args.add(java);

      if (PluginUtil.getJreVersion() == 8) {
         args.add("-Xbootclasspath/p:" + PluginUtil.join(File.pathSeparator, runtimeClasspath));
      } else {
         args.addAll(
            Arrays.asList(
               "--illegal-access=warn",
               "--add-opens",
               "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"));
      }

      args.add("-classpath");
      args.add(String.join(File.pathSeparator, runtimeClasspath));
      args.add("-ea");
      // com.sun.tools needs to be enabled separately
      args.add("-ea:com.sun.tools...");

      args.addAll(jvmOpts);

      addMainToArgs(args);

      if (!argsListHasClassPath(argListFiles)) {
         args.add("-classpath");
         args.add(quote(concatenatePaths(cpOpts)));
      }
      if (!argsListHasProcessorPath(argListFiles)) {
         args.add("-processorpath");
         args.add(quote(concatenatePaths(ppOpts)));
      }

      if (PluginUtil.getJreVersion() == 8) {
         // No classes on the compilation bootclasspath will be loaded
         // during compilation, but the classes are read by the compiler
         // without loading them.  The compiler assumes that any class on
         // this bootclasspath will be on the bootclasspath of the JVM used
         // to later run the classfiles that Javac produces.  Our
         // jdk8.jar classes don't have bodies, so they won't be used at
         // run time, but other, real definitions of those classes will be
         // on the classpath at run time.
         args.add(
            "-Xbootclasspath/p:"
               + String.join(File.pathSeparator, compilationBootclasspath));

         // We currently provide a Java 8 JDK and want to be runnable
         // on a Java 8 JVM. So set source/target to 8.
         args.add("-source");
         args.add("8");
         args.add("-target");
         args.add("8");
      }

      args.addAll(toolOpts);
      return args;
   }

   private String concatenatePaths(List<String> paths) {
      List<String> elements = new ArrayList<>();
      for (String path : paths) {
         for (String element : path.split(File.pathSeparator)) {
            elements.addAll(expandWildcards(element));
         }
      }
      return String.join(File.pathSeparator, elements);
   }

   private static final String FILESEP_STAR = File.separator + "*";

   private List<String> expandWildcards(String pathElement) {
      if (pathElement.equals("*")) {
         return jarFiles(".");
      } else if (pathElement.endsWith(FILESEP_STAR)) {
         return jarFiles(pathElement.substring(0, pathElement.length() - 1));
      } else if (pathElement.equals("")) {
         return Collections.emptyList();
      } else {
         return Collections.singletonList(pathElement);
      }
   }

   private List<String> jarFiles(String directory) {
      File dir = new File(directory);
      File[] jarFiles =
         dir.listFiles((d, name) -> name.endsWith(".jar") || name.endsWith(".JAR"));
      List<String> result = new ArrayList<>(jarFiles.length);
      for (File jarFile : jarFiles) {
         result.add(jarFile.toString());
      }
      return result;
   }

   public int invokeCompiler() {
      List<String> args = getExecArguments();

      for (int i = 0; i < args.size(); i++) {
         String arg = args.get(i);

         if (arg.startsWith("-AoutputArgsToFile=")) {
            String fileName = arg.substring(19);
            args.remove(i);
            outputArgumentsToFile(fileName, args);
            break;
         }
      }

      return ExecUtil.execute(args.toArray(new String[args.size()]), System.out, System.err);
   }

   private static void outputArgumentsToFile(String outputFilename, List<String> args) {
      if (outputFilename != null) {
         String errorMessage = null;

         try {
            PrintWriter writer =
               (outputFilename.equals("-")
                  ? new PrintWriter(System.out)
                  : new PrintWriter(outputFilename, "UTF-8"));
            for (int i = 0; i < args.size(); i++) {
               String arg = args.get(i);

               // We would like to include the filename of the argfile instead of its contents.
               // The problem is that the file will sometimes disappear by the time the user
               // can look at or run the resulting script. Maven deletes the argfile very
               // shortly after it has been handed off to javac, for example. Ideally we would
               // print the argfile filename as a comment but the resulting file couldn't then
               // be run as a script on Unix or Windows.
               if (arg.startsWith("@")) {
                  // Read argfile and include its parameters in the output file.
                  String inputFilename = arg.substring(1);

                  BufferedReader br = new BufferedReader(new FileReader(inputFilename));
                  String line;
                  while ((line = br.readLine()) != null) {
                     writer.print(line);
                     writer.print(" ");
                  }
                  br.close();
               } else {
                  writer.print(arg);
                  writer.print(" ");
               }
            }
            writer.close();
         } catch (IOException e) {
            errorMessage = e.toString();
         }

         if (errorMessage != null) {
            System.err.println(
               "Failed to output command-line arguments to file "
                  + outputFilename
                  + " due to exception: "
                  + errorMessage);
         }
      }
   }

   private static boolean argsListHasClassPath(final List<File> argListFiles) {
      for (final String arg : expandArgFiles(argListFiles)) {
         if (arg.contains("-classpath") || arg.contains("-cp")) {
            return true;
         }
      }

      return false;
   }

   private static boolean argsListHasProcessorPath(final List<File> argListFiles) {
      for (final String arg : expandArgFiles(argListFiles)) {
         if (arg.contains("-processorpath")) {
            return true;
         }
      }

      return false;
   }

   protected static List<String> expandArgFiles(final List<File> files) {
      final List<String> content = new ArrayList<>();
      for (final File file : files) {
         try {
            content.addAll(PluginUtil.readFile(file));
         } catch (final IOException exc) {
            throw new RuntimeException("Could not open file: " + file.getAbsolutePath(), exc);
         }
      }
      return content;
   }

   public static String findPathTo(Class<?> cls, boolean errIfFromDirectory)
      throws IllegalStateException {
      if (cls == null) {
         cls = CheckerMain.class;
      }
      String name = cls.getName();
      String classFileName;
      /* name is something like package.name.ContainingClass$ClassName. We need to turn this into ContainingClass$ClassName.class. */
      {
         int idx = name.lastIndexOf('.');
         classFileName = (idx == -1 ? name : name.substring(idx + 1)) + ".class";
      }

      String uri = cls.getResource(classFileName).toString();
      if (uri.startsWith("file:")) {
         if (errIfFromDirectory) {
            return uri;
         } else {
            throw new IllegalStateException(
               "This class has been loaded from a directory and not from a jar file.");
         }
      }
      if (!uri.startsWith("jar:file:")) {
         int idx = uri.indexOf(':');
         String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
         throw new IllegalStateException(
            "This class has been loaded remotely via the "
               + protocol
               + " protocol. Only loading from a jar on the local file system is supported.");
      }

      int idx = uri.indexOf('!');
      // Sanity check
      if (idx == -1) {
         throw new IllegalStateException(
            "You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");
      }

      try {
         String fileName =
            URLDecoder.decode(
               uri.substring("jar:file:".length(), idx),
               Charset.defaultCharset().name());
         return new File(fileName).getAbsolutePath();
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException("Default charset doesn't exist. Your VM is borked.");
      }
   }

   private static void assertFilesExist(final List<File> expectedFiles) {
      final List<File> missingFiles = new ArrayList<>();
      for (final File file : expectedFiles) {
         if (file == null) {
            throw new RuntimeException("Null passed to assertFilesExist");
         }
         if (!file.exists()) {
            missingFiles.add(file);
         }
      }

      if (!missingFiles.isEmpty()) {
         List<String> missingAbsoluteFilenames = new ArrayList<>(missingFiles.size());
         for (File missingFile : missingFiles) {
            missingAbsoluteFilenames.add(missingFile.getAbsolutePath());
         }
         throw new RuntimeException(
            "The following files could not be located: "
               + String.join(", ", missingAbsoluteFilenames));
      }
   }

   private static String quote(final String str) {
      if (str.contains(" ")) {
         if (str.contains("\"")) {
            throw new RuntimeException(
               "Don't know how to quote a string containing a double-quote character "
                  + str);
         }
         return "\"" + str + "\"";
      }
      return str;
   }

   protected static final String CHECKER_BASE_PACKAGE = "org.checkerframework.checker";
   // Forward slash is used instead of File.separator because checker.jar uses / as the separator.
   protected static final String CHECKER_BASE_DIR_NAME = CHECKER_BASE_PACKAGE.replace(".", "/");

   protected static final String FULLY_QUALIFIED_SUBTYPING_CHECKER = "";

   protected static final String SUBTYPING_CHECKER_NAME = "";

   public static boolean matchesCheckerOrSubcheckerFromList(
      final String processorString, List<String> fullyQualifiedCheckerNames) {
      if (processorString.contains(",")) {
         return false; // Do not process strings containing multiple processors.
      }

      return fullyQualifiedCheckerNames.contains(
         unshorthandProcessorNames(processorString, fullyQualifiedCheckerNames, true));
   }

   protected void replaceShorthandProcessor(final List<String> args) {
      for (int i = 0; i < args.size(); i++) {
         final int nextIndex = i + 1;
         if (args.size() > nextIndex) {
            if (args.get(i).equals("-processor")) {
               final String replacement =
                  unshorthandProcessorNames(
                     args.get(nextIndex), getAllCheckerClassNames(), false);
               args.remove(nextIndex);
               args.add(nextIndex, replacement);
            }
         }
      }
   }

   private List<String> getAllCheckerClassNames() {
      ArrayList<String> checkerClassNames = new ArrayList<>();
      try {
         final JarInputStream checkerJarIs = new JarInputStream(new FileInputStream(checkerJar));
         ZipEntry entry;
         while ((entry = checkerJarIs.getNextEntry()) != null) {
            final String name = entry.getName();
            // Checkers ending in "Subchecker" are not included in this list used by
            // CheckerMain.
            if (name.startsWith(CHECKER_BASE_DIR_NAME) && name.endsWith("Checker.class")) {
               // Forward slash is used instead of File.separator because checker.jar uses / as
               // the separator.
               checkerClassNames.add(
                  PluginUtil.join(
                     ".",
                     name.substring(0, name.length() - ".class".length())
                        .split("/")));
            }
         }
         checkerJarIs.close();
      } catch (IOException e) {
         // When using CheckerDevelMain we might not have a checker.jar file built yet.
         // Issue a warning instead of aborting execution.
         System.err.printf(
            "Could not read %s. Shorthand processor names will not work.%n", checkerJar);
      }

      return checkerClassNames;
   }

   protected static String unshorthandProcessorNames(
      final String processorsString,
      List<String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
      final String[] processors = processorsString.split(",");
      for (int i = 0; i < processors.length; i++) {
         if (processors[i].equals(SUBTYPING_CHECKER_NAME)) { // Allow "subtyping" as well.
            processors[i] = FULLY_QUALIFIED_SUBTYPING_CHECKER;
         } else {
            if (!processors[i].contains(".")) { // Not already fully qualified
               processors[i] =
                  unshorthandProcessorName(
                     processors[i], fullyQualifiedCheckerNames, allowSubcheckers);
            }
         }
      }

      return PluginUtil.join(",", processors);
   }

   private static String unshorthandProcessorName(
      final String processor,
      List<String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
      for (final String name : fullyQualifiedCheckerNames) {
         boolean tryMatch = false;
         String[] checkerPath =
            name.substring(0, name.length() - "Checker".length()).split("\\.");
         String checkerNameShort = checkerPath[checkerPath.length - 1];
         String checkerName = checkerNameShort + "Checker";

         if (name.endsWith("Checker")) {
            checkerPath = name.substring(0, name.length() - "Checker".length()).split("\\.");
            checkerNameShort = checkerPath[checkerPath.length - 1];
            checkerName = checkerNameShort + "Checker";
            tryMatch = true;
         } else if (allowSubcheckers && name.endsWith("Subchecker")) {
            checkerPath = name.substring(0, name.length() - "Subchecker".length()).split("\\.");
            checkerNameShort = checkerPath[checkerPath.length - 1];
            checkerName = checkerNameShort + "Subchecker";
            tryMatch = true;
         }

         if (tryMatch) {
            if (processor.equalsIgnoreCase(checkerName)
               || processor.equalsIgnoreCase(checkerNameShort)) {
               return name;
            }
         }
      }

      return processor; // If not matched, return the input string.
   }

   public static boolean matchesFullyQualifiedProcessor(
      final String processor,
      List<String> fullyQualifiedCheckerNames,
      boolean allowSubcheckers) {
      return !processor.equals(
         unshorthandProcessorName(processor, fullyQualifiedCheckerNames, allowSubcheckers));
   }
}
