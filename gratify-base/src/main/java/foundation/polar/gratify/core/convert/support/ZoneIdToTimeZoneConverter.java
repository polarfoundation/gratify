package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Simple converter from Java 8's {@link java.time.ZoneId} to {@link java.util.TimeZone}.
 *
 * <p>Note that Spring's default ConversionService setup understands the 'from'/'to' convention
 * that the JSR-310 {@code java.time} package consistently uses. That convention is implemented
 * reflectively in {@link ObjectToObjectConverter}, not in specific JSR-310 converters.
 * It covers {@link java.util.TimeZone#toZoneId()} as well, and also
 * {@link java.util.Date#from(java.time.Instant)} and {@link java.util.Date#toInstant()}.
 *
 * @author Juergen Hoeller
 * @see TimeZone#getTimeZone(java.time.ZoneId)
 */
final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {
   @Override
   public TimeZone convert(ZoneId source) {
      return TimeZone.getTimeZone(source);
   }
}