package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.artifacts.ArtifactsException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyChangeEvent;

/**
 * Superclass for exceptions related to a property access,
 * such as type mismatch or invocation target exception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class PropertyAccessException extends ArtifactsException {

   @Nullable
   private final PropertyChangeEvent propertyChangeEvent;

   /**
    * Create a new PropertyAccessException.
    * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
    * @param msg the detail message
    * @param cause the root cause
    */
   public PropertyAccessException(PropertyChangeEvent propertyChangeEvent, String msg, @Nullable Throwable cause) {
      super(msg, cause);
      this.propertyChangeEvent = propertyChangeEvent;
   }

   /**
    * Create a new PropertyAccessException without PropertyChangeEvent.
    * @param msg the detail message
    * @param cause the root cause
    */
   public PropertyAccessException(String msg, @Nullable Throwable cause) {
      super(msg, cause);
      this.propertyChangeEvent = null;
   }

   /**
    * Return the PropertyChangeEvent that resulted in the problem.
    * <p>May be {@code null}; only available if an actual bean property
    * was affected.
    */
   @Nullable
   public PropertyChangeEvent getPropertyChangeEvent() {
      return this.propertyChangeEvent;
   }

   /**
    * Return the name of the affected property, if available.
    */
   @Nullable
   public String getPropertyName() {
      return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getPropertyName() : null);
   }

   /**
    * Return the affected value that was about to be set, if any.
    */
   @Nullable
   public Object getValue() {
      return (this.propertyChangeEvent != null ? this.propertyChangeEvent.getNewValue() : null);
   }

   /**
    * Return a corresponding error code for this type of exception.
    */
   public abstract String getErrorCode();

}
