package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Converts an array to another array. First adapts the source array to a List,
 * then delegates to {@link CollectionToArrayConverter} to perform the target
 * array conversion.
 *
 * @author Keith Donald
 * @author Phillip Webb
 */
final class ArrayToArrayConverter implements ConditionalGenericConverter {
   private final CollectionToArrayConverter helperConverter;
   private final ConversionService conversionService;

   public ArrayToArrayConverter(ConversionService conversionService) {
      this.helperConverter = new CollectionToArrayConverter(conversionService);
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object[].class, Object[].class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return this.helperConverter.matches(sourceType, targetType);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (this.conversionService instanceof GenericConversionService) {
         TypeDescriptor targetElement = targetType.getElementTypeDescriptor();
         if (targetElement != null &&
            ((GenericConversionService) this.conversionService).canBypassConvert(
               sourceType.getElementTypeDescriptor(), targetElement)) {
            return source;
         }
      }
      List<Object> sourceList = Arrays.asList(ObjectUtils.toObjectArray(source));
      return this.helperConverter.convert(sourceList, sourceType, targetType);
   }
}
