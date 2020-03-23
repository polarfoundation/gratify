package foundation.polar.gratify.artifacts.factory;

/**
 * Extension of the {@link FactoryArtifact} interface. Implementations may
 * indicate whether they always return independent instances, for the
 * case where their {@link #isSingleton()} implementation returning
 * {@code false} does not clearly indicate independent instances.
 *
 * <p>Plain {@link FactoryArtifact} implementations which do not implement
 * this extended interface are simply assumed to always return independent
 * instances if their {@link #isSingleton()} implementation returns
 * {@code false}; the exposed object is only accessed on demand.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework and within collaborating frameworks.
 * In general, application-provided FactoryArtifacts should simply implement
 * the plain {@link FactoryArtifact} interface. New methods might be added
 * to this extended interface even in point releases.
 *
 * @author Juergen Hoeller
 * @param <T> the bean type
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryArtifact<T> extends FactoryArtifact<T> {

   /**
    * Is the object managed by this factory a prototype? That is,
    * will {@link #getObject()} always return an independent instance?
    * <p>The prototype status of the FactoryArtifact itself will generally
    * be provided by the owning {@link ArtifactFactory}; usually, it has to be
    * defined as singleton there.
    * <p>This method is supposed to strictly check for independent instances;
    * it should not return {@code true} for scoped objects or other
    * kinds of non-singleton, non-independent objects. For this reason,
    * this is not simply the inverted form of {@link #isSingleton()}.
    * <p>The default implementation returns {@code false}.
    * @return whether the exposed object is a prototype
    * @see #getObject()
    * @see #isSingleton()
    */
   default boolean isPrototype() {
      return false;
   }

   /**
    * Does this FactoryArtifact expect eager initialization, that is,
    * eagerly initialize itself as well as expect eager initialization
    * of its singleton object (if any)?
    * <p>A standard FactoryArtifact is not expected to initialize eagerly:
    * Its {@link #getObject()} will only be called for actual access, even
    * in case of a singleton object. Returning {@code true} from this
    * method suggests that {@link #getObject()} should be called eagerly,
    * also applying post-processors eagerly. This may make sense in case
    * of a {@link #isSingleton() singleton} object, in particular if
    * post-processors expect to be applied on startup.
    * <p>The default implementation returns {@code false}.
    * @return whether eager initialization applies
    * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory#preInstantiateSingletons()
    */
   default boolean isEagerInit() {
      return false;
   }

}
