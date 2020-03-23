package foundation.polar.gratify.artifacts.factory;


/**
 * Exception thrown in case of a bean being requested despite
 * bean creation currently not being allowed (for example, during
 * the shutdown phase of a bean factory).
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactCreationNotAllowedException extends ArtifactCreationException{
   /**
    * Create a new ArtifactCreationNotAllowedException.
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    */
   public ArtifactCreationNotAllowedException(String artifactName, String msg) {
      super(artifactName, msg);
   }
}
