package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;

/**
 * Interface that defines type conversion methods. Typically (but not necessarily)
 * implemented in conjunction with the {@link PropertyEditorRegistry} interface.
 *
 * <p><b>Note:</b> Since TypeConverter implementations are typically based on
 * {@link java.beans.PropertyEditor PropertyEditors} which aren't thread-safe,
 * TypeConverters themselves are <em>not</em> to be considered as thread-safe either.
 *
 * @author Juergen Hoeller
 * @see SimpleTypeConverter
 * @see ArtifactWrapperImpl
 */
public interface TypeConverter {
   /**
    * Convert the value to the required type (if necessary from a String).
    * <p>Conversions from String to any type will typically use the {@code setAsText}
    * method of the PropertyEditor class, or a Gratify Converter in a ConversionService.
    * @param value the value to convert
    * @param requiredType the type we must convert to
    * (or {@code null} if not known, for example in case of a collection element)
    * @return the new value, possibly the result of type conversion
    * @throws TypeMismatchException if type conversion failed
    * @see java.beans.PropertyEditor#setAsText(String)
    * @see java.beans.PropertyEditor#getValue()
    * @see foundation.polar.gratify.core.convert.ConversionService
    * @see foundation.polar.gratify.core.convert.converter.Converter
    */
   @Nullable
   <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;

   /**
    * Convert the value to the required type (if necessary from a String).
    * <p>Conversions from String to any type will typically use the {@code setAsText}
    * method of the PropertyEditor class, or a Gratify Converter in a ConversionService.
    * @param value the value to convert
    * @param requiredType the type we must convert to
    * (or {@code null} if not known, for example in case of a collection element)
    * @param methodParam the method parameter that is the target of the conversion
    * (for analysis of generic types; may be {@code null})
    * @return the new value, possibly the result of type conversion
    * @throws TypeMismatchException if type conversion failed
    * @see java.beans.PropertyEditor#setAsText(String)
    * @see java.beans.PropertyEditor#getValue()
    * @see foundation.polar.gratify.core.convert.ConversionService
    * @see foundation.polar.gratify.core.convert.converter.Converter
    */
   @Nullable
   <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                            @Nullable MethodParameter methodParam) throws TypeMismatchException;

   /**
    * Convert the value to the required type (if necessary from a String).
    * <p>Conversions from String to any type will typically use the {@code setAsText}
    * method of the PropertyEditor class, or a Gratify Converter in a ConversionService.
    * @param value the value to convert
    * @param requiredType the type we must convert to
    * (or {@code null} if not known, for example in case of a collection element)
    * @param field the reflective field that is the target of the conversion
    * (for analysis of generic types; may be {@code null})
    * @return the new value, possibly the result of type conversion
    * @throws TypeMismatchException if type conversion failed
    * @see java.beans.PropertyEditor#setAsText(String)
    * @see java.beans.PropertyEditor#getValue()
    * @see foundation.polar.gratify.core.convert.ConversionService
    * @see foundation.polar.gratify.core.convert.converter.Converter
    */
   @Nullable
   <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
      throws TypeMismatchException;

   /**
    * Convert the value to the required type (if necessary from a String).
    * <p>Conversions from String to any type will typically use the {@code setAsText}
    * method of the PropertyEditor class, or a Gratify Converter in a ConversionService.
    * @param value the value to convert
    * @param requiredType the type we must convert to
    * (or {@code null} if not known, for example in case of a collection element)
    * @param typeDescriptor the type descriptor to use (may be {@code null}))
    * @return the new value, possibly the result of type conversion
    * @throws TypeMismatchException if type conversion failed
    * @see java.beans.PropertyEditor#setAsText(String)
    * @see java.beans.PropertyEditor#getValue()
    * @see foundation.polar.gratify.core.convert.ConversionService
    * @see foundation.polar.gratify.core.convert.converter.Converter
    */
   @Nullable
   default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                    @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

      throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
   }
}
