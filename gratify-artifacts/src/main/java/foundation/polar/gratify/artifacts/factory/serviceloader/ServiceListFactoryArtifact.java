package foundation.polar.gratify.artifacts.factory.serviceloader;

import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} that exposes <i>all</i>
 * services for the configured service class, represented as a List of service objects,
 * obtained through the JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 *
 * @see java.util.ServiceLoader
 */
public class ServiceListFactoryArtifact
   extends AbstractServiceLoaderBasedFactoryArtifact implements ArtifactClassLoaderAware {

   @Override
   protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
      List<Object> result = new LinkedList<>();
      for (Object loaderObject : serviceLoader) {
         result.add(loaderObject);
      }
      return result;
   }

   @Override
   public Class<?> getObjectType() {
      return List.class;
   }

}
