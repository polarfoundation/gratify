package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown on an unrecoverable problem encountered in the
 * beans packages or sub-packages, e.g. bad class or field.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class FatalArtifactException extends ArtifactsException {
   /**
    * Create a new FatalBeanException with the specified message.
    * @param msg the detail message
    */
   public FatalArtifactException(String msg) {
      super(msg);
   }

   /**
    * Create a new FatalBeanException with the specified message
    * and root cause.
    * @param msg the detail message
    * @param cause the root cause
    */
   public FatalArtifactException(String msg, @Nullable Throwable cause) {
      super(msg, cause);
   }
}
