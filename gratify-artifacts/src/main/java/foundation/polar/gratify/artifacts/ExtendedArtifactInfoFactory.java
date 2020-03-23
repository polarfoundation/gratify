package foundation.polar.gratify.artifacts;

import foundation.polar.gratify.core.Ordered;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;

/**
 * {@link ArtifactInfoFactory} implementation that evaluates whether bean classes have
 * "non-standard" JavaBeans setter methods and are thus candidates for introspection
 * by Gratify's (package-visible) {@code ExtendedArtifactInfo} implementation.
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link ArtifactInfoFactory} types to take precedence.
 *
 * @author Chris Beams
 * @see ArtifactInfoFactory
 * @see CachedIntrospectionResults
 */
public class ExtendedArtifactInfoFactory implements ArtifactInfoFactory, Ordered {

   /**
    * Return an {@link ExtendedArtifactInfo} for the given bean class, if applicable.
    */
   @Override
   @Nullable
   public BeanInfo getArtifactInfo(Class<?> beanClass) throws IntrospectionException {
      return (supports(beanClass) ? new ExtendedArtifactInfo(Introspector.getBeanInfo(beanClass)) : null);
   }

   /**
    * Return whether the given bean class declares or inherits any non-void
    * returning bean property or indexed property setter methods.
    */
   private boolean supports(Class<?> beanClass) {
      for (Method method : beanClass.getMethods()) {
         if (ExtendedArtifactInfo.isCandidateWriteMethod(method)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
   }

}
