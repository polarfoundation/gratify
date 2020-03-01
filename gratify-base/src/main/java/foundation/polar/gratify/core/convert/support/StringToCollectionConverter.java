package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.CollectionFactory;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Converts a comma-delimited String to a Collection.
 * If the target collection element type is declared, only matches if
 * {@code String.class} can be converted to it.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class StringToCollectionConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public StringToCollectionConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return (targetType.getElementTypeDescriptor() == null ||
         this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor()));
   }

   @Override
   @Nullable
   public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
         return null;
      }
      String string = (String) source;

      String[] fields = StringUtils.commaDelimitedListToStringArray(string);
      TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
      Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
         (elementDesc != null ? elementDesc.getType() : null), fields.length);

      if (elementDesc == null) {
         for (String field : fields) {
            target.add(field.trim());
         }
      }
      else {
         for (String field : fields) {
            Object targetElement = this.conversionService.convert(field.trim(), sourceType, elementDesc);
            target.add(targetElement);
         }
      }
      return target;
   }
}
