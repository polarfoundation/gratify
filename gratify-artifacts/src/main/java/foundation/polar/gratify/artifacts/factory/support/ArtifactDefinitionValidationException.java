package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.FatalArtifactException;

/**
 * Exception thrown when the validation of a bean definition failed.
 *
 * @author Juergen Hoeller
 *
 * @see AbstractArtifactDefinition#validate()
 */
@SuppressWarnings("serial")
public class ArtifactDefinitionValidationException extends FatalArtifactException {
   /**
    * Create a new ArtifactDefinitionValidationException with the specified message.
    * @param msg the detail message
    */
   public ArtifactDefinitionValidationException(String msg) {
      super(msg);
   }

   /**
    * Create a new ArtifactDefinitionValidationException with the specified message
    * and root cause.
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactDefinitionValidationException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
