package foundation.polar.gratify.artifacts.propertyeditors;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;

/**
 * Editor for byte arrays. Strings will simply be converted to
 * their corresponding byte representations.
 *
 * @author Juergen Hoeller
 * @see java.lang.String#getBytes
 */
public class ByteArrayPropertyEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(@Nullable String text) {
      setValue(text != null ? text.getBytes() : null);
   }

   @Override
   public String getAsText() {
      byte[] value = (byte[]) getValue();
      return (value != null ? new String(value) : "");
   }

}
