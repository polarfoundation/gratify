package foundation.polar.gratify.artifacts.factory.parsing;


import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactReference;

/**
 * Base implementation of {@link ComponentDefinition} that provides a basic implementation of
 * {@link #getDescription} which delegates to {@link #getName}. Also provides a base implementation
 * of {@link #toString} which delegates to {@link #getDescription} in keeping with the recommended
 * implementation strategy. Also provides default implementations of {@link #getInnerArtifactDefinitions}
 * and {@link #getArtifactReferences} that return an empty array.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class AbstractComponentDefinition implements ComponentDefinition {
   /**
    * Delegates to {@link #getName}.
    */
   @Override
   public String getDescription() {
      return getName();
   }

   /**
    * Returns an empty array.
    */
   @Override
   public ArtifactDefinition[] getArtifactDefinitions() {
      return new ArtifactDefinition[0];
   }

   /**
    * Returns an empty array.
    */
   @Override
   public ArtifactDefinition[] getInnerArtifactDefinitions() {
      return new ArtifactDefinition[0];
   }

   /**
    * Returns an empty array.
    */
   @Override
   public ArtifactReference[] getArtifactReferences() {
      return new ArtifactReference[0];
   }

   /**
    * Delegates to {@link #getDescription}.
    */
   @Override
   public String toString() {
      return getDescription();
   }
}
