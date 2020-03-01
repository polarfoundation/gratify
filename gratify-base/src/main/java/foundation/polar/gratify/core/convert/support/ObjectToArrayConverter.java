package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.core.convert.converter.GenericConverter;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * Converts an Object to a single-element array containing the Object.
 * Will convert the Object to the target array's component type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class ObjectToArrayConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public ObjectToArrayConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new GenericConverter.ConvertiblePair(Object.class, Object[].class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(),
         this.conversionService);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }
      TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
      AssertUtils.state(targetElementType != null, "No target element type");
      Object target = Array.newInstance(targetElementType.getType(), 1);
      Object targetElement = this.conversionService.convert(source, sourceType, targetElementType);
      Array.set(target, 0, targetElement);
      return target;
   }
}

