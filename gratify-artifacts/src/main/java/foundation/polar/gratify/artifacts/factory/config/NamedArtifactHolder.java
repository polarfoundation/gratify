package foundation.polar.gratify.artifacts.factory.config;


import foundation.polar.gratify.artifacts.factory.NamedArtifact;
import foundation.polar.gratify.utils.AssertUtils;

/**
 * A simple holder for a given artifact name plus artifact instance.
 *
 * @author Juergen Hoeller
 * @param <T> the artifact type
 * @see AutowireCapableArtifactFactory#resolveNamedArtifact(Class)
 */
public class NamedArtifactHolder<T> implements NamedArtifact {
   private final String artifactName;
   private final T artifactInstance;

   /**
    * Create a new holder for the given artifact name plus instance.
    * @param artifactName the name of the artifact
    * @param artifactInstance the corresponding artifact instance
    */
   public NamedArtifactHolder(String artifactName, T artifactInstance) {
      AssertUtils.notNull(artifactName, "Artifact name must not be null");
      this.artifactName = artifactName;
      this.artifactInstance = artifactInstance;
   }

   /**
    * Return the name of the artifact.
    */
   @Override
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the corresponding artifact instance.
    */
   public T getArtifactInstance() {
      return this.artifactInstance;
   }
}
