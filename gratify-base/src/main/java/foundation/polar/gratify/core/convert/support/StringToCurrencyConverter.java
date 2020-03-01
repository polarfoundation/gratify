package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

import java.util.Currency;

/**
 * Convert a String to a {@link Currency}.
 *
 * @author Stephane Nicoll
 */
class StringToCurrencyConverter implements Converter<String, Currency> {

   @Override
   public Currency convert(String source) {
      return Currency.getInstance(source);
   }
}
