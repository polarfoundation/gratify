package foundation.polar.gratify.artifacts.factory;

/**
 * Exception thrown when a bean instance has been requested for
 * a bean definition which has been marked as abstract.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#setAbstract
 */
@SuppressWarnings("serial")
public class ArtifactIsAbstractException extends ArtifactCreationException {
   /**
    * Create a new ArtifactIsAbstractException.
    * @param artifactName the name of the bean requested
    */
   public ArtifactIsAbstractException(String artifactName) {
      super(artifactName, "Artifact definition is abstract");
   }

}
