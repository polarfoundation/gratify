package foundation.polar.gratify.utils.compiler;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import foundation.polar.gratify.lang.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginUtil {
   public static final String CHECKER_QUAL_PATH_OPT = "-checkerQualJar";
   public static final String JAVAC_PATH_OPT = "-javacJar";
   public static final String JDK_PATH_OPT = "-jdkJar";
   private static final String LINE_SEPARATOR = System.lineSeparator();

   public static List<File> toFiles(final List<String> fileNames) {
      final List<File> files = new ArrayList<>(fileNames.size());
      for (final String fn : fileNames) {
         files.add(new File(fn));
      }

      return files;
   }

   public static void writeFofn(final File destination, final List<File> files)
      throws IOException {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
      try {
         for (final File file : files) {
            bw.write(wrapArg(file.getAbsolutePath()));
            bw.newLine();
         }
         bw.flush();
      } finally {
         bw.close();
      }
   }

   public static void writeFofn(final File destination, final File... files) throws IOException {
      writeFofn(destination, Arrays.asList(files));
   }

   public static File writeTmpFofn(
      final String prefix,
      final String suffix,
      final boolean deleteOnExit,
      final List<File> files)
      throws IOException {
      final File tmpFile = File.createTempFile(prefix, suffix);
      if (deleteOnExit) {
         tmpFile.deleteOnExit();
      }
      writeFofn(tmpFile, files);
      return tmpFile;
   }

   public static File writeTmpFile(
      final String prefix,
      final String suffix,
      final boolean deleteOnExit,
      final List<String> args)
      throws IOException {
      final File tmpFile = File.createTempFile(prefix, suffix);
      if (deleteOnExit) {
         tmpFile.deleteOnExit();
      }
      writeFile(tmpFile, args);
      return tmpFile;
   }

   public static void writeFile(final File destination, final List<String> contents)
      throws IOException {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(destination));
      try {
         for (String line : contents) {
            bw.write(line);
            bw.newLine();
         }
         bw.flush();
      } finally {
         bw.close();
      }
   }

   public static List<String> readFile(final File argFile) throws IOException {
      final BufferedReader br = new BufferedReader(new FileReader(argFile));
      String line;

      List<String> lines = new ArrayList<>();
      while ((line = br.readLine()) != null) {
         lines.add(line);
      }
      br.close();
      return lines;
   }

   public static <T> String join(final CharSequence delimiter, final T[] objs) {
      if (objs == null) {
         return "null";
      }
      final StringJoiner sb = new StringJoiner(delimiter);
      for (final Object obj : objs) {
         sb.add(Objects.toString(obj));
      }
      return sb.toString();
   }

   public static String join(CharSequence delimiter, Iterable<?> values) {
      if (values == null) {
         return "null";
      }
      StringJoiner sb = new StringJoiner(delimiter);
      for (Object value : values) {
         sb.add(Objects.toString(value));
      }
      return sb.toString();
   }

   @SafeVarargs
   @SuppressWarnings("varargs")
   public static <T> String joinLines(T... a) {
      return join(LINE_SEPARATOR, a);
   }

   public static String joinLines(Iterable<? extends Object> v) {
      return join(LINE_SEPARATOR, v);
   }

   public static List<String> getStringProp(
      final Map<CheckerProp, Object> props,
      final CheckerProp prop,
      final String cmdLineArgStart,
      final String... extras) {
      final List<String> out = new ArrayList<>();
      final String strProp = (String) props.get(prop);
      if (strProp != null && !strProp.isEmpty()) {
         out.add(cmdLineArgStart + strProp);
         for (final String extra : extras) {
            out.add(extra);
         }
      }

      return out;
   }

   public static List<String> getBooleanProp(
      final Map<CheckerProp, Object> props, final CheckerProp prop, final String cmdLineArg) {
      Boolean aSkip = (Boolean) props.get(prop);
      if (aSkip != null && aSkip) {
         return Arrays.asList(cmdLineArg);
      }
      return new ArrayList<>();
   }

   public enum CheckerProp {
      MISC_COMPILER() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            @SuppressWarnings("unchecked")
            List<String> miscOpts = (List<String>) props.get(this);

            if (miscOpts != null && !miscOpts.isEmpty()) {
               return new ArrayList<>(miscOpts);
            }
            return new ArrayList<>();
         }
      },

      A_SKIP() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getStringProp(props, this, "-AskipUses=");
         }
      },

      A_LINT() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getStringProp(props, this, "-Alint=");
         }
      },

      A_WARNS() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getBooleanProp(props, this, "-Awarns");
         }
      },
      A_NO_MSG_TXT() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getBooleanProp(props, this, "-Anomsgtext");
         }
      },
      A_SHOW_CHECKS() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getBooleanProp(props, this, "-Ashowchecks");
         }
      },
      A_FILENAMES() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getBooleanProp(props, this, "-Afilenames");
         }
      },
      A_DETAILED_MSG() {
         @Override
         public List<String> getCmdLine(final Map<CheckerProp, Object> props) {
            return getBooleanProp(props, this, "-Adetailedmsgtext");
         }
      };
      public abstract List<String> getCmdLine(final Map<CheckerProp, Object> props);
   }

   private static void addOptions(final List<String> cmd, Map<CheckerProp, Object> props) {
      for (CheckerProp cp : CheckerProp.values()) {
         cmd.addAll(cp.getCmdLine(props));
      }
   }

   public static boolean getBooleanSystemProperty(String key) {
      return Boolean.valueOf(System.getProperty(key, "false"));
   }

   public static boolean getBooleanSystemProperty(String key, boolean defaultValue) {
      String value = System.getProperty(key);
      if (value == null) {
         return defaultValue;
      }
      if (value.equals("true")) {
         return true;
      }
      if (value.equals("false")) {
         return false;
      }
      throw new Error(
         String.format(
            "Value for system property %s should be boolean, but is \"%s\".",
            key, value));
   }

   public static File writeTmpSrcFofn(
      final String prefix, final boolean deleteOnExit, final List<File> files)
      throws IOException {
      return writeTmpFofn(prefix, ".src_files", deleteOnExit, files);
   }

   public static File writeTmpCpFile(
      final String prefix, final boolean deleteOnExit, final String classpath)
      throws IOException {
      return writeTmpFile(
         prefix,
         ".classpath",
         deleteOnExit,
         Arrays.asList("-classpath", wrapArg(classpath)));
   }

   public static boolean isWindows() {
      final String os = System.getProperty("os.name");
      return os.toLowerCase().contains("win");
   }

   public static String wrapArg(final String classpath) {
      if (classpath.contains(" ")) {
         return '"' + escapeQuotesAndSlashes(classpath) + '"';
      }
      return classpath;
   }

   public static String escapeQuotesAndSlashes(final String toEscape) {
      final Map<String, String> replacements = new HashMap<>();
      replacements.put("\\\\", "\\\\\\\\");
      replacements.put("\"", "\\\\\"");

      String replacement = toEscape;
      for (final Map.Entry<String, String> entry : replacements.entrySet()) {
         replacement = replacement.replaceAll(entry.getKey(), entry.getValue());
      }

      return replacement;
   }

   public static String getJavaCommand(final String javaHome, final PrintStream out) {
      if (javaHome == null || javaHome.equals("")) {
         return "java";
      }

      final File java = new File(javaHome, "bin" + File.separator + "java");
      final File javaExe = new File(javaHome, "bin" + File.separator + "java.exe");
      if (java.exists()) {
         return java.getAbsolutePath();
      } else if (javaExe.exists()) {
         return javaExe.getAbsolutePath();
      } else {
         if (out != null) {
            out.printf(
               "Could not find java executable at: (%s,%s)%n  Using \"java\" command.%n",
               java.getAbsolutePath(), javaExe.getAbsolutePath());
         }
         return "java";
      }
   }

   public static String fileArgToStr(final File fileArg) {
      return "@" + fileArg.getAbsolutePath();
   }

   public static List<String> getCmd(
      final @Nullable String executable,
      final @Nullable File javacPath,
      final @Nullable File jdkPath,
      final File srcFofn,
      final String processors,
      final String checkerHome,
      final String javaHome,
      final File classPathFofn,
      final String bootClassPath,
      final Map<CheckerProp, Object> props,
      PrintStream out,
      final boolean procOnly,
      final String outputDirectory) {

      final List<String> cmd = new ArrayList<>();

      final String java = (executable != null) ? executable : getJavaCommand(javaHome, out);

      cmd.add(java);
      cmd.add("-jar");
      cmd.add(checkerHome);

      if (procOnly) {
         cmd.add("-proc:only");
      } else if (outputDirectory != null) {
         cmd.add("-d");
         cmd.add(outputDirectory);
      }

      if (bootClassPath != null && !bootClassPath.trim().isEmpty()) {
         cmd.add("-Xbootclasspath/p:" + bootClassPath);
      }

      if (javacPath != null) {
         cmd.add(JAVAC_PATH_OPT);
         cmd.add(javacPath.getAbsolutePath());
      }

      if (jdkPath != null) {
         cmd.add(JDK_PATH_OPT);
         cmd.add(jdkPath.getAbsolutePath());
      }

      if (classPathFofn != null) {
         cmd.add(fileArgToStr(classPathFofn));
      }

      if (processors != null) {
         cmd.add("-processor");
         cmd.add(processors);
      }

      addOptions(cmd, props);
      cmd.add(fileArgToStr(srcFofn));

      return cmd;
   }

   public static List<String> toJavaOpts(final List<String> opts) {
      final List<String> outOpts = new ArrayList<>(opts.size());
      for (final String opt : opts) {
         outOpts.add("-J" + opt);
      }

      return outOpts;
   }

   public static List<String> getCmdArgsOnly(
      final File srcFofn,
      final String processors,
      final String checkerHome,
      final String javaHome,
      final File classpathFofn,
      final String bootClassPath,
      final Map<CheckerProp, Object> props,
      PrintStream out,
      final boolean procOnly,
      final String outputDirectory) {
      final List<String> cmd =
         getCmd(
            null,
            null,
            null,
            srcFofn,
            processors,
            checkerHome,
            javaHome,
            classpathFofn,
            bootClassPath,
            props,
            out,
            procOnly,
            outputDirectory);
      cmd.remove(0);
      return cmd;
   }

   public static List<String> getCmdArgsOnly(
      final File javacPath,
      final File jdkPath,
      final File srcFofn,
      final String processors,
      final String checkerHome,
      final String javaHome,
      final File classpathFofn,
      final String bootClassPath,
      final Map<CheckerProp, Object> props,
      PrintStream out,
      final boolean procOnly,
      final String outputDirectory) {

      final List<String> cmd =
         getCmd(
            null,
            javacPath,
            jdkPath,
            srcFofn,
            processors,
            checkerHome,
            javaHome,
            classpathFofn,
            bootClassPath,
            props,
            out,
            procOnly,
            outputDirectory);
      cmd.remove(0);
      return cmd;
   }

   public static int getJreVersion() {
      final String jreVersionStr = System.getProperty("java.version");
      final Pattern oldVersionPattern = Pattern.compile("^1\\.(\\d+)\\..*$");
      final Matcher oldVersionMatcher = oldVersionPattern.matcher(jreVersionStr);
      if (oldVersionMatcher.matches()) {
         String v = oldVersionMatcher.group(1);
         assert v != null : "@AssumeAssertion(nullness): inspection";
         return Integer.parseInt(v);
      }

      // See http://openjdk.java.net/jeps/223
      // We only care about the major version number.
      final Pattern newVersionPattern = Pattern.compile("^(\\d+).*$");
      final Matcher newVersionMatcher = newVersionPattern.matcher(jreVersionStr);
      if (newVersionMatcher.matches()) {
         String v = newVersionMatcher.group(1);
         assert v != null : "@AssumeAssertion(nullness): inspection";
         return Integer.parseInt(v);
      }

      // For Early Access version of the JDK
      final Pattern eaVersionPattern = Pattern.compile("^(\\d+)-ea$");
      final Matcher eaVersionMatcher = eaVersionPattern.matcher(jreVersionStr);
      if (eaVersionMatcher.matches()) {
         String v = eaVersionMatcher.group(1);
         assert v != null : "@AssumeAssertion(nullness): inspection";
         return Integer.parseInt(v);
      }

      throw new RuntimeException(
         "Could not determine version from property java.version=" + jreVersionStr);
   }

   public static String getJdkJarPrefix() {
      final int jreVersion = getJreVersion();
      final String prefix;

      if (jreVersion < 8) {
         throw new AssertionError("Unsupported JRE version: " + jreVersion);
      } else {
         prefix = "jdk" + jreVersion;
      }

      return prefix;
   }

   public static String getJdkJarName() {
      final String fileName = getJdkJarPrefix() + ".jar";
      return fileName;
   }

   public static @Nullable String getReleaseValue(ProcessingEnvironment env) {
      Context ctx = ((JavacProcessingEnvironment) env).getContext();
      Options options = Options.instance(ctx);
      return options.get(Option.RELEASE);
   }
}
