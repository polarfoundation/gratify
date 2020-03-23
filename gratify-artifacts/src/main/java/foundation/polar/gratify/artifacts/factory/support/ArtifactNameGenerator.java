package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;

/**
 * Strategy interface for generating artifact names for artifact definitions.
 *
 * @author Juergen Hoeller
 */
public interface ArtifactNameGenerator {
   /**
    * Generate a artifact name for the given artifact definition.
    * @param definition the artifact definition to generate a name for
    * @param registry the artifact definition registry that the given definition
    * is supposed to be registered with
    * @return the generated artifact name
    */
   String generateArtifactName(ArtifactDefinition definition, ArtifactDefinitionRegistry registry);
}
