package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.PropertyEditorRegistrar;
import foundation.polar.gratify.core.Ordered;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditor;
import java.util.Map;

/**
 * {@link ArtifactFactoryPostProcessor} implementation that allows for convenient
 * registration of custom {@link PropertyEditor property editors}.
 *
 * <p>In case you want to register {@link PropertyEditor} instances,
 * the recommended usage as of Gratify 2.0 is to use custom
 * {@link PropertyEditorRegistrar} implementations that in turn register any
 * desired editor instances on a given
 * {@link foundation.polar.gratify.artifacts.PropertyEditorRegistry registry}. Each
 * PropertyEditorRegistrar can register any number of custom editors.
 *
 * <pre class="code">
 * &lt;artifact id="customEditorConfigurer" class="foundation.polar.gratify.artifacts.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="propertyEditorRegistrars"&gt;
 *     &lt;list&gt;
 *       &lt;artifact class="mypackage.MyCustomDateEditorRegistrar"/&gt;
 *       &lt;artifact class="mypackage.MyObjectEditorRegistrar"/&gt;
 *     &lt;/list&gt;
 *   &lt;/property&gt;
 * &lt;/artifact&gt;
 * </pre>
 *
 * <p>
 * It's perfectly fine to register {@link PropertyEditor} <em>classes</em> via
 * the {@code customEditors} property. Gratify will create fresh instances of
 * them for each editing attempt then:
 *
 * <pre class="code">
 * &lt;artifact id="customEditorConfigurer" class="foundation.polar.gratify.artifacts.factory.config.CustomEditorConfigurer"&gt;
 *   &lt;property name="customEditors"&gt;
 *     &lt;map&gt;
 *       &lt;entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/&gt;
 *       &lt;entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/&gt;
 *     &lt;/map&gt;
 *   &lt;/property&gt;
 * &lt;/artifact&gt;
 * </pre>
 *
 * <p>
 * Note, that you shouldn't register {@link PropertyEditor} artifact instances via
 * the {@code customEditors} property as {@link PropertyEditor PropertyEditors} are stateful
 * and the instances will then have to be synchronized for every editing
 * attempt. In case you need control over the instantiation process of
 * {@link PropertyEditor PropertyEditors}, use a {@link PropertyEditorRegistrar} to register
 * them.
 *
 * <p>
 * Also supports "java.lang.String[]"-style array class names and primitive
 * class names (e.g. "boolean"). Delegates to {@link ClassUtils} for actual
 * class name resolution.
 *
 * <p><b>NOTE:</b> Custom property editors registered with this configurer do
 * <i>not</i> apply to data binding. Custom editors for data binding need to
 * be registered on the {@link foundation.polar.gratify.validation.DataBinder}:
 * Use a common base class or delegate to common PropertyEditorRegistrar
 * implementations to reuse editor registration there.
 *
 * @author Juergen Hoeller
 *
 * @see java.artifacts.PropertyEditor
 * @see foundation.polar.gratify.artifacts.PropertyEditorRegistrar
 * @see ConfigurableArtifactFactory#addPropertyEditorRegistrar
 * @see ConfigurableArtifactFactory#registerCustomEditor
 * @see foundation.polar.gratify.validation.DataBinder#registerCustomEditor
 */
public class CustomEditorConfigurer implements ArtifactFactoryPostProcessor, Ordered {
   protected final Log logger = LogFactory.getLog(getClass());

   private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

   @Nullable
   private PropertyEditorRegistrar[] propertyEditorRegistrars;

   @Nullable
   private Map<Class<?>, Class<? extends PropertyEditor>> customEditors;


   public void setOrder(int order) {
      this.order = order;
   }

   @Override
   public int getOrder() {
      return this.order;
   }

   /**
    * Specify the {@link PropertyEditorRegistrar PropertyEditorRegistrars}
    * to apply to artifacts defined within the current application context.
    * <p>This allows for sharing {@code PropertyEditorRegistrars} with
    * {@link foundation.polar.gratify.validation.DataBinder DataBinders}, etc.
    * Furthermore, it avoids the need for synchronization on custom editors:
    * A {@code PropertyEditorRegistrar} will always create fresh editor
    * instances for each artifact creation attempt.
    * @see ConfigurableListableArtifactFactory#addPropertyEditorRegistrar
    */
   public void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
      this.propertyEditorRegistrars = propertyEditorRegistrars;
   }

   /**
    * Specify the custom editors to register via a {@link Map}, using the
    * class name of the required type as the key and the class name of the
    * associated {@link PropertyEditor} as value.
    * @see ConfigurableListableArtifactFactory#registerCustomEditor
    */
   public void setCustomEditors(Map<Class<?>, Class<? extends PropertyEditor>> customEditors) {
      this.customEditors = customEditors;
   }

   @Override
   public void postProcessArtifactFactory(ConfigurableListableArtifactFactory artifactFactory) throws ArtifactsException {
      if (this.propertyEditorRegistrars != null) {
         for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
            artifactFactory.addPropertyEditorRegistrar(propertyEditorRegistrar);
         }
      }
      if (this.customEditors != null) {
         this.customEditors.forEach(artifactFactory::registerCustomEditor);
      }
   }
}
