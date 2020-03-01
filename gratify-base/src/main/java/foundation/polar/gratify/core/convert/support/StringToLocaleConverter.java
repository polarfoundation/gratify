package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * Converts from a String to a {@link java.util.Locale}.
 *
 * <p>Accepts the classic {@link Locale} String format ({@link Locale#toString()})
 * as well as BCP 47 language tags ({@link Locale#forLanguageTag} on Java 7+).
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @see StringUtils#parseLocale
 */
final class StringToLocaleConverter implements Converter<String, Locale> {
   @Override
   @Nullable
   public Locale convert(String source) {
      return StringUtils.parseLocale(source);
   }
}