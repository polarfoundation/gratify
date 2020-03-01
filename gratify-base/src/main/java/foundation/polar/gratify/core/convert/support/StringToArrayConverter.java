package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalGenericConverter;
import foundation.polar.gratify.core.convert.converter.GenericConverter;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

/**
 * Converts a comma-delimited String to an Array.
 * Only matches if String.class can be converted to the target array element type.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 */
final class StringToArrayConverter implements ConditionalGenericConverter {

   private final ConversionService conversionService;

   public StringToArrayConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Object[].class));
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
      String string = (String) source;
      String[] fields = StringUtils.commaDelimitedListToStringArray(string);
      TypeDescriptor targetElementType = targetType.getElementTypeDescriptor();
      AssertUtils.state(targetElementType != null, "No target element type");
      Object target = Array.newInstance(targetElementType.getType(), fields.length);
      for (int i = 0; i < fields.length; i++) {
         String sourceElement = fields[i];
         Object targetElement = this.conversionService.convert(sourceElement.trim(), sourceType, targetElementType);
         Array.set(target, i, targetElement);
      }
      return target;
   }
}
