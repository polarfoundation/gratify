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
 * Converts an Object to a single-element Collection containing the Object.
 * Will convert the Object to the target Collection's parameterized type if necessary.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class ObjectToCollectionConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public ObjectToCollectionConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(Object.class, Collection.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return ConversionUtils.canConvertElements(sourceType, targetType.getElementTypeDescriptor(), this.conversionService);
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }

      TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
      Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
         (elementDesc != null ? elementDesc.getType() : null), 1);

      if (elementDesc == null || elementDesc.isCollection()) {
         target.add(source);
      }
      else {
         Object singleElement = this.conversionService.convert(source, sourceType, elementDesc);
         target.add(singleElement);
      }
      return target;
   }
}
