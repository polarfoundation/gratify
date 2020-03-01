package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Converts an array to a comma-delimited String. First adapts the source array
 * to a List, then delegates to {@link CollectionToStringConverter} to perform
 * the target String conversion.
 *
 * @author Keith Donald
 */
final class ArrayToStringConverter implements ConditionalGenericConverter {

   private final CollectionToStringConverter helperConverter;

   public ArrayToStringConverter(ConversionService conversionService) {
      this.helperConverter = new CollectionToStringConverter(conversionService);
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.helperConverter.matches(sourceType, targetType);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.helperConverter.convert(Arrays.asList(ObjectUtils.toObjectArray(source)), sourceType, targetType);
   }

}
