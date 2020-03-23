package foundation.polar.gratify.artifacts.factory.serviceloader;

import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;

import java.util.ServiceLoader;

/**
 * {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} that exposes the
 * JDK 1.6 {@link java.util.ServiceLoader} for the configured service class.
 *
 * @author Juergen Hoeller
 * @see java.util.ServiceLoader
 */
public class ServiceLoaderFactoryArtifact
   extends AbstractServiceLoaderBasedFactoryArtifact implements ArtifactClassLoaderAware {

   @Override
   protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
      return serviceLoader;
   }

   @Override
   public Class<?> getObjectType() {
      return ServiceLoader.class;
   }

}
