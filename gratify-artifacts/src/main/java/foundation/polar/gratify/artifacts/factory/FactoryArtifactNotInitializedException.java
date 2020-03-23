package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.artifacts.FatalArtifactException;

/**
 * Exception to be thrown from a FactoryArtifact's {@code getObject()} method
 * if the bean is not fully initialized yet, for example because it is involved
 * in a circular reference.
 *
 * <p>Note: A circular reference with a FactoryArtifact cannot be solved by eagerly
 * caching singleton instances like with normal beans. The reason is that
 * <i>every</i> FactoryArtifact needs to be fully initialized before it can
 * return the created bean, while only <i>specific</i> normal beans need
 * to be initialized - that is, if a collaborating bean actually invokes
 * them on initialization instead of just storing the reference.
 *
 * @author Juergen Hoeller
 * @see FactoryArtifact#getObject()
 */
public class FactoryArtifactNotInitializedException extends FatalArtifactException {
   /**
    * Create a new FactoryArtifactNotInitializedException with the default message.
    */
   public FactoryArtifactNotInitializedException() {
      super("FactoryArtifact is not fully initialized yet");
   }

   /**
    * Create a new FactoryArtifactNotInitializedException with the given message.
    * @param msg the detail message
    */
   public FactoryArtifactNotInitializedException(String msg) {
      super(msg);
   }
}
