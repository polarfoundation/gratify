package foundation.polar.gratify.aspectj;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public class AspectJBasePlugin implements Plugin<Project> {
   private Configuration aspectJConfiguration;
   private AspectJExtension aspectJExtension;

   @Override
   public void apply(Project project) {
      aspectJExtension = project.getExtensions().create("aspectj", AspectJExtension.class);

      aspectJConfiguration = project.getConfigurations().create("aspectj");

      aspectJConfiguration.defaultDependencies(dependencies -> {
         dependencies.add(project.getDependencies().create("org.aspectj:aspectjtools:" + aspectJExtension.getVersion().get()));
      });

      project.getTasks().withType(AspectjCompile.class).configureEach(aspectjCompile -> {
         aspectjCompile.getAspectJClassPath().from(aspectJConfiguration);
      });
   }

   public Configuration getAspectJConfiguration() {
      return aspectJConfiguration;
   }

   public AspectJExtension getAspectJExtension() {
      return aspectJExtension;
   }
}
