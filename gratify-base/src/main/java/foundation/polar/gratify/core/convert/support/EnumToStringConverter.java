package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.converter.Converter;

/**
 * Calls {@link Enum#name()} to convert a source Enum to a String.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Keith Donald
 * @author Phillip Webb
 */
final class EnumToStringConverter extends AbstractConditionalEnumConverter
   implements Converter<Enum<?>, String> {
   public EnumToStringConverter(ConversionService conversionService) {
      super(conversionService);
   }

   @Override
   public String convert(Enum<?> source) {
      return source.name();
   }
}