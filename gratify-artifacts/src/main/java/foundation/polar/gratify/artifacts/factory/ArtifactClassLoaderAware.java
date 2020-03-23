package foundation.polar.gratify.artifacts.factory;


/**
 * Callback that allows a bean to be aware of the bean
 * {@link ClassLoader class loader}; that is, the class loader used by the
 * present bean factory to load bean classes.
 *
 * <p>This is mainly intended to be implemented by framework classes which
 * have to pick up application classes by name despite themselves potentially
 * being loaded from a shared class loader.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link ArtifactFactory ArtifactFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see ArtifactNameAware
 * @see ArtifactFactoryAware
 * @see InitializingArtifact
 */
public interface ArtifactClassLoaderAware extends Aware {
   /**
    * Callback that supplies the bean {@link ClassLoader class loader} to
    * a bean instance.
    * <p>Invoked <i>after</i> the population of normal bean properties but
    * <i>before</i> an initialization callback such as
    * {@link InitializingArtifact InitializingArtifact's}
    * {@link InitializingArtifact#afterPropertiesSet()}
    * method or a custom init-method.
    * @param classLoader the owning class loader
    */
   void setArtifactClassLoader(ClassLoader classLoader);
}
