package foundation.polar.gratify.artifacts.propertyeditors;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;

/**
 * Editor for char arrays. Strings will simply be converted to
 * their corresponding char representations.
 *
 * @author Juergen Hoeller
 * @see String#toCharArray()
 */
public class CharArrayPropertyEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(@Nullable String text) {
      setValue(text != null ? text.toCharArray() : null);
   }

   @Override
   public String getAsText() {
      char[] value = (char[]) getValue();
      return (value != null ? new String(value) : "");
   }

}
