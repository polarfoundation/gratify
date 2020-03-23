package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.PropertyValues;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyDescriptor;

/**
 * Subinterface of {@link ArtifactPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target artifacts,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link ArtifactPostProcessor} interface as far as possible, or to derive from
 * {@link InstantiationAwareArtifactPostProcessorAdapter} in order to be shielded
 * from extensions to this interface.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see foundation.polar.gratify.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see foundation.polar.gratify.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
public interface InstantiationAwareArtifactPostProcessor extends ArtifactPostProcessor {
   /**
    * Apply this ArtifactPostProcessor <i>before the target artifact gets instantiated</i>.
    * The returned artifact object may be a proxy to use instead of the target artifact,
    * effectively suppressing default instantiation of the target artifact.
    * <p>If a non-null object is returned by this method, the artifact creation process
    * will be short-circuited. The only further processing applied is the
    * {@link #postProcessAfterInitialization} callback from the configured
    * {@link ArtifactPostProcessor ArtifactPostProcessors}.
    * <p>This callback will be applied to artifact definitions with their artifact class,
    * as well as to factory-method definitions in which case the returned artifact type
    * will be passed in here.
    * <p>Post-processors may implement the extended
    * {@link SmartInstantiationAwareArtifactPostProcessor} interface in order
    * to predict the type of the artifact object that they are going to return here.
    * <p>The default implementation returns {@code null}.
    * @param artifactClass the class of the artifact to be instantiated
    * @param artifactName the name of the artifact
    * @return the artifact object to expose instead of a default instance of the target artifact,
    * or {@code null} to proceed with default instantiation
    * @throws foundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see #postProcessAfterInstantiation
    * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#getArtifactClass()
    * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#getFactoryMethodName()
    */
   @Nullable
   default Object postProcessBeforeInstantiation(Class<?> artifactClass, String artifactName) throws ArtifactsException {
      return null;
   }

   /**
    * Perform operations after the artifact has been instantiated, via a constructor or factory method,
    * but before Gratify property population (from explicit properties or autowiring) occurs.
    * <p>This is the ideal callback for performing custom field injection on the given artifact
    * instance, right before Gratify's autowiring kicks in.
    * <p>The default implementation returns {@code true}.
    * @param artifact the artifact instance created, with properties not having been set yet
    * @param artifactName the name of the artifact
    * @return {@code true} if properties should be set on the artifact; {@code false}
    * if property population should be skipped. Normal implementations should return {@code true}.
    * Returning {@code false} will also prevent any subsequent InstantiationAwareArtifactPostProcessor
    * instances being invoked on this artifact instance.
    * @throws foundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see #postProcessBeforeInstantiation
    */
   default boolean postProcessAfterInstantiation(Object artifact, String artifactName) throws ArtifactsException {
      return true;
   }

   /**
    * Post-process the given property values before the factory applies them
    * to the given artifact, without any need for property descriptors.
    * <p>Implementations should return {@code null} (the default) if they provide a custom
    * {@link #postProcessPropertyValues} implementation, and {@code pvs} otherwise.
    * In a future version of this interface (with {@link #postProcessPropertyValues} removed),
    * the default implementation will return the given {@code pvs} as-is directly.
    * @param pvs the property values that the factory is about to apply (never {@code null})
    * @param artifact the artifact instance created, but whose properties have not yet been set
    * @param artifactName the name of the artifact
    * @return the actual property values to apply to the given artifact (can be the passed-in
    * PropertyValues instance), or {@code null} which proceeds with the existing properties
    * but specifically continues with a call to {@link #postProcessPropertyValues}
    * (requiring initialized {@code PropertyDescriptor}s for the current artifact class)
    * @throws foundation.polar.gratify.artifacts.ArtifactsException in case of errors
    *
    * @see #postProcessPropertyValues
    */
   @Nullable
   default PropertyValues postProcessProperties(PropertyValues pvs, Object artifact, String artifactName)
      throws ArtifactsException {
      return null;
   }

   /**
    * Post-process the given property values before the factory applies them
    * to the given bean. Allows for checking whether all dependencies have been
    * satisfied, for example based on a "Required" annotation on bean property setters.
    * <p>Also allows for replacing the property values to apply, typically through
    * creating a new MutablePropertyValues instance based on the original PropertyValues,
    * adding or removing specific values.
    * <p>The default implementation returns the given {@code pvs} as-is.
    * @param pvs the property values that the factory is about to apply (never {@code null})
    * @param pds the relevant property descriptors for the target bean (with ignored
    * dependency types - which the factory handles specifically - already filtered out)
    * @param bean the bean instance created, but whose properties have not yet been set
    * @param beanName the name of the bean
    * @return the actual property values to apply to the given bean (can be the passed-in
    * PropertyValues instance), or {@code null} to skip property population
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see #postProcessProperties
    * @see foundation.polar.gratify.artifacts.MutablePropertyValues
    * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
    */
   @Deprecated
   @Nullable
   default PropertyValues postProcessPropertyValues(
      PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws ArtifactsException {

      return pvs;
   }
}
