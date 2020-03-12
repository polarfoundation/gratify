package foundation.polar.gratify.artifacts.propertyeditors;


import foundation.polar.gratify.utils.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * Editor for {@code java.util.Locale}, to directly populate a Locale property.
 *
 * <p>Expects the same syntax as Locale's {@code toString}, i.e. language +
 * optionally country + optionally variant, separated by "_" (e.g. "en", "en_US").
 * Also accepts spaces as separators, as alternative to underscores.
 *
 * @author Juergen Hoeller
 * @since 26.05.2003
 * @see java.util.Locale
 * @see foundation.polar.gratify.utils.StringUtils#parseLocaleString
 */
public class LocaleEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(String text) {
      setValue(StringUtils.parseLocaleString(text));
   }

   @Override
   public String getAsText() {
      Object value = getValue();
      return (value != null ? value.toString() : "");
   }

}
