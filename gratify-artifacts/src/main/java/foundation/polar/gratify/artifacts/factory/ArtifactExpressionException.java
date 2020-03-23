package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.FatalArtifactException;

/**
 * Exception that indicates an expression evaluation attempt having failed.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactExpressionException extends FatalArtifactException {
   /**
    * Create a new ArtifactExpressionException with the specified message.
    * @param msg the detail message
    */
   public ArtifactExpressionException(String msg) {
      super(msg);
   }

   /**
    * Create a new ArtifactExpressionException with the specified message
    * and root cause.
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactExpressionException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
