package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;

/**
 * Default implementation of the {@link ArtifactNameGenerator} interface, delegating to
 * {@link ArtifactDefinitionReaderUtils#generateArtifactName(ArtifactDefinition, ArtifactDefinitionRegistry)}.
 *
 * @author Juergen Hoeller
 */
public class DefaultArtifactNameGenerator implements ArtifactNameGenerator {

   /**
    * A convenient constant for a default {@code DefaultArtifactNameGenerator} instance,
    * as used for {@link AbstractArtifactDefinitionReader} setup.
    */
   public static final DefaultArtifactNameGenerator INSTANCE = new DefaultArtifactNameGenerator();

   @Override
   public String generateArtifactName(ArtifactDefinition definition, ArtifactDefinitionRegistry registry) {
      return ArtifactDefinitionReaderUtils.generateArtifactName(definition, registry);
   }

}