package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exception thrown on an attempt to set the value of a property that
 * is not writable (typically because there is no setter method).
 *
 * @author Rod Johnson
 * @author Alef Arendsen
 * @author Arjen Poutsma
 */
@SuppressWarnings("serial")
public class NotWritablePropertyException extends InvalidPropertyException {

   @Nullable
   private final String[] possibleMatches;

   /**
    * Create a new NotWritablePropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property name
    */
   public NotWritablePropertyException(Class<?> beanClass, String propertyName) {
      super(beanClass, propertyName,
         "Artifact property '" + propertyName + "' is not writable or has an invalid setter method: " +
            "Does the return type of the getter match the parameter type of the setter?");
      this.possibleMatches = null;
   }

   /**
    * Create a new NotWritablePropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property name
    * @param msg the detail message
    */
   public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg) {
      super(beanClass, propertyName, msg);
      this.possibleMatches = null;
   }

   /**
    * Create a new NotWritablePropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property name
    * @param msg the detail message
    * @param cause the root cause
    */
   public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, Throwable cause) {
      super(beanClass, propertyName, msg, cause);
      this.possibleMatches = null;
   }

   /**
    * Create a new NotWritablePropertyException.
    * @param beanClass the offending bean class
    * @param propertyName the offending property name
    * @param msg the detail message
    * @param possibleMatches suggestions for actual bean property names
    * that closely match the invalid property name
    */
   public NotWritablePropertyException(Class<?> beanClass, String propertyName, String msg, String[] possibleMatches) {
      super(beanClass, propertyName, msg);
      this.possibleMatches = possibleMatches;
   }


   /**
    * Return suggestions for actual bean property names that closely match
    * the invalid property name, if any.
    */
   @Nullable
   public String[] getPossibleMatches() {
      return this.possibleMatches;
   }

}
