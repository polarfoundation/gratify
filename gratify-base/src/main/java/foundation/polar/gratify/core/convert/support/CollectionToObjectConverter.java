package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Converts a Collection to an Object by returning the first collection element after converting it to the desired targetType.
 *
 * @author Keith Donald
 */
final class CollectionToObjectConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public CollectionToObjectConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Collection.class, Object.class));
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
      Collection<?> sourceCollection = (Collection<?>) source;
      if (sourceCollection.isEmpty()) {
         return null;
      }
      Object firstElement = sourceCollection.iterator().next();
      return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
   }

}
