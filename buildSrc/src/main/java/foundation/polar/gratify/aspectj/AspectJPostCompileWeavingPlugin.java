package foundation.polar.gratify.aspectj;

import foundation.polar.gratify.aspectj.internal.DefaultWeavingSourceSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.scala.ScalaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.scala.ScalaCompile;

public class AspectJPostCompileWeavingPlugin implements Plugin<Project> {

   private Project project;
   private AspectJBasePlugin aspectjBasePlugin;

   @Override
   public void apply(Project project) {
      this.project = project;
      aspectjBasePlugin = project.getPlugins().apply(AspectJBasePlugin.class);

      project.getPlugins().apply(JavaBasePlugin.class);

      project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(this::configureSourceSet);
   }

   private void configureSourceSet(SourceSet sourceSet) {
      project.afterEvaluate(p ->
         p.getDependencies().add(sourceSet.getImplementationConfigurationName(), "org.aspectj:aspectjrt:" + aspectjBasePlugin.getAspectJExtension().getVersion().get())
      );

      DefaultWeavingSourceSet weavingSourceSet = new DefaultWeavingSourceSet(sourceSet);
      new DslObject(sourceSet).getConvention().add("aspectj", weavingSourceSet);

      Configuration aspectpath = project.getConfigurations().create(weavingSourceSet.getAspectJConfigurationName());
      weavingSourceSet.setAspectJPath(aspectpath);

      Configuration inpath = project.getConfigurations().create(weavingSourceSet.getInpathConfigurationName());
      weavingSourceSet.setInPath(inpath);

      project.getConfigurations().getByName(sourceSet.getImplementationConfigurationName())
         .extendsFrom(aspectpath);

      project.getConfigurations().getByName(sourceSet.getCompileOnlyConfigurationName()).extendsFrom(inpath);

      project.getPlugins().withType(JavaPlugin.class, javaPlugin ->
         project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class, compileJava -> {
            AjcAction ajcAction = enhanceWithWeavingAction(compileJava, aspectpath, inpath, aspectjBasePlugin.getAspectJConfiguration());
            ajcAction.getOptions().getBootClassPath().from(compileJava.getOptions().getBootstrapClasspath());
            ajcAction.getOptions().getExtDirs().from(compileJava.getOptions().getExtensionDirs());
         })
      );

      project.getPlugins().withType(GroovyPlugin.class, groovyPlugin ->
         project.getTasks().named(sourceSet.getCompileTaskName("groovy"), GroovyCompile.class, compileGroovy -> {
            AjcAction ajcAction = enhanceWithWeavingAction(compileGroovy, aspectpath, inpath, aspectjBasePlugin.getAspectJConfiguration());
            ajcAction.getOptions().getBootClassPath().from(compileGroovy.getOptions().getBootstrapClasspath());
            ajcAction.getOptions().getExtDirs().from(compileGroovy.getOptions().getExtensionDirs());
         })
      );

      project.getPlugins().withType(ScalaBasePlugin.class, scalaBasePlugin ->
         project.getTasks().named(sourceSet.getCompileTaskName("scala"), ScalaCompile.class, compileScala -> {
            AjcAction ajcAction = enhanceWithWeavingAction(compileScala, aspectpath, inpath, aspectjBasePlugin.getAspectJConfiguration());
            ajcAction.getOptions().getBootClassPath().from(compileScala.getOptions().getBootstrapClasspath());
            ajcAction.getOptions().getExtDirs().from(compileScala.getOptions().getExtensionDirs());
         })
      );

      project.getPlugins().withId("org.jetbrains.kotlin.jvm", kotlinPlugin ->
         project.getTasks().named(sourceSet.getCompileTaskName("kotlin"), AbstractCompile.class, compileKotlin ->
            enhanceWithWeavingAction(compileKotlin, aspectpath, inpath, aspectjBasePlugin.getAspectJConfiguration())
         )
      );
   }

   private AjcAction enhanceWithWeavingAction(AbstractCompile abstractCompile, Configuration aspectpath, Configuration inpath, Configuration aspectjConfiguration) {
      AjcAction action = project.getObjects().newInstance(AjcAction.class);

      action.getOptions().getAspectJPath().from(aspectpath);
      action.getOptions().getInpath().from(abstractCompile.getDestinationDir());
      action.getOptions().getInpath().from(inpath);
      action.getClasspath().from(aspectjConfiguration);

      action.addToTask(abstractCompile);

      return action;
   }
}

