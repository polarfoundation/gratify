package foundation.polar.gratify.javamodule.tasks;

import foundation.polar.gratify.javamodule.extensions.CompileModuleOptions;
import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

abstract class AbstractCompileTask extends AbstractModulePluginTask {
   AbstractCompileTask(Project project) {
      super(project);
   }

   final CompileJavaTaskMutator createCompileJavaTaskMutator(JavaCompile compileJava, CompileModuleOptions moduleOptions) {
      return new CompileJavaTaskMutator(project, compileJava.getClasspath(), moduleOptions);
   }
}
