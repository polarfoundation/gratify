package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

/**
 * Converts from any JDK-standard Number implementation to a Character.
 *
 * @author Keith Donald
 * @see java.lang.Character
 * @see java.lang.Short
 * @see java.lang.Integer
 * @see java.lang.Long
 * @see java.math.BigInteger
 * @see java.lang.Float
 * @see java.lang.Double
 * @see java.math.BigDecimal
 */
final class NumberToCharacterConverter implements Converter<Number, Character> {
   @Override
   public Character convert(Number source) {
      return (char) source.shortValue();
   }
}