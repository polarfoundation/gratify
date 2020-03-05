package foundation.polar.gratify.javamodule.tasks;

import foundation.polar.gratify.javamodule.extensions.CompileModuleOptions;
import foundation.polar.gratify.javamodule.utils.CompileModuleInfoHelper;
import foundation.polar.gratify.javamodule.utils.MergeClassesHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.Optional;

public class CompileTask extends AbstractCompileTask {
   public CompileTask(Project project) {
      super(project);
   }

   /**
    * @see CompileModuleInfoTask#configureCompileModuleInfoJava()
    */
   public void configureCompileJava() {
      helper().findTask(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class)
      .ifPresent(this::configureCompileJava);
   }

   private void configureCompileJava(JavaCompile javaCompile) {
      var moduleOptions = javaCompile.getExtensions().create("moduleOptions", CompileModuleOptions.class, project);
      project.afterEvaluate(p -> {
         MergeClassesHelper.POST_JAVA_COMPILE_TASK_NAMES.stream()
            .map(name -> helper().findTask(name, AbstractCompile.class))
            .flatMap(Optional::stream)
            .filter(task -> !task.getSource().isEmpty())
            .findAny()
            .ifPresent(task -> moduleOptions.setCompileModuleInfoSeparately(true));
         if (moduleOptions.getCompileModuleInfoSeparately()) {
            javaCompile.exclude("module-info.java");
         } else {
            configureModularityForCompileJava(javaCompile, moduleOptions);
         }
      });
   }

   /**
    * @see CompileModuleInfoTask#configureModularityForCompileModuleInfoJava
    */
   void configureModularityForCompileJava(JavaCompile javaCompile, CompileModuleOptions moduleOptions) {
      CompileModuleInfoHelper.dependOnOtherCompileModuleInfoJavaTasks(javaCompile);
      CompileJavaTaskMutator mutator = createCompileJavaTaskMutator(javaCompile, moduleOptions);
      // don't convert to lambda: https://github.com/java9-modularity/gradle-modules-plugin/issues/54
      javaCompile.doFirst(new Action<Task>() {
         @Override
         public void execute(Task task) {
            mutator.modularizeJavaCompileTask(javaCompile);
         }
      });
   }
}
