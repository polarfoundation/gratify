package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyChangeEvent;

/**
 * Exception thrown when no suitable editor or converter can be found for a bean property.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ConversionNotSupportedException extends TypeMismatchException {

   /**
    * Create a new ConversionNotSupportedException.
    * @param propertyChangeEvent the PropertyChangeEvent that resulted in the problem
    * @param requiredType the required target type (or {@code null} if not known)
    * @param cause the root cause (may be {@code null})
    */
   public ConversionNotSupportedException(PropertyChangeEvent propertyChangeEvent,
                                          @Nullable Class<?> requiredType, @Nullable Throwable cause) {
      super(propertyChangeEvent, requiredType, cause);
   }

   /**
    * Create a new ConversionNotSupportedException.
    * @param value the offending value that couldn't be converted (may be {@code null})
    * @param requiredType the required target type (or {@code null} if not known)
    * @param cause the root cause (may be {@code null})
    */
   public ConversionNotSupportedException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
      super(value, requiredType, cause);
   }

}
