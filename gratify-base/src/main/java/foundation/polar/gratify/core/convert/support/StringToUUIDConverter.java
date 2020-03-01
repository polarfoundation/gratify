package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.utils.StringUtils;

import java.util.UUID;

/**
 * Converts from a String to a {@link java.util.UUID}.
 *
 * @author Phillip Webb
 * @see UUID#fromString
 */
final class StringToUUIDConverter implements Converter<String, UUID> {
   @Override
   public UUID convert(String source) {
      return (StringUtils.hasText(source) ? UUID.fromString(source.trim()) : null);
   }
}