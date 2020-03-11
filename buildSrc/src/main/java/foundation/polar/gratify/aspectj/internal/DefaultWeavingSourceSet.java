package foundation.polar.gratify.aspectj.internal;

import foundation.polar.gratify.aspectj.WeavingSourceSet;
import org.gradle.api.file.FileCollection;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;

public class DefaultWeavingSourceSet implements WeavingSourceSet, HasPublicType {
   private final String aspectJConfigurationName;
   private FileCollection aspectJPath;

   private final String inpathConfigurationName;
   private FileCollection inPath;

   public DefaultWeavingSourceSet(SourceSet sourceSet) {
      aspectJConfigurationName = sourceSet.getTaskName("", "aspect");
      inpathConfigurationName = sourceSet.getTaskName("", "inpath");
   }

   @Override
   public TypeOf<?> getPublicType() {
      return TypeOf.typeOf(WeavingSourceSet.class);
   }

   @Override
   public String getAspectJConfigurationName() {
      return aspectJConfigurationName;
   }

   @Override
   public FileCollection getAspectJPath() {
      return aspectJPath;
   }

   @Override
   public void setAspectJPath(FileCollection aspectJPath) {
      this.aspectJPath = aspectJPath;
   }

   @Override
   public String getInpathConfigurationName() {
      return inpathConfigurationName;
   }

   @Override
   public FileCollection getInPath() {
      return inPath;
   }

   @Override
   public void setInPath(FileCollection inPath) {
      this.inPath = inPath;
   }
}
