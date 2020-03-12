package foundation.polar.gratify.artifacts.propertyeditors;


import java.beans.PropertyEditorSupport;
import java.util.Currency;

/**
 * Editor for {@code java.util.Currency}, translating currency codes into Currency
 * objects. Exposes the currency code as text representation of a Currency object.
 *
 * @author Juergen Hoeller
 * @see java.util.Currency
 */
public class CurrencyEditor extends PropertyEditorSupport {

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      setValue(Currency.getInstance(text));
   }

   @Override
   public String getAsText() {
      Currency value = (Currency) getValue();
      return (value != null ? value.getCurrencyCode() : "");
   }

}
