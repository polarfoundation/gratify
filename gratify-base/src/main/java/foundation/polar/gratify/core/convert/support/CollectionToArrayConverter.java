package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class CollectionToArrayConverter implements ConditionalGenericConverter {
   private final ConversionService conversionService;


   public CollectionToArrayConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }


   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Collection.class, Object[].class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(),
         targetType.getElementTypeDescriptor(), this.conversionService);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }
      Collection<?> sourceCollection = (Collection<?>) source;
      TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
      AssertUtils.state(targetElementType != null, "No target element type");
      Object array = Array.newInstance(targetElementType.getType(), sourceCollection.size());
      int i = 0;
      for (Object sourceElement : sourceCollection) {
         Object targetElement = this.conversionService.convert(sourceElement,
            sourceType.elementTypeDescriptor(sourceElement), targetElementType);
         Array.set(array, i++, targetElement);
      }
      return array;
   }
}
