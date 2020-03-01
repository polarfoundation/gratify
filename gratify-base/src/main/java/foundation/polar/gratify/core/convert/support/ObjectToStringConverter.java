package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

/**
 * Simply calls {@link Object#toString()} to convert a source Object to a String.
 *
 * @author Keith Donald
 */
final class ObjectToStringConverter implements Converter<Object, String> {
   @Override
   public String convert(Object source) {
      return source.toString();
   }
}