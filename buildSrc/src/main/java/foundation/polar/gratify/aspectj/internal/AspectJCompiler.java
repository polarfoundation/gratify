package foundation.polar.gratify.aspectj.internal;
import foundation.polar.gratify.aspectj.AjcForkOptions;
import okio.BufferedSink;
import okio.Okio;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecHandle;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.JavaExecHandleFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class AspectJCompiler implements Compiler<AspectJCompileSpec>{
   private final JavaExecHandleFactory javaExecHandleFactory;

   public AspectJCompiler(JavaExecHandleFactory javaExecHandleFactory) {
      this.javaExecHandleFactory = javaExecHandleFactory;
   }

   @Override
   public WorkResult execute(AspectJCompileSpec spec) {
      ExecHandle handle = createCompilerHandle(spec);
      executeCompiler(handle);
      return WorkResults.didWork(true);
   }

   private ExecHandle createCompilerHandle(AspectJCompileSpec spec) {
      JavaExecHandleBuilder ajc = javaExecHandleFactory.newJavaExec();
      ajc.setWorkingDir(spec.getWorkingDir());
      ajc.setClasspath(spec.getAspectJClasspath());
      ajc.setMain("org.aspectj.tools.ajc.Main");

      AjcForkOptions forkOptions = spec.getAspectJCompileOptions().getForkOptions();

      ajc.setMinHeapSize(forkOptions.getMemoryInitialSize());
      ajc.setMaxHeapSize(forkOptions.getMemoryMaximumSize());
      ajc.jvmArgs(forkOptions.getJvmArgs());

      List<String> args = new LinkedList<>();

      if (!spec.getAspectJCompileOptions().getInpath().isEmpty()) {
         args.add("-inpath");
         args.add(getAsPath(spec.getAspectJCompileOptions().getInpath().getFiles()));
      }

      if (!spec.getAspectJCompileOptions().getAspectJPath().isEmpty()) {
         args.add("-aspectpath");
         args.add(getAsPath(spec.getAspectJCompileOptions().getAspectJPath().getFiles()));
      }

      if (spec.getAspectJCompileOptions().getOutJar().isPresent()) {
         args.add("-outjar");
         args.add(spec.getAspectJCompileOptions().getOutJar().get().getAsFile().getAbsolutePath());
      }

      if (spec.getAspectJCompileOptions().getOutxml().getOrElse(false)) {
         args.add("-outxml");
      }

      if (spec.getAspectJCompileOptions().getOutXmlFile().isPresent()) {
         args.add("-outxmlfile");
         args.add(spec.getAspectJCompileOptions().getOutXmlFile().get());
      }

      if (!spec.getAspectJCompileOptions().getSourceRoots().isEmpty()) {
         args.add("-sourceroots");
         args.add(spec.getAspectJCompileOptions().getSourceRoots().getAsPath());
      }

      if (spec.getAspectJCompileOptions().getCrossRefs().getOrElse(false)) {
         args.add("-crossrefs");
      }

      List<File> compileClasspath = spec.getCompileClasspath();
      if (compileClasspath != null && !compileClasspath.isEmpty()) {
         args.add("-classpath");
         args.add(getAsPath(compileClasspath));
      }

      if (!spec.getAspectJCompileOptions().getBootClassPath().isEmpty()) {
         args.add("-bootclasspath");
         args.add(getAsPath(spec.getAspectJCompileOptions().getBootClassPath().getFiles()));
      }

      if (!spec.getAspectJCompileOptions().getExtDirs().isEmpty()) {
         args.add("-extdirs");
         args.add(getAsPath(spec.getAspectJCompileOptions().getExtDirs().getFiles()));
      }

      if (spec.getDestinationDir() != null) {
         args.add("-d");
         args.add(spec.getDestinationDir().getAbsolutePath());
      }

      if (spec.getTargetCompatibility() != null) {
         args.add("-target");
         args.add(spec.getTargetCompatibility());
      }

      if (spec.getSourceCompatibility() != null) {
         args.add("-source");
         args.add(spec.getSourceCompatibility());
      }

      if (spec.getAspectJCompileOptions().getEncoding().isPresent()) {
         args.add("-encoding");
         args.add(spec.getAspectJCompileOptions().getEncoding().get());
      }

      if (spec.getAspectJCompileOptions().getVerbose().getOrElse(false)) {
         args.add("-verbose");
      }

      args.addAll(spec.getAspectJCompileOptions().getCompilerArgs());

      spec.getAspectJCompileOptions().getCompilerArgumentProviders()
         .forEach(commandLineArgumentProvider -> commandLineArgumentProvider.asArguments().forEach(args::add));

      if (spec.getSourceFiles() != null) {
         spec.getSourceFiles().forEach(sourceFile ->
            args.add(sourceFile.getAbsolutePath())
         );
      }

      File argFile = new File(spec.getTempDir(), "ajc.options");
      try {
         try (BufferedSink sink = Okio.buffer(Okio.sink(argFile, false))) {
            for (String arg : args) {
               sink.writeUtf8(arg);
               sink.writeUtf8(System.lineSeparator());
            }
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      ajc.args("-argfile", argFile.getAbsolutePath());

      ajc.setIgnoreExitValue(true);
      return ajc.build();
   }

   private void executeCompiler(ExecHandle handle) {
      handle.start();
      ExecResult result = handle.waitForFinish();
      if (result.getExitValue() != 0) {
         throw new CompilationFailedException(result.getExitValue());
      }
   }

   private String getAsPath(Collection<File> files) {
      return files.stream()
         .filter(File::exists)
         .map(File::getAbsolutePath)
         .collect(Collectors.joining(File.pathSeparator));
   }
}
