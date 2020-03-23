package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Factory hook that allows for custom modification of new bean instances &mdash;
 * for example, checking for marker interfaces or wrapping beans with proxies.
 *
 * <p>Typically, post-processors that populate beans via marker interfaces
 * or the like will implement {@link #postProcessBeforeInitialization},
 * while post-processors that wrap beans with proxies will normally
 * implement {@link #postProcessAfterInitialization}.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} can autodetect {@code ArtifactPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans subsequently
 * created. A plain {@code ArtifactFactory} allows for programmatic registration of
 * post-processors, applying them to all beans created through the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>{@code ArtifactPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link foundation.polar.gratify.core.PriorityOrdered} and
 * {@link foundation.polar.gratify.core.Ordered} semantics. In contrast,
 * {@code ArtifactPostProcessor} beans that are registered programmatically with a
 * {@code ArtifactFactory} will be applied in the order of registration; any ordering
 * semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link foundation.polar.gratify.core.annotation.Order @Order} annotation is not
 * taken into account for {@code ArtifactPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see InstantiationAwareArtifactPostProcessor
 * @see DestructionAwareArtifactPostProcessor
 * @see ConfigurableArtifactFactory#addArtifactPostProcessor
 * @see ArtifactFactoryPostProcessor
 */
public interface ArtifactPostProcessor {
   /**
    * Apply this {@code ArtifactPostProcessor} to the given new bean instance <i>before</i> any bean
    * initialization callbacks (like InitializingArtifact's {@code afterPropertiesSet}
    * or a custom init-method). The bean will already be populated with property values.
    * The returned bean instance may be a wrapper around the original.
    * <p>The default implementation returns the given {@code bean} as-is.
    * @param bean the new bean instance
    * @param artifactName the name of the bean
    * @return the bean instance to use, either the original or a wrapped one;
    * if {@code null}, no subsequent ArtifactPostProcessors will be invoked
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see foundation.polar.gratify.artifacts.factory.InitializingArtifact#afterPropertiesSet
    */
   @Nullable
   default Object postProcessBeforeInitialization(Object artifact, String artifactName) throws ArtifactsException {
      return artifact;
   }

   /**
    * Apply this {@code ArtifactPostProcessor} to the given new bean instance <i>after</i> any bean
    * initialization callbacks (like InitializingArtifact's {@code afterPropertiesSet}
    * or a custom init-method). The bean will already be populated with property values.
    * The returned bean instance may be a wrapper around the original.
    * <p>In case of a FactoryArtifact, this callback will be invoked for both the FactoryArtifact
    * instance and the objects created by the FactoryArtifact (as of Gratify 2.0). The
    * post-processor can decide whether to apply to either the FactoryArtifact or created
    * objects or both through corresponding {@code bean instanceof FactoryArtifact} checks.
    * <p>This callback will also be invoked after a short-circuiting triggered by a
    * {@link InstantiationAwareArtifactPostProcessor#postProcessBeforeInstantiation} method,
    * in contrast to all other {@code ArtifactPostProcessor} callbacks.
    * <p>The default implementation returns the given {@code bean} as-is.
    * @param bean the new bean instance
    * @param artifactName the name of the bean
    * @return the bean instance to use, either the original or a wrapped one;
    * if {@code null}, no subsequent ArtifactPostProcessors will be invoked
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see foundation.polar.gratify.artifacts.factory.InitializingArtifact#afterPropertiesSet
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact
    */
   @Nullable
   default Object postProcessAfterInitialization(Object artifact, String artifactName) throws ArtifactsException {
      return artifact;
   }
}
