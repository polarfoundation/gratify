package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.convert.converter.ConverterFactory;

/**
 * Converts from a Integer to a {@link java.lang.Enum} by calling {@link Class#getEnumConstants()}.
 *
 * @author Yanming Zhou
 * @author Stephane Nicoll
 * @since 4.3
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {

   @Override
   public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
      return new IntegerToEnum(ConversionUtils.getEnumType(targetType));
   }

   private static class IntegerToEnum<T extends Enum> implements Converter<Integer, T> {
      private final Class<T> enumType;
      public IntegerToEnum(Class<T> enumType) {
         this.enumType = enumType;
      }
      @Override
      public T convert(Integer source) {
         return this.enumType.getEnumConstants()[source];
      }
   }
}