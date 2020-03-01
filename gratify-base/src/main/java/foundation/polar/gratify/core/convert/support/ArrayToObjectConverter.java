package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * Converts an array to an Object by returning the first array element
 * after converting it to the desired target type.
 *
 * @author Keith Donald
 */
final class ArrayToObjectConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public ArrayToObjectConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object[].class, Object.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }
      if (sourceType.isAssignableTo(targetType)) {
         return source;
      }
      if (Array.getLength(source) == 0) {
         return null;
      }
      Object firstElement = Array.get(source, 0);
      return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
   }

}
