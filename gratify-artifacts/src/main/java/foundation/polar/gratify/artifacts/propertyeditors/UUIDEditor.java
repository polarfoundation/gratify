package foundation.polar.gratify.artifacts.propertyeditors;


import foundation.polar.gratify.utils.StringUtils;
import java.beans.PropertyEditorSupport;
import java.util.UUID;

/**
 * Editor for {@code java.util.UUID}, translating UUID
 * String representations into UUID objects and back.
 *
 * @author Juergen Hoeller
 *
 * @see java.util.UUID
 */
public class UUIDEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      if (StringUtils.hasText(text)) {
         setValue(UUID.fromString(text.trim()));
      }
      else {
         setValue(null);
      }
   }

   @Override
   public String getAsText() {
      UUID value = (UUID) getValue();
      return (value != null ? value.toString() : "");
   }

}