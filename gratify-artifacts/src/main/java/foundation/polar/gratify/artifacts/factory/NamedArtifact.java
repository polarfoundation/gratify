package foundation.polar.gratify.artifacts.factory;


/**
 * Counterpart of {@link ArtifactNameAware}. Returns the bean name of an object.
 *
 * <p>This interface can be introduced to avoid a brittle dependence on
 * bean name in objects used with Gratify IoC and Gratify AOP.
 *
 * @author Rod Johnson
 * @see ArtifactNameAware
 */
public interface NamedArtifact {
   /**
    * Return the name of this bean in a Gratify bean factory, if known.
    */
   String getArtifactName();
}
