package foundation.polar.gratify.aspectj;

import foundation.polar.gratify.aspectj.internal.AspectJCompileSpec;
import foundation.polar.gratify.aspectj.internal.AspectJCompiler;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.process.internal.JavaExecHandleFactory;

import java.util.ArrayList;

@CacheableTask
public class AspectjCompile extends AbstractCompile {
   @Classpath
   private final ConfigurableFileCollection aspectJClassPath = getProject().getObjects().fileCollection();

   @Nested
   private final CompileOptions options = getProject().getObjects().newInstance(CompileOptions.class);

   @Nested
   private final AspectJCompileOptions ajcOptions = getProject().getObjects().newInstance(AspectJCompileOptions.class);

   /**
    * {@inheritDoc}
    */
   @Override
   @InputFiles
   @SkipWhenEmpty
   @PathSensitive(PathSensitivity.RELATIVE)
   public FileTree getSource() {
      return super.getSource();
   }

   @Override
   @CompileClasspath
   public FileCollection getClasspath() {
      return super.getClasspath();
   }

   @TaskAction
   protected void compile() {
      getProject().delete(getDestinationDir());

      AspectJCompileSpec spec = createSpec();
      WorkResult result = getCompiler().execute(spec);
      setDidWork(result.getDidWork());
   }

   private AspectJCompiler getCompiler() {
      return new AspectJCompiler(getServices().get(JavaExecHandleFactory.class));
   }

   private AspectJCompileSpec createSpec() {
      AspectJCompileSpec spec = new AspectJCompileSpec();
      spec.setSourceFiles(getSource());
      spec.setDestinationDir(getDestinationDir());
      spec.setWorkingDir(getProject().getProjectDir());
      spec.setTempDir(getTemporaryDir());
      spec.setCompileClasspath(new ArrayList<>(getClasspath().getFiles()));
      spec.setSourceCompatibility(getSourceCompatibility());
      spec.setTargetCompatibility(getTargetCompatibility());
      spec.setAspectJClasspath(getAspectJClassPath());
      spec.setAspectJCompileOptions(getAjcOptions());

      return spec;
   }

   public ConfigurableFileCollection getAspectJClassPath() {
      return aspectJClassPath;
   }

   public CompileOptions getOptions() {
      return options;
   }

   public AspectJCompileOptions getAjcOptions() {
      return ajcOptions;
   }
}
