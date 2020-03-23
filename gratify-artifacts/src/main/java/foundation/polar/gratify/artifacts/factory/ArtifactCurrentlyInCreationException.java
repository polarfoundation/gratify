package foundation.polar.gratify.artifacts.factory;

/**
 * Exception thrown in case of a reference to a bean that's currently in creation.
 * Typically happens when constructor autowiring matches the currently constructed bean.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactCurrentlyInCreationException extends ArtifactCreationException {
   /**
    * Create a new ArtifactCurrentlyInCreationException,
    * with a default error message that indicates a circular reference.
    * @param artifactName the name of the bean requested
    */
   public ArtifactCurrentlyInCreationException(String artifactName) {
      super(artifactName,
         "Requested bean is currently in creation: Is there an unresolvable circular reference?");
   }

   /**
    * Create a new ArtifactCurrentlyInCreationException.
    * @param artifactName the name of the bean requested
    * @param msg the detail message
    */
   public ArtifactCurrentlyInCreationException(String artifactName, String msg) {
      super(artifactName, msg);
   }

}
