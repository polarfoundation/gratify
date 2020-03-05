package foundation.polar.gratify.javamodule.tasks;

import foundation.polar.gratify.javamodule.JavaProjectHelper;
import foundation.polar.gratify.javamodule.extensions.CompileModuleOptions;
import foundation.polar.gratify.javamodule.extensions.PatchModuleExtension;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

class CompileJavaTaskMutator {
   private final Project project;

   /**
    * {@linkplain JavaCompile#getClasspath() Classpath} of {@code compileJava} task.
    */
   private final FileCollection compileJavaClasspath;

   /**
    * {@link CompileModuleOptions} of {@code compileJava} task.
    */
   private final CompileModuleOptions compileModuleOptions;

   CompileJavaTaskMutator(Project project, FileCollection compileJavaClasspath,
                          CompileModuleOptions moduleOptions) {
      this.project = project;
      this.compileJavaClasspath = compileJavaClasspath;
      this.compileModuleOptions = moduleOptions;
   }

   /**
    * The argument is a {@link JavaCompile} task whose modularity is to be configured.
    *
    * @param javaCompile {@code compileJava} if {@link CompileModuleOptions#getCompileModuleInfoSeparately()}
    *                    is {@code false}, {@code compileModuleInfoJava} if it is {@code true}
    */
   void modularizeJavaCompileTask(JavaCompile javaCompile) {
      List<String> compilerArgs = buildCompilerArgs(javaCompile);
      javaCompile.getOptions().setCompilerArgs(compilerArgs);
      javaCompile.setClasspath(project.files());
      configureSourcepath(javaCompile);
   }

   // Setting the sourcepath is necessary when using forked compilation for module-info.java
   void configureSourcepath(JavaCompile javaCompile) {
      var sourcePaths = project.files(helper().mainSourceSet().getJava().getSrcDirs());
      javaCompile.getOptions().setSourcepath(sourcePaths);
   }

   // Setting the sourcepath is necessary when using forked compilation for module-info.java

   private List<String> buildCompilerArgs(JavaCompile javaCompile) {
      var compilerArgs = new ArrayList<>(javaCompile.getOptions().getCompilerArgs());
      var patchModuleExtension = helper().extension(PatchModuleExtension.class);
      patchModuleExtension.buildModulePathOption(compileJavaClasspath)
         .ifPresent(option -> option.mutateArgs(compilerArgs));
      patchModuleExtension.resolvePatched(compileJavaClasspath).mutateArgs(compilerArgs);
      compileModuleOptions.mutateArgs(compilerArgs);
      return compilerArgs;
   }

   private JavaProjectHelper helper() {
      return new JavaProjectHelper(project);
   }
}
