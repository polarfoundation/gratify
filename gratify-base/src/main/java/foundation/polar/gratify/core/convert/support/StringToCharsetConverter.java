package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

import java.nio.charset.Charset;

/**
 * Convert a String to a {@link Charset}.
 *
 * @author Stephane Nicoll
 */
class StringToCharsetConverter implements Converter<String, Charset> {
   @Override
   public Charset convert(String source) {
      return Charset.forName(source);
   }
}