package foundation.polar.gratify.artifacts.factory;


/**
 * Callback interface triggered at the end of the singleton pre-instantiation phase
 * during {@link ArtifactFactory} bootstrap. This interface can be implemented by
 * singleton beans in order to perform some initialization after the regular
 * singleton instantiation algorithm, avoiding side effects with accidental early
 * initialization (e.g. from {@link ListableArtifactFactory#getArtifactsOfType} calls).
 * In that sense, it is an alternative to {@link InitializingArtifact} which gets
 * triggered right at the end of a bean's local construction phase.
 *
 * <p>This callback variant is somewhat similar to
 * {@link foundation.polar.gratify.context.event.ContextRefreshedEvent} but doesn't
 * require an implementation of {@link foundation.polar.gratify.context.ApplicationListener},
 * with no need to filter context references across a context hierarchy etc.
 * It also implies a more minimal dependency on just the {@code beans} package
 * and is being honored by standalone {@link ListableArtifactFactory} implementations,
 * not just in an {@link foundation.polar.gratify.context.ApplicationContext} environment.
 *
 * <p><b>NOTE:</b> If you intend to start/manage asynchronous tasks, preferably
 * implement {@link foundation.polar.gratify.context.Lifecycle} instead which offers
 * a richer model for runtime management and allows for phased startup/shutdown.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory#preInstantiateSingletons()
 */
public interface SmartInitializingSingleton {

   /**
    * Invoked right at the end of the singleton pre-instantiation phase,
    * with a guarantee that all regular singleton beans have been created
    * already. {@link ListableArtifactFactory#getArtifactsOfType} calls within
    * this method won't trigger accidental side effects during bootstrap.
    * <p><b>NOTE:</b> This callback won't be triggered for singleton beans
    * lazily initialized on demand after {@link ArtifactFactory} bootstrap,
    * and not for any other bean scope either. Carefully use it for beans
    * with the intended bootstrap semantics only.
    */
   void afterSingletonsInstantiated();

}
