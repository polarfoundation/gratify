package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.PropertyValue;
import foundation.polar.gratify.artifacts.factory.ArtifactInitializationException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Property resource configurer that overrides bean property values in an application
 * context definition. It <i>pushes</i> values from a properties file into bean definitions.
 *
 * <p>Configuration lines are expected to be of the following form:
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * Example properties file:
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * In contrast to PropertyPlaceholderConfigurer, the original definition can have default
 * values or no values at all for such bean properties. If an overriding properties file does
 * not have an entry for a certain bean property, the default context definition is used.
 *
 * <p>Note that the context definition <i>is not</i> aware of being overridden;
 * so this is not immediately obvious when looking at the XML definition file.
 * Furthermore, note that specified override values are always <i>literal</i> values;
 * they are not translated into bean references. This also applies when the original
 * value in the XML bean definition specifies a bean reference.
 *
 * <p>In case of multiple PropertyOverrideConfigurers that define different values for
 * the same bean property, the <i>last</i> one will win (due to the overriding mechanism).
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@code convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 *
 * @see #convertPropertyValue
 * @see PropertyPlaceholderConfigurer
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {
   /**
    * The default bean name separator.
    */
   public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";


   private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

   private boolean ignoreInvalidKeys = false;

   /**
    * Contains names of beans that have overrides.
    */
   private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

   /**
    * Set the separator to expect between bean name and property path.
    * Default is a dot (".").
    */
   public void setArtifactNameSeparator(String beanNameSeparator) {
      this.beanNameSeparator = beanNameSeparator;
   }

   /**
    * Set whether to ignore invalid keys. Default is "false".
    * <p>If you ignore invalid keys, keys that do not follow the 'beanName.property' format
    * (or refer to invalid bean names or properties) will just be logged at debug level.
    * This allows one to have arbitrary other keys in a properties file.
    */
   public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
      this.ignoreInvalidKeys = ignoreInvalidKeys;
   }

   @Override
   protected void processProperties(ConfigurableListableArtifactFactory beanFactory, Properties props)
      throws ArtifactsException {

      for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements();) {
         String key = (String) names.nextElement();
         try {
            processKey(beanFactory, key, props.getProperty(key));
         }
         catch (ArtifactsException ex) {
            String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
            if (!this.ignoreInvalidKeys) {
               throw new ArtifactInitializationException(msg, ex);
            }
            if (logger.isDebugEnabled()) {
               logger.debug(msg, ex);
            }
         }
      }
   }

   /**
    * Process the given key as 'beanName.property' entry.
    */
   protected void processKey(ConfigurableListableArtifactFactory factory, String key, String value)
      throws ArtifactsException {

      int separatorIndex = key.indexOf(this.beanNameSeparator);
      if (separatorIndex == -1) {
         throw new ArtifactInitializationException("Invalid key '" + key +
            "': expected 'beanName" + this.beanNameSeparator + "property'");
      }
      String beanName = key.substring(0, separatorIndex);
      String beanProperty = key.substring(separatorIndex + 1);
      this.beanNames.add(beanName);
      applyPropertyValue(factory, beanName, beanProperty, value);
      if (logger.isDebugEnabled()) {
         logger.debug("Property '" + key + "' set to value [" + value + "]");
      }
   }

   /**
    * Apply the given property value to the corresponding bean.
    */
   protected void applyPropertyValue(
      ConfigurableListableArtifactFactory factory, String beanName, String property, String value) {

      ArtifactDefinition bd = factory.getArtifactDefinition(beanName);
      ArtifactDefinition bdToUse = bd;
      while (bd != null) {
         bdToUse = bd;
         bd = bd.getOriginatingArtifactDefinition();
      }
      PropertyValue pv = new PropertyValue(property, value);
      pv.setOptional(this.ignoreInvalidKeys);
      bdToUse.getPropertyValues().addPropertyValue(pv);
   }

   /**
    * Were there overrides for this bean?
    * Only valid after processing has occurred at least once.
    * @param beanName name of the bean to query status for
    * @return whether there were property overrides for the named bean
    */
   public boolean hasPropertyOverridesFor(String beanName) {
      return this.beanNames.contains(beanName);
   }
}
