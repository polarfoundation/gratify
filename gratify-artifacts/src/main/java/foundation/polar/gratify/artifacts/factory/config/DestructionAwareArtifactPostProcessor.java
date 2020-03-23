package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;

/**
 * Subinterface of {@link ArtifactPostProcessor} that adds a before-destruction callback.
 *
 * <p>The typical usage will be to invoke custom destruction callbacks on
 * specific artifact types, matching corresponding initialization callbacks.
 *
 * @author Juergen Hoeller
 */
public interface DestructionAwareArtifactPostProcessor extends ArtifactPostProcessor {
   /**
    * Apply this ArtifactPostProcessor to the given artifact instance before its
    * destruction, e.g. invoking custom destruction callbacks.
    * <p>Like DisposableArtifact's {@code destroy} and a custom destroy method, this
    * callback will only apply to artifacts which the container fully manages the
    * lifecycle for. This is usually the case for singletons and scoped artifacts.
    * @param artifact the artifact instance to be destroyed
    * @param artifactName the name of the artifact
    * @throws foundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see foundation.polar.gratify.artifacts.factory.DisposableArtifact#destroy()
    * @see foundation.polar.gratify.artifacts.factory.support.AbstractArtifactDefinition#setDestroyMethodName(String)
    */
   void postProcessBeforeDestruction(Object artifact, String artifactName) throws ArtifactsException;

   /**
    * Determine whether the given artifact instance requires destruction by this
    * post-processor.
    * <p>The default implementation returns {@code true}. If a pre-5 implementation
    * of {@code DestructionAwareArtifactPostProcessor} does not provide a concrete
    * implementation of this method, Gratify silently assumes {@code true} as well.
    * @param artifact the artifact instance to check
    * @return {@code true} if {@link #postProcessBeforeDestruction} is supposed to
    * be called for this artifact instance eventually, or {@code false} if not needed
    */
   default boolean requiresDestruction(Object artifact) {
      return true;
   }
}
