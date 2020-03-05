package foundation.polar.gratify.javamodule.tasks;
import foundation.polar.gratify.javamodule.JavaProjectHelper;
import foundation.polar.gratify.javamodule.utils.MergeClassesHelper;
import org.apache.tools.ant.taskdefs.Java;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.JavaExec;

import java.util.Objects;

abstract class AbstractExecutionMutator {
   private static final Logger LOGGER = Logging.getLogger(AbstractExecutionMutator.class);
   protected final JavaExec execTask;
   protected final Project project;

   AbstractExecutionMutator(JavaExec execTask, Project project) {
      this.execTask = execTask;
      this.project = project;
   }

   protected final String getMainClassName() {
      String mainClassName = Objects.requireNonNull(
         execTask.getMain(),
         "Main class name not found. Try setting 'application.mainClassName' in your Gradle build file."
      );
      if (!mainClassName.contains("/")) {
         LOGGER.warn("No module was provided for main class, assuming the current module. Prefer providing 'mainClassName' " +
            "in the following format: '$moduleName/a.b.Main'");
         return helper().moduleName() + "/" + mainClassName;
      }
      return mainClassName;
   }

   protected final JavaProjectHelper helper() {
      return new JavaProjectHelper(project);
   }

   protected final MergeClassesHelper mergeClassesHelper() {
      return new MergeClassesHelper(project);
   }
}
