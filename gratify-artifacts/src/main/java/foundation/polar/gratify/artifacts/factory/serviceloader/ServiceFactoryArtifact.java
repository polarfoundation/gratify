package foundation.polar.gratify.artifacts.factory.serviceloader;


import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} that exposes the
 * 'primary' service for the configured service class, obtained through
 * the JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 * @see java.util.ServiceLoader
 */
public class ServiceFactoryArtifact
   extends AbstractServiceLoaderBasedFactoryArtifact implements ArtifactClassLoaderAware {

   @Override
   protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
      Iterator<?> it = serviceLoader.iterator();
      if (!it.hasNext()) {
         throw new IllegalStateException(
            "ServiceLoader could not find service for type [" + getServiceType() + "]");
      }
      return it.next();
   }

   @Override
   @Nullable
   public Class<?> getObjectType() {
      return getServiceType();
   }

}
