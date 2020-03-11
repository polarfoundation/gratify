package foundation.polar.gratify.aspectj;

import foundation.polar.gratify.aspectj.internal.AspectJCompileSpec;
import foundation.polar.gratify.aspectj.internal.AspectJCompiler;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.process.internal.JavaExecHandleFactory;

import java.io.File;
import java.util.ArrayList;

public class AjcAction implements Action<Task> {
   private final ConfigurableFileCollection classpath;
   private final Property<Boolean> enabled;
   private final AspectJCompileOptions options;
   private final JavaExecHandleFactory javaExecHandleFactory;

   public AjcAction(ObjectFactory objectFactory, JavaExecHandleFactory javaExecHandleFactory) {
      options = new AspectJCompileOptions(objectFactory);
      classpath = objectFactory.fileCollection();
      enabled = objectFactory.property(Boolean.class).convention(true);
      this.javaExecHandleFactory = javaExecHandleFactory;
   }

   public void options(Action<AspectJCompileOptions> action) {
      action.execute(getOptions());
   }

   public ConfigurableFileCollection getClasspath() {
      return classpath;
   }

   public Property<Boolean> getEnabled() {
      return enabled;
   }

   public AspectJCompileOptions getOptions() {
      return options;
   }

   JavaExecHandleFactory getJavaExecHandleFactory() {
      return javaExecHandleFactory;
   }

   @SuppressWarnings("WeakerAccess")
   public void addToTask(Task task) {
      task.doLast("ajc", this);
      task.getExtensions().add("ajc", this);

      task.getInputs().files(this.getClasspath())
         .withPropertyName("aspectjClasspath")
         .withNormalizer(ClasspathNormalizer.class)
         .optional(false);

      task.getInputs().files(this.getOptions().getAspectJPath())
         .withPropertyName("aspectpath")
         .withNormalizer(ClasspathNormalizer.class)
         .optional(true);

      task.getInputs().files(this.getOptions().getInpath())
         .withPropertyName("ajcInpath")
         .withNormalizer(ClasspathNormalizer.class)
         .optional(true);

      task.getInputs().property("ajcArgs", this.getOptions().getCompilerArgs())
         .optional(true);

      task.getInputs().property("ajcEnabled", this.getEnabled())
         .optional(true);
   }

   @Override
   public void execute(Task task) {
      if (!enabled.getOrElse(true)) {
         return;
      }

      AspectJCompileSpec spec = createSpec((AbstractCompile) task);

      new AspectJCompiler(javaExecHandleFactory).execute(spec);
   }

   private AspectJCompileSpec createSpec(AbstractCompile compile) {
      AspectJCompileSpec spec = new AspectJCompileSpec();

      spec.setDestinationDir(compile.getDestinationDir());
      spec.setWorkingDir(compile.getProject().getProjectDir());
      spec.setTempDir(compile.getTemporaryDir());
      spec.setCompileClasspath(new ArrayList<>(compile.getClasspath().filter(File::exists).getFiles()));
      spec.setTargetCompatibility(compile.getTargetCompatibility());
      spec.setSourceCompatibility(compile.getSourceCompatibility());

      spec.setAspectJClasspath(getClasspath());
      spec.setAspectJCompileOptions(getOptions());

      return spec;
   }
}
