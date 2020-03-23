package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * {@link java.beans.PropertyEditor} implementation for standard JDK
 * {@link java.util.ResourceBundle ResourceBundles}.
 *
 * <p>Only supports conversion <i>from</i> a String, but not <i>to</i> a String.
 *
 * Find below some examples of using this class in a (properly configured)
 * Gratify container using XML-based metadata:
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'messages' property is of type java.util.ResourceBundle.
 *        the 'DialogMessages.properties' file exists at the root of the CLASSPATH
 *    --&gt;
 *    &lt;property name="messages" value="DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'DialogMessages.properties' file exists in the 'com/messages' package
 *    --&gt;
 *    &lt;property name="messages" value="com/messages/DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>A 'properly configured' Gratify {@link foundation.polar.gratify.di.ApplicationContext container}
 * might contain a {@link foundation.polar.gratify.artifacts.factory.config.CustomEditorConfigurer}
 * definition such that the conversion can be effected transparently:
 *
 * <pre class="code"> &lt;bean class="foundation.polar.gratify.beans.factory.config.CustomEditorConfigurer"&gt;
 *    &lt;property name="customEditors"&gt;
 *        &lt;map&gt;
 *            &lt;entry key="java.util.ResourceBundle"&gt;
 *                &lt;bean class="foundation.polar.gratify.beans.propertyeditors.ResourceBundleEditor"/&gt;
 *            &lt;/entry&gt;
 *        &lt;/map&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>Please note that this {@link java.beans.PropertyEditor} is <b>not</b>
 * registered by default with any of the Gratify infrastructure.
 *
 * <p>Thanks to David Leal Valmana for the suggestion and initial prototype.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ResourceBundleEditor extends PropertyEditorSupport {

   /**
    * The separator used to distinguish between the base name and the locale
    * (if any) when {@link #setAsText(String) converting from a String}.
    */
   public static final String BASE_NAME_SEPARATOR = "_";


   @Override
   public void setAsText(String text) throws IllegalArgumentException {
      AssertUtils.hasText(text, "'text' must not be empty");
      String name = text.trim();

      int separator = name.indexOf(BASE_NAME_SEPARATOR);
      if (separator == -1) {
         setValue(ResourceBundle.getBundle(name));
      }
      else {
         // The name potentially contains locale information
         String baseName = name.substring(0, separator);
         if (!StringUtils.hasText(baseName)) {
            throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
         }
         String localeString = name.substring(separator + 1);
         Locale locale = StringUtils.parseLocaleString(localeString);
         setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
      }
   }

}
