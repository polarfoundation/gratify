package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor;

public interface MergedArtifactDefinitionPostProcessor extends ArtifactPostProcessor {
   /**
    * Post-process the given merged artifact definition for the specified artifact.
    * @param artifactDefinition the merged artifact definition for the artifact
    * @param artifactType the actual type of the managed artifact instance
    * @param artifactName the name of the artifact
    * @see AbstractAutowireCapableArtifactFactory#applyMergedArtifactDefinitionPostProcessors
    */
   void postProcessMergedArtifactDefinition(RootArtifactDefinition artifactDefinition, Class<?> artifactType, String artifactName);

   /**
    * A notification that the artifact definition for the specified name has been reset,
    * and that this post-processor should clear any metadata for the affected artifact.
    * <p>The default implementation is empty.
    * @param artifactName the name of the artifact
    * @see DefaultListableArtifactFactory#resetArtifactDefinition
    */
   default void resetArtifactDefinition(String artifactName) {
   }
}
