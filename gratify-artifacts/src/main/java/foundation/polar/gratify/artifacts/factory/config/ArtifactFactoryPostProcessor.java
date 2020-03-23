package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;

/**
 * Factory hook that allows for custom modification of an application context's
 * bean definitions, adapting the bean property values of the context's underlying
 * bean factory.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context. See
 * {@link PropertyResourceConfigurer} and its concrete implementations for
 * out-of-the-box solutions that address such configuration needs.
 *
 * <p>A {@code ArtifactFactoryPostProcessor} may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link ArtifactPostProcessor} instead.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} auto-detects {@code ArtifactFactoryPostProcessor}
 * beans in its bean definitions and applies them before any other beans get created.
 * A {@code ArtifactFactoryPostProcessor} may also be registered programmatically
 * with a {@code ConfigurableApplicationContext}.
 *
 * <h3>Ordering</h3>
 * <p>{@code ArtifactFactoryPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link foundation.polar.gratify.core.PriorityOrdered} and
 * {@link foundation.polar.gratify.core.Ordered} semantics. In contrast,
 * {@code ArtifactFactoryPostProcessor} beans that are registered programmatically
 * with a {@code ConfigurableApplicationContext} will be applied in the order of
 * registration; any ordering semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link foundation.polar.gratify.core.annotation.Order @Order} annotation is not
 * taken into account for {@code ArtifactFactoryPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ArtifactPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface ArtifactFactoryPostProcessor {
   /**
    * Modify the application context's internal bean factory after its standard
    * initialization. All bean definitions will have been loaded, but no beans
    * will have been instantiated yet. This allows for overriding or adding
    * properties even to eager-initializing beans.
    * @param beanFactory the bean factory used by the application context
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException in case of errors
    */
   void postProcessArtifactFactory(ConfigurableListableArtifactFactory artifactFactory) throws ArtifactsException;
}
