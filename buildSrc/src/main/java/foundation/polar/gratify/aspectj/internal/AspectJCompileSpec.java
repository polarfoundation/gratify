package foundation.polar.gratify.aspectj.internal;

import foundation.polar.gratify.aspectj.AspectJCompileOptions;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.tasks.compile.DefaultJvmLanguageCompileSpec;

public class AspectJCompileSpec extends DefaultJvmLanguageCompileSpec {
   private FileCollection aspectJClasspath;
   AspectJCompileOptions aspectJCompileOptions;

   public FileCollection getAspectJClasspath() {
      return aspectJClasspath;
   }

   public void setAspectJClasspath(FileCollection aspectJClasspath) {
      this.aspectJClasspath = aspectJClasspath;
   }

   public AspectJCompileOptions getAspectJCompileOptions() {
      return aspectJCompileOptions;
   }

   public void setAspectJCompileOptions(AspectJCompileOptions aspectJCompileOptions) {
      this.aspectJCompileOptions = aspectJCompileOptions;
   }
}
