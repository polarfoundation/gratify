package foundation.polar.gratify.artifacts.factory;

/**
 * Interface to be implemented by beans that need to react once all their properties
 * have been set by a {@link ArtifactFactory}: e.g. to perform custom initialization,
 * or merely to check that all mandatory properties have been set.
 *
 * <p>An alternative to implementing {@code InitializingArtifact} is specifying a custom
 * init method, for example in an XML bean definition. For a list of all bean
 * lifecycle methods, see the {@link ArtifactFactory ArtifactFactory javadocs}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DisposableArtifact
 * @see foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition#getPropertyValues()
 * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#getInitMethodName()
 */
public interface InitializingArtifact {
   /**
    * Invoked by the containing {@code ArtifactFactory} after it has set all bean properties
    * and satisfied {@link ArtifactFactoryAware}, {@code ApplicationContextAware} etc.
    * <p>This method allows the bean instance to perform validation of its overall
    * configuration and final initialization when all bean properties have been set.
    * @throws Exception in the event of misconfiguration (such as failure to set an
    * essential property) or if initialization fails for any other reason
    */
   void afterPropertiesSet() throws Exception;
}
