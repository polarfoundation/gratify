package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.CollectionFactory;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Converts an array to a Collection.
 *
 * <p>First, creates a new Collection of the requested target type.
 * Then adds each array element to the target collection.
 * Will perform an element conversion from the source component type
 * to the collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {
   private final ConversionService conversionService;

   public ArrayToCollectionConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return ConversionUtils.canConvertElements(
         sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }

      int length = Array.getLength(source);
      TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
      Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
         (elementDesc != null ? elementDesc.getType() : null), length);

      if (elementDesc == null) {
         for (int i = 0; i < length; i++) {
            Object sourceElement = Array.get(source, i);
            target.add(sourceElement);
         }
      }
      else {
         for (int i = 0; i < length; i++) {
            Object sourceElement = Array.get(source, i);
            Object targetElement = this.conversionService.convert(sourceElement,
               sourceType.elementTypeDescriptor(sourceElement), elementDesc);
            target.add(targetElement);
         }
      }
      return target;
   }
}
