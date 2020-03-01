package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

/**
 * Simply calls {@link Object#toString()} to convert any supported object
 * to a {@link String}.
 *
 * <p>Supports {@link CharSequence}, {@link StringWriter}, and any class
 * with a String constructor or one of the following static factory methods:
 * {@code valueOf(String)}, {@code of(String)}, {@code from(String)}.
 *
 * <p>Used by the {@link DefaultConversionService} as a fallback if there
 * are no other explicit to-String converters registered.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ObjectToObjectConverter
 */
final class FallbackObjectToStringConverter implements ConditionalGenericConverter {

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object.class, String.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      Class<?> sourceClass = sourceType.getObjectType();
      if (String.class == sourceClass) {
         // no conversion required
         return false;
      }
      return (CharSequence.class.isAssignableFrom(sourceClass) ||
         StringWriter.class.isAssignableFrom(sourceClass) ||
         ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceClass, String.class));
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return (source != null ? source.toString() : null);
   }
}
