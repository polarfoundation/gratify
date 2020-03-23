package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.NestedRuntimeException;

/**
 * Abstract superclass for all exceptions thrown in the beans package
 * and subpackages.
 *
 * <p>Note that this is a runtime (unchecked) exception. Artifacts exceptions
 * are usually fatal; there is no reason for them to be checked.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class ArtifactsException extends NestedRuntimeException {
   /**
    * Create a new ArtifactsException with the specified message.
    * @param msg the detail message
    */
   public ArtifactsException(String msg) {
      super(msg);
   }

   /**
    * Create a new ArtifactsException with the specified message
    * and root cause.
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactsException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
