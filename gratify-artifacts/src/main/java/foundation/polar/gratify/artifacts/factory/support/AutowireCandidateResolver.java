package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.DependencyDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Strategy interface for determining whether a specific bean definition
 * qualifies as an autowire candidate for a specific dependency.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
public interface AutowireCandidateResolver {

   /**
    * Determine whether the given bean definition qualifies as an
    * autowire candidate for the given dependency.
    * <p>The default implementation checks
    * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition#isAutowireCandidate()}.
    * @param bdHolder the bean definition including bean name and aliases
    * @param descriptor the descriptor for the target method parameter or field
    * @return whether the bean definition qualifies as autowire candidate
    * @see foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition#isAutowireCandidate()
    */
   default boolean isAutowireCandidate(ArtifactDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      return bdHolder.getArtifactDefinition().isAutowireCandidate();
   }

   /**
    * Determine whether the given descriptor is effectively required.
    * <p>The default implementation checks {@link DependencyDescriptor#isRequired()}.
    * @param descriptor the descriptor for the target method parameter or field
    * @return whether the descriptor is marked as required or possibly indicating
    * non-required status some other way (e.g. through a parameter annotation)
    *
    * @see DependencyDescriptor#isRequired()
    */
   default boolean isRequired(DependencyDescriptor descriptor) {
      return descriptor.isRequired();
   }

   /**
    * Determine whether the given descriptor declares a qualifier beyond the type
    * (typically - but not necessarily - a specific kind of annotation).
    * <p>The default implementation returns {@code false}.
    * @param descriptor the descriptor for the target method parameter or field
    * @return whether the descriptor declares a qualifier, narrowing the candidate
    * status beyond the type match
    *
    * @see foundation.polar.gratify.artifacts.factory.annotation.QualifierAnnotationAutowireCandidateResolver#hasQualifier
    */
   default boolean hasQualifier(DependencyDescriptor descriptor) {
      return false;
   }

   /**
    * Determine whether a default value is suggested for the given dependency.
    * <p>The default implementation simply returns {@code null}.
    * @param descriptor the descriptor for the target method parameter or field
    * @return the value suggested (typically an expression String),
    * or {@code null} if none found
    */
   @Nullable
   default Object getSuggestedValue(DependencyDescriptor descriptor) {
      return null;
   }

   /**
    * Build a proxy for lazy resolution of the actual dependency target,
    * if demanded by the injection point.
    * <p>The default implementation simply returns {@code null}.
    * @param descriptor the descriptor for the target method parameter or field
    * @param beanName the name of the bean that contains the injection point
    * @return the lazy resolution proxy for the actual dependency target,
    * or {@code null} if straight resolution is to be performed
    */
   @Nullable
   default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
      return null;
   }

}
