package foundation.polar.gratify.artifacts.factory;

/**
 * Exception thrown when a bean is not a factory, but a user tries to get
 * at the factory for the given bean name. Whether a bean is a factory is
 * determined by whether it implements the FactoryArtifact interface.
 *
 * @author Rod Johnson
 * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact
 */
@SuppressWarnings("serial")
public class ArtifactIsNotAFactoryException extends ArtifactNotOfRequiredTypeException {
   /**
    * Create a new ArtifactIsNotAFactoryException.
    * @param name the name of the bean requested
    * @param actualType the actual type returned, which did not match
    * the expected type
    */
   public ArtifactIsNotAFactoryException(String name, Class<?> actualType) {
      super(name, FactoryArtifact.class, actualType);
   }
}
