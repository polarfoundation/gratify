package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.artifacts.factory.wiring.ArtifactWiringInfo;
import foundation.polar.gratify.artifacts.factory.wiring.ArtifactWiringInfoResolver;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link foundation.polar.gratify.artifacts.factory.wiring.ArtifactWiringInfoResolver} that
 * uses the Configurable annotation to identify which classes need autowiring.
 * The bean name to look up will be taken from the {@link Configurable} annotation
 * if specified; otherwise the default will be the fully-qualified name of the
 * class being configured.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see Configurable
 * @see foundation.polar.gratify.artifacts.factory.wiring.ClassNameArtifactWiringInfoResolver
 */
public class AnnotationArtifactWiringInfoResolver implements ArtifactWiringInfoResolver {

   @Override
   @Nullable
   public ArtifactWiringInfo resolveWiringInfo(Object beanInstance) {
      AssertUtils.notNull(beanInstance, "Artifact instance must not be null");
      Configurable annotation = beanInstance.getClass().getAnnotation(Configurable.class);
      return (annotation != null ? buildWiringInfo(beanInstance, annotation) : null);
   }

   /**
    * Build the {@link ArtifactWiringInfo} for the given {@link Configurable} annotation.
    * @param beanInstance the bean instance
    * @param annotation the Configurable annotation found on the bean class
    * @return the resolved ArtifactWiringInfo
    */
   protected ArtifactWiringInfo buildWiringInfo(Object beanInstance, Configurable annotation) {
      if (!Autowire.NO.equals(annotation.autowire())) {
         // Autowiring by name or by type
         return new ArtifactWiringInfo(annotation.autowire().value(), annotation.dependencyCheck());
      }
      else if (!annotation.value().isEmpty()) {
         // Explicitly specified bean name for bean definition to take property values from
         return new ArtifactWiringInfo(annotation.value(), false);
      }
      else {
         // Default bean name for bean definition to take property values from
         return new ArtifactWiringInfo(getDefaultArtifactName(beanInstance), true);
      }
   }

   /**
    * Determine the default bean name for the specified bean instance.
    * <p>The default implementation returns the superclass name for a CGLIB
    * proxy and the name of the plain bean class else.
    * @param beanInstance the bean instance to build a default name for
    * @return the default bean name to use
    * @see foundation.polar.gratify.util.ClassUtils#getUserClass(Class)
    */
   protected String getDefaultArtifactName(Object beanInstance) {
      return ClassUtils.getUserClass(beanInstance).getName();
   }
}
