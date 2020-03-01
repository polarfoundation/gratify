package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.CollectionFactory;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Converts from a Collection to another Collection.
 *
 * <p>First, creates a new Collection of the requested targetType with a size equal to the
 * size of the source Collection. Then copies each element in the source collection to the
 * target collection. Will perform an element conversion from the source collection's
 * parameterized type to the target collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class CollectionToCollectionConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public CollectionToCollectionConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Collection.class, Collection.class));
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
      Collection<?> sourceCollection = (Collection<?>) source;

      // Shortcut if possible...
      boolean copyRequired = !targetType.getType().isInstance(source);
      if (!copyRequired && sourceCollection.isEmpty()) {
         return source;
      }
      TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
      if (elementDesc == null && !copyRequired) {
         return source;
      }

      // At this point, we need a collection copy in any case, even if just for finding out about element copies...
      Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
         (elementDesc != null ? elementDesc.getType() : null), sourceCollection.size());

      if (elementDesc == null) {
         target.addAll(sourceCollection);
      }
      else {
         for (Object sourceElement : sourceCollection) {
            Object targetElement = this.conversionService.convert(sourceElement,
               sourceType.elementTypeDescriptor(sourceElement), elementDesc);
            target.add(targetElement);
            if (sourceElement != targetElement) {
               copyRequired = true;
            }
         }
      }
      return (copyRequired ? target : source);
   }
}
