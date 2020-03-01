package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.convert.converter.ConverterFactory;

/**
 * Converts from a String to a {@link java.lang.Enum} by calling {@link Enum#valueOf(Class, String)}.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 */
@SuppressWarnings({"rawtypes", "unchecked"})
final class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

   @Override
   public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
      return new StringToEnum(ConversionUtils.getEnumType(targetType));
   }

   private static class StringToEnum<T extends Enum> implements Converter<String, T> {
      private final Class<T> enumType;
      public StringToEnum(Class<T> enumType) {
         this.enumType = enumType;
      }
      @Override
      public T convert(String source) {
         if (source.isEmpty()) {
            // It's an empty enum identifier: reset the enum value to null.
            return null;
         }
         return (T) Enum.valueOf(this.enumType, source.trim());
      }
   }
}
