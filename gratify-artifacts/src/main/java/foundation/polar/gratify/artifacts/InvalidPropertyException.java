package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown when referring to an invalid bean property.
 * Carries the offending bean class and property name.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class InvalidPropertyException extends FatalArtifactException {

   private final Class<?> artifactClass;
   private final String propertyName;

   /**
    * Create a new InvalidPropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property
    * @param msg the detail message
    */
   public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg) {
      this(beanClass, propertyName, msg, null);
   }

   /**
    * Create a new InvalidPropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property
    * @param msg the detail message
    * @param cause the root cause
    */
   public InvalidPropertyException(Class<?> beanClass, String propertyName, String msg, @Nullable Throwable cause) {
      super("Invalid property '" + propertyName + "' of bean class [" + beanClass.getName() + "]: " + msg, cause);
      this.artifactClass = beanClass;
      this.propertyName = propertyName;
   }

   /**
    * Return the offending bean class.
    */
   public Class<?> getArtifactClass() {
      return this.artifactClass;
   }

   /**
    * Return the name of the offending property.
    */
   public String getPropertyName() {
      return this.propertyName;
   }
}
