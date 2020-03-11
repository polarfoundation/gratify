package foundation.polar.gratify.aspectj;

import org.gradle.api.file.FileCollection;

public interface WeavingSourceSet {
   String getAspectJConfigurationName();
   String getInpathConfigurationName();
   FileCollection getAspectJPath();
   void setAspectJPath(FileCollection aspectPath);
   FileCollection getInPath();
   void setInPath(FileCollection inPath);
}
