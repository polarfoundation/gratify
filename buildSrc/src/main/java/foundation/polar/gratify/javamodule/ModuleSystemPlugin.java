package foundation.polar.gratify.javamodule;

import foundation.polar.gratify.javamodule.extensions.DefaultModularityExtension;
import foundation.polar.gratify.javamodule.extensions.ModularityExtension;
import foundation.polar.gratify.javamodule.extensions.PatchModuleExtension;
import foundation.polar.gratify.javamodule.tasks.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;

public class ModuleSystemPlugin implements Plugin<Project> {
   @Override
   public void apply(Project project) {
      project.getPlugins().apply(JavaPlugin.class);
      ModuleName.findModuleName(project).ifPresent(moduleName -> configureModularity(project, moduleName));
   }

   private void configureModularity(Project project, String moduleName) {
      ExtensionContainer extensions = project.getExtensions();
      extensions.add("moduleName", moduleName);
      extensions.create("patchModules", PatchModuleExtension.class);
      extensions.create(ModularityExtension.class, "modularity", DefaultModularityExtension.class, project);
      new CompileTask(project).configureCompileJava();
      new CompileModuleInfoTask(project).configureCompileModuleInfoJava();
      new MergeClassesTask(project).configureMergeClasses();
      new CompileTestTask(project).configureCompileTestJava();
      new TestTask(project).configureTestJava();
      new RunTask(project).configureRun();
      new JavadocTask(project).configureJavaDoc();
      ModularJavaExec.configure(project);
      ModularCreateStartScripts.configure(project);
   }
}
