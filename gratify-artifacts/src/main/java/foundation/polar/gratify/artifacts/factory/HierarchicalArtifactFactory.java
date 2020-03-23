package foundation.polar.gratify.artifacts.factory;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentArtifactFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableArtifactFactory interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory#setParentArtifactFactory
 */
public interface HierarchicalArtifactFactory extends ArtifactFactory {
   /**
    * Return the parent bean factory, or {@code null} if there is none.
    */
   @Nullable
   ArtifactFactory getParentArtifactFactory();

   /**
    * Return whether the local bean factory contains a bean of the given name,
    * ignoring beans defined in ancestor contexts.
    * <p>This is an alternative to {@code containsArtifact}, ignoring a bean
    * of the given name from an ancestor bean factory.
    * @param name the name of the bean to query
    * @return whether a bean with the given name is defined in the local factory
    * @see ArtifactFactory#containsArtifact
    */
   boolean containsLocalArtifact(String name);
}
