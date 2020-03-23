package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;
import foundation.polar.gratify.artifacts.factory.config.ArtifactFactoryPostProcessor;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.support.DefaultListableArtifactFactory;
import foundation.polar.gratify.core.Ordered;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * A {@link foundation.polar.gratify.artifacts.factory.config.ArtifactFactoryPostProcessor}
 * implementation that allows for convenient registration of custom autowire
 * qualifier types.
 *
 * <pre class="code">
 * &lt;bean id="customAutowireConfigurer" class="foundation.polar.gratify.beans.factory.annotation.CustomAutowireConfigurer"&gt;
 *   &lt;property name="customQualifierTypes"&gt;
 *     &lt;set&gt;
 *       &lt;value&gt;mypackage.MyQualifier&lt;/value&gt;
 *     &lt;/set&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.annotation.Qualifier
 */
public class CustomAutowireConfigurer
   implements ArtifactFactoryPostProcessor, ArtifactClassLoaderAware, Ordered {
   private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

   @Nullable
   private Set<?> customQualifierTypes;

   @Nullable
   private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


   public void setOrder(int order) {
      this.order = order;
   }

   @Override
   public int getOrder() {
      return this.order;
   }

   @Override
   public void setArtifactClassLoader(@Nullable ClassLoader beanClassLoader) {
      this.beanClassLoader = beanClassLoader;
   }

   /**
    * Register custom qualifier annotation types to be considered
    * when autowiring beans. Each element of the provided set may
    * be either a Class instance or a String representation of the
    * fully-qualified class name of the custom annotation.
    * <p>Note that any annotation that is itself annotated with Gratify's
    * {@link foundation.polar.gratify.artifacts.factory.annotation.Qualifier}
    * does not require explicit registration.
    * @param customQualifierTypes the custom types to register
    */
   public void setCustomQualifierTypes(Set<?> customQualifierTypes) {
      this.customQualifierTypes = customQualifierTypes;
   }

   @Override
   @SuppressWarnings("unchecked")
   public void postProcessArtifactFactory(ConfigurableListableArtifactFactory beanFactory) throws ArtifactsException {
      if (this.customQualifierTypes != null) {
         if (!(beanFactory instanceof DefaultListableArtifactFactory)) {
            throw new IllegalStateException(
               "CustomAutowireConfigurer needs to operate on a DefaultListableArtifactFactory");
         }
         DefaultListableArtifactFactory dlbf = (DefaultListableArtifactFactory) beanFactory;
         if (!(dlbf.getAutowireCandidateResolver() instanceof QualifierAnnotationAutowireCandidateResolver)) {
            dlbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
         }
         QualifierAnnotationAutowireCandidateResolver resolver =
            (QualifierAnnotationAutowireCandidateResolver) dlbf.getAutowireCandidateResolver();
         for (Object value : this.customQualifierTypes) {
            Class<? extends Annotation> customType = null;
            if (value instanceof Class) {
               customType = (Class<? extends Annotation>) value;
            }
            else if (value instanceof String) {
               String className = (String) value;
               customType = (Class<? extends Annotation>) ClassUtils.resolveClassName(className, this.beanClassLoader);
            }
            else {
               throw new IllegalArgumentException(
                  "Invalid value [" + value + "] for custom qualifier type: needs to be Class or String.");
            }
            if (!Annotation.class.isAssignableFrom(customType)) {
               throw new IllegalArgumentException(
                  "Qualifier type [" + customType.getName() + "] needs to be annotation type");
            }
            resolver.addQualifierType(customType);
         }
      }
   }
}
