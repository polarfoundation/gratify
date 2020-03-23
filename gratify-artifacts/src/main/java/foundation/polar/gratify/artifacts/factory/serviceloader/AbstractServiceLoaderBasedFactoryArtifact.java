package foundation.polar.gratify.artifacts.factory.serviceloader;


import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;
import foundation.polar.gratify.artifacts.factory.config.AbstractFactoryArtifact;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ServiceLoader;

/**
 * Abstract base class for FactoryArtifacts operating on the
 * JDK 1.6 {@link java.util.ServiceLoader} facility.
 *
 * @author Juergen Hoeller
 *
 * @see java.util.ServiceLoader
 */
public abstract class AbstractServiceLoaderBasedFactoryArtifact extends AbstractFactoryArtifact<Object>
   implements ArtifactClassLoaderAware {

   @Nullable
   private Class<?> serviceType;

   @Nullable
   private ClassLoader artifactClassLoader = ClassUtils.getDefaultClassLoader();

   /**
    * Specify the desired service type (typically the service's public API).
    */
   public void setServiceType(@Nullable Class<?> serviceType) {
      this.serviceType = serviceType;
   }

   /**
    * Return the desired service type.
    */
   @Nullable
   public Class<?> getServiceType() {
      return this.serviceType;
   }

   @Override
   public void setArtifactClassLoader(@Nullable ClassLoader artifactClassLoader) {
      this.artifactClassLoader = artifactClassLoader;
   }

   /**
    * Delegates to {@link #getObjectToExpose(java.util.ServiceLoader)}.
    * @return the object to expose
    */
   @Override
   protected Object createInstance() {
      AssertUtils.notNull(getServiceType(), "Property 'serviceType' is required");
      return getObjectToExpose(ServiceLoader.load(getServiceType(), this.artifactClassLoader));
   }

   /**
    * Determine the actual object to expose for the given ServiceLoader.
    * <p>Left to concrete subclasses.
    * @param serviceLoader the ServiceLoader for the configured service class
    * @return the object to expose
    */
   protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);

}
