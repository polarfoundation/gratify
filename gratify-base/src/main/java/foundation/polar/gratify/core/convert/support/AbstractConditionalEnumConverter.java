package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalConverter;
import foundation.polar.gratify.utils.ClassUtils;

/**
 * A {@link ConditionalConverter} base implementation for enum-based converters.
 *
 * @author Stephane Nicoll
 */
abstract class AbstractConditionalEnumConverter implements ConditionalConverter {
   private final ConversionService conversionService;
   protected AbstractConditionalEnumConverter(ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClassAsSet(sourceType.getType())) {
         if (this.conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
            return false;
         }
      }
      return true;
   }
}
