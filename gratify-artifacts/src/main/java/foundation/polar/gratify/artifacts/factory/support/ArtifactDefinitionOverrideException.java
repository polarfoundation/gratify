package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Subclass of {@link ArtifactDefinitionStoreException} indicating an invalid override
 * attempt: typically registering a new definition for the same artifact name while
 * {@link DefaultListableArtifactFactory#isAllowArtifactDefinitionOverriding()} is {@code false}.
 *
 * @author Juergen Hoeller
 *
 * @see DefaultListableArtifactFactory#setAllowArtifactDefinitionOverriding
 * @see DefaultListableArtifactFactory#registerArtifactDefinition
 */
@SuppressWarnings("serial")
public class ArtifactDefinitionOverrideException extends ArtifactDefinitionStoreException {
   private final ArtifactDefinition artifactDefinition;

   private final ArtifactDefinition existingDefinition;

   /**
    * Create a new ArtifactDefinitionOverrideException for the given new and existing definition.
    * @param artifactName the name of the artifact
    * @param artifactDefinition the newly registered artifact definition
    * @param existingDefinition the existing artifact definition for the same name
    */
   public ArtifactDefinitionOverrideException(
      String artifactName, ArtifactDefinition artifactDefinition, ArtifactDefinition existingDefinition) {

      super(artifactDefinition.getResourceDescription(), artifactName,
         "Cannot register artifact definition [" + artifactDefinition + "] for artifact '" + artifactName +
            "': There is already [" + existingDefinition + "] bound.");
      this.artifactDefinition = artifactDefinition;
      this.existingDefinition = existingDefinition;
   }

   /**
    * Return the description of the resource that the artifact definition came from.
    */
   @Override
   @NonNull
   public String getResourceDescription() {
      return String.valueOf(super.getResourceDescription());
   }

   /**
    * Return the name of the artifact.
    */
   @Override
   @NonNull
   public String getArtifactName() {
      return String.valueOf(super.getArtifactName());
   }

   /**
    * Return the newly registered artifact definition.
    * @see #getArtifactName()
    */
   public ArtifactDefinition getArtifactDefinition() {
      return this.artifactDefinition;
   }

   /**
    * Return the existing artifact definition for the same name.
    * @see #getArtifactName()
    */
   public ArtifactDefinition getExistingDefinition() {
      return this.existingDefinition;
   }
}
