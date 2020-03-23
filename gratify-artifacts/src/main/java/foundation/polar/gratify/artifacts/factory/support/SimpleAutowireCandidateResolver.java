package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.DependencyDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link AutowireCandidateResolver} implementation to use when no annotation
 * support is available. This implementation checks the bean definition only.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

   @Override
   public boolean isAutowireCandidate(ArtifactDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      return bdHolder.getArtifactDefinition().isAutowireCandidate();
   }

   @Override
   public boolean isRequired(DependencyDescriptor descriptor) {
      return descriptor.isRequired();
   }

   @Override
   @Nullable
   public Object getSuggestedValue(DependencyDescriptor descriptor) {
      return null;
   }

   @Override
   @Nullable
   public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
      return null;
   }

}
