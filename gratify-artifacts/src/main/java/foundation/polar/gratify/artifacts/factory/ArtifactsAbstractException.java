package foundation.polar.gratify.artifacts.factory;

/**
 * Exception thrown when a bean instance has been requested for
 * a bean definition which has been marked as abstract.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#setAbstract
 */
@SuppressWarnings("serial")
public class ArtifactsAbstractException extends ArtifactCreationException {
   /**
    * Create a new ArtifactsAbstractException.
    * @param artifactName the name of the artifact requested
    */
   public ArtifactsAbstractException(String artifactName) {
      super(artifactName, "Artifact definition is abstract");
   }
}
