package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.PropertyValues;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.lang.reflect.Constructor;

/**
 * Adapter that implements all methods on {@link SmartInstantiationAwareArtifactPostProcessor}
 * as no-ops, which will not change normal processing of each artifact instantiated
 * by the container. Subclasses may override merely those methods that they are
 * actually interested in.
 *
 * <p>Note that this base class is only recommendable if you actually require
 * {@link InstantiationAwareArtifactPostProcessor} functionality. If all you need
 * is plain {@link ArtifactPostProcessor} functionality, prefer a straight
 * implementation of that (simpler) interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class InstantiationAwareArtifactPostProcessorAdapter
   implements SmartInstantiationAwareArtifactPostProcessor {
   @Override
   @Nullable
   public Class<?> predictArtifactType(Class<?> artifactClass, String artifactName) throws ArtifactsException {
      return null;
   }

   @Override
   @Nullable
   public Constructor<?>[] determineCandidateConstructors(Class<?> artifactClass, String artifactName)
      throws ArtifactsException {
      return null;
   }

   @Override
   public Object getEarlyArtifactReference(Object artifact, String artifactName)
      throws ArtifactsException {
      return artifact;
   }

   @Override
   @Nullable
   public Object postProcessBeforeInstantiation(Class<?> artifactClass, String artifactName)
      throws ArtifactsException {
      return null;
   }

   @Override
   public boolean postProcessAfterInstantiation(Object artifact, String artifactName)
      throws ArtifactsException {
      return true;
   }

   @Override
   public PropertyValues postProcessProperties(PropertyValues pvs, Object artifact, String artifactName)
      throws ArtifactsException {
      return null;
   }

   @Override
   public Object postProcessBeforeInitialization(Object artifact, String artifactName)
      throws ArtifactsException {
      return artifact;
   }

   @Override
   public Object postProcessAfterInitialization(Object artifact, String artifactName)
      throws ArtifactsException {
      return artifact;
   }
}
