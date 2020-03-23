package foundation.polar.gratify.artifacts.factory.support;

/**
 * Interface to be implemented by beans that want to release resources on destruction.
 * A {@link ArtifactFactory} will invoke the destroy method on individual destruction of a
 * scoped bean. An {@link foundation.polar.gratify.context.ApplicationContext} is supposed
 * to dispose all of its singletons on shutdown, driven by the application lifecycle.
 *
 * <p>A Gratify-managed bean may also implement Java's {@link AutoCloseable} interface
 * for the same purpose. An alternative to implementing an interface is specifying a
 * custom destroy method, for example in an XML bean definition. For a list of all
 * bean lifecycle methods, see the {@link ArtifactFactory ArtifactFactory javadocs}.
 *
 * @author Juergen Hoeller
 *
 * @see InitializingArtifact
 * @see foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition#getDestroyMethodName()
 * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory#destroySingletons()
 * @see foundation.polar.gratify.context.ConfigurableApplicationContext#close()
 */
public interface DisposableArtifact {
   /**
    * Invoked by the containing {@code ArtifactFactory} on destruction of a bean.
    * @throws Exception in case of shutdown errors. Exceptions will get logged
    * but not rethrown to allow other beans to release their resources as well.
    */
   void destroy() throws Exception;
}
