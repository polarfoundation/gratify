package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

/**
 * Converts a String to a Character.
 *
 * @author Keith Donald
 */
final class StringToCharacterConverter implements Converter<String, Character> {
   @Override
   public Character convert(String source) {
      if (source.isEmpty()) {
         return null;
      }
      if (source.length() > 1) {
         throw new IllegalArgumentException(
            "Can only convert a [String] with length of 1 to a [Character]; string value '" + source + "'  has length of " + source.length());
      }
      return source.charAt(0);
   }
}