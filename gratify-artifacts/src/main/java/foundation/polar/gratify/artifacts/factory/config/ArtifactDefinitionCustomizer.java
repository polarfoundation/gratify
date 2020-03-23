package foundation.polar.gratify.artifacts.factory.config;

/**
 * Callback for customizing a given bean definition.
 * Designed for use with a lambda expression or method reference.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.support.ArtifactDefinitionBuilder#applyCustomizers
 */
@FunctionalInterface
public interface ArtifactDefinitionCustomizer {

   /**
    * Customize the given bean definition.
    */
   void customize(ArtifactDefinition bd);

}