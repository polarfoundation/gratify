package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.artifacts.propertyeditors.PropertiesEditor;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

/**
 * {@link java.beans.PropertyEditor Editor} for a {@link PropertyValues} object.
 *
 * <p>The required format is defined in the {@link java.util.Properties}
 * documentation. Each property must be on a new line.
 *
 * <p>The present implementation relies on a
 * {@link foundation.polar.gratify.artifacts.propertyeditors.PropertiesEditor}
 * underneath.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PropertyValuesEditor extends PropertyEditorSupport {

   private final PropertiesEditor propertiesEditor = new PropertiesEditor();

   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      this.propertiesEditor.setAsText(text);
      Properties props = (Properties) this.propertiesEditor.getValue();
      setValue(new MutablePropertyValues(props));
   }

}