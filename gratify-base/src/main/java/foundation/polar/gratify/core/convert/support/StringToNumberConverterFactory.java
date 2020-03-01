package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.convert.converter.ConverterFactory;
import foundation.polar.gratify.utils.NumberUtils;

/**
 * Converts from a String any JDK-standard Number implementation.
 *
 * <p>Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#parseNumber(String, Class)} to perform the conversion.
 *
 * @author Keith Donald
 * @see java.lang.Byte
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 * @see NumberUtils
 */
final class StringToNumberConverterFactory implements ConverterFactory<String, Number> {

   @Override
   public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
      return new StringToNumber<>(targetType);
   }

   private static final class StringToNumber<T extends Number> implements Converter<String, T> {
      private final Class<T> targetType;

      public StringToNumber(Class<T> targetType) {
         this.targetType = targetType;
      }

      @Override
      public T convert(String source) {
         if (source.isEmpty()) {
            return null;
         }
         return NumberUtils.parseNumber(source, this.targetType);
      }
   }
}