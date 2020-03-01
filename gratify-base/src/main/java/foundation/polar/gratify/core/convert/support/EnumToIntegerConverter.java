package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.converter.Converter;

/**
 * Calls {@link Enum#ordinal()} to convert a source Enum to a Integer.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Yanming Zhou
 */
final class EnumToIntegerConverter extends AbstractConditionalEnumConverter
   implements Converter<Enum<?>, Integer> {

   public EnumToIntegerConverter(ConversionService conversionService) {
      super(conversionService);
   }

   @Override
   public Integer convert(Enum<?> source) {
      return source.ordinal();
   }
}

