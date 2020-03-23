package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.utils.ClassUtils;

/**
 * Thrown when a bean doesn't match the expected type.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactNotOfRequiredTypeException extends ArtifactsException {

   /** The name of the instance that was of the wrong type. */
   private final String artifactName;

   /** The required type. */
   private final Class<?> requiredType;

   /** The offending type. */
   private final Class<?> actualType;

   /**
    * Create a new ArtifactNotOfRequiredTypeException.
    * @param artifactName the name of the bean requested
    * @param requiredType the required type
    * @param actualType the actual type returned, which did not match
    * the expected type
    */
   public ArtifactNotOfRequiredTypeException(String artifactName, Class<?> requiredType, Class<?> actualType) {
      super("Artifact named '" + artifactName + "' is expected to be of type '" + ClassUtils.getQualifiedName(requiredType) +
         "' but was actually of type '" + ClassUtils.getQualifiedName(actualType) + "'");
      this.artifactName = artifactName;
      this.requiredType = requiredType;
      this.actualType = actualType;
   }

   /**
    * Return the name of the instance that was of the wrong type.
    */
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Return the expected type for the bean.
    */
   public Class<?> getRequiredType() {
      return this.requiredType;
   }

   /**
    * Return the actual type of the instance found.
    */
   public Class<?> getActualType() {
      return this.actualType;
   }
}
