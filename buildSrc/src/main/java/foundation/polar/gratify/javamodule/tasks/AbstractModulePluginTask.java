package foundation.polar.gratify.javamodule.tasks;

import foundation.polar.gratify.javamodule.JavaProjectHelper;
import foundation.polar.gratify.javamodule.utils.MergeClassesHelper;
import org.gradle.api.Project;

abstract class AbstractModulePluginTask {
   protected final Project project;
   AbstractModulePluginTask(Project project) {
      this.project = project;
   }
   protected final JavaProjectHelper helper() {
      return new JavaProjectHelper(project);
   }
   protected final MergeClassesHelper mergeClassesHelper() {
      return new MergeClassesHelper(project);
   }
}
