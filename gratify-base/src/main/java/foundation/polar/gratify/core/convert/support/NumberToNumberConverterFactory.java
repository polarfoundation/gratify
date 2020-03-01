package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.ConditionalConverter;
import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.convert.converter.ConverterFactory;
import foundation.polar.gratify.utils.NumberUtils;

/**
 * Converts from any JDK-standard Number implementation to any other JDK-standard Number implementation.
 *
 * <p>Support Number classes including Byte, Short, Integer, Float, Double, Long, BigInteger, BigDecimal. This class
 * delegates to {@link NumberUtils#convertNumberToTargetClass(Number, Class)} to perform the conversion.
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
final class NumberToNumberConverterFactory implements ConverterFactory<Number, Number>, ConditionalConverter {

   @Override
   public <T extends Number> Converter<Number, T> getConverter(Class<T> targetType) {
      return new NumberToNumber<>(targetType);
   }

   @Override
   public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      return !sourceType.equals(targetType);
   }

   private static final class NumberToNumber<T extends Number> implements Converter<Number, T> {
      private final Class<T> targetType;

      public NumberToNumber(Class<T> targetType) {
         this.targetType = targetType;
      }

      @Override
      public T convert(Number source) {
         return NumberUtils.convertNumberToTargetClass(source, this.targetType);
      }
   }

}
