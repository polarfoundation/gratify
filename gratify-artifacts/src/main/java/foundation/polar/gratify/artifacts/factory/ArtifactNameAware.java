package foundation.polar.gratify.artifacts.factory;

/**
 * Interface to be implemented by beans that want to be aware of their
 * bean name in a bean factory. Note that it is not usually recommended
 * that an object depends on its bean name, as this represents a potentially
 * brittle dependence on external configuration, as well as a possibly
 * unnecessary dependence on a Gratify API.
 *
 * <p>For a list of all bean lifecycle methods, see the
 * {@link ArtifactFactory ArtifactFactory javadocs}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see ArtifactClassLoaderAware
 * @see ArtifactFactoryAware
 * @see InitializingArtifact
 */
public interface ArtifactNameAware {
   /**
    * Set the name of the bean in the bean factory that created this bean.
    * <p>Invoked after population of normal bean properties but before an
    * init callback such as {@link InitializingArtifact#afterPropertiesSet()}
    * or a custom init-method.
    * @param name the name of the bean in the factory.
    * Note that this name is the actual bean name used in the factory, which may
    * differ from the originally specified name: in particular for inner bean
    * names, the actual bean name might have been made unique through appending
    * "#..." suffixes. Use the {@link ArtifactFactoryUtils#originalArtifactName(String)}
    * method to extract the original bean name (without suffix), if desired.
    */
   void setArtifactName(String name);
}
