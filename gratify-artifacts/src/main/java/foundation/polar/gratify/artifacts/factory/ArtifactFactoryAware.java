package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.ArtifactsException;

/**
 * Interface to be implemented by beans that wish to be aware of their
 * owning {@link ArtifactFactory}.
 *
 * <p>For example, beans can look up collaborating beans via the factory
 * (Dependency Lookup). Note that most beans will choose to receive references
 * to collaborating beans via corresponding bean properties or constructor
 * arguments (Dependency Injection).
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link ArtifactFactory ArtifactFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @see ArtifactNameAware
 * @see ArtifactClassLoaderAware
 * @see InitializingArtifact
 * @see foundation.polar.gratify.context.ApplicationContextAware
 */
public interface ArtifactFactoryAware extends Aware {
   /**
    * Callback that supplies the owning factory to a bean instance.
    * <p>Invoked after the population of normal bean properties
    * but before an initialization callback such as
    * {@link InitializingArtifact#afterPropertiesSet()} or a custom init-method.
    * @param artifactFactory owning ArtifactFactory (never {@code null}).
    * The bean can immediately call methods on the factory.
    * @throws ArtifactsException in case of initialization errors
    * @see ArtifactInitializationException
    */
   void setArtifactFactory(ArtifactFactory artifactFactory) throws ArtifactsException;
}
