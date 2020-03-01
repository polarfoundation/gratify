package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.utils.StringUtils;

import java.util.TimeZone;

/**
 * Convert a String to a {@link TimeZone}.
 *
 * @author Stephane Nicoll
 */
class StringToTimeZoneConverter implements Converter<String, TimeZone> {
   @Override
   public TimeZone convert(String source) {
      return StringUtils.parseTimeZoneString(source);
   }
}
