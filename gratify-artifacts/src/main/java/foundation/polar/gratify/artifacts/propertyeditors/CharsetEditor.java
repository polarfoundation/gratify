package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.utils.StringUtils;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

/**
 * Editor for {@code java.nio.charset.Charset}, translating charset
 * String representations into Charset objects and back.
 *
 * <p>Expects the same syntax as Charset's {@link java.nio.charset.Charset#name()},
 * e.g. {@code UTF-8}, {@code ISO-8859-16}, etc.
 *
 * @author Arjen Poutsma
 * @see Charset
 */
public class CharsetEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      if (StringUtils.hasText(text)) {
         setValue(Charset.forName(text));
      }
      else {
         setValue(null);
      }
   }

   @Override
   public String getAsText() {
      Charset value = (Charset) getValue();
      return (value != null ? value.name() : "");
   }
}
