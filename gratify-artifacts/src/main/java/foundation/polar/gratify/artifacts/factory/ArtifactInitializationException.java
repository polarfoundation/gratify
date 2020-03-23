package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.FatalArtifactException;

/**
 * Exception that a bean implementation is suggested to throw if its own
 * factory-aware initialization code fails. ArtifactsExceptions thrown by
 * bean factory methods themselves should simply be propagated as-is.
 *
 * <p>Note that {@code afterPropertiesSet()} or a custom "init-method"
 * can throw any exception.
 *
 * @author Juergen Hoeller
 * @see ArtifactFactoryAware#setArtifactFactory
 * @see InitializingArtifact#afterPropertiesSet
 */
@SuppressWarnings("serial")
public class ArtifactInitializationException extends FatalArtifactException {
   /**
    * Create a new ArtifactInitializationException with the specified message.
    * @param msg the detail message
    */
   public ArtifactInitializationException(String msg) {
      super(msg);
   }

   /**
    * Create a new ArtifactInitializationException with the specified message
    * and root cause.
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactInitializationException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
