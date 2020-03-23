package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.factory.FactoryArtifact;
import foundation.polar.gratify.artifacts.factory.InitializingArtifact;
import foundation.polar.gratify.core.io.support.PropertiesLoaderSupport;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.Properties;

/**
 * Allows for making a properties file from a classpath location available
 * as Properties instance in a bean factory. Can be used to populate
 * any bean property of type Properties via a bean reference.
 *
 * <p>Supports loading from a properties file and/or setting local properties
 * on this FactoryArtifact. The created Properties instance will be merged from
 * loaded and local values. If neither a location nor local properties are set,
 * an exception will be thrown on initialization.
 *
 * <p>Can create a singleton or a new object on each request.
 * Default is a singleton.
 *
 * @author Juergen Hoeller
 * @see #setLocation
 * @see #setProperties
 * @see #setLocalOverride
 * @see java.util.Properties
 */
public class PropertiesFactoryArtifact extends PropertiesLoaderSupport
   implements FactoryArtifact<Properties>, InitializingArtifact {
   private boolean singleton = true;

   @Nullable
   private Properties singletonInstance;

   /**
    * Set whether a shared 'singleton' Properties instance should be
    * created, or rather a new Properties instance on each request.
    * <p>Default is "true" (a shared singleton).
    */
   public final void setSingleton(boolean singleton) {
      this.singleton = singleton;
   }

   @Override
   public final boolean isSingleton() {
      return this.singleton;
   }


   @Override
   public final void afterPropertiesSet() throws IOException {
      if (this.singleton) {
         this.singletonInstance = createProperties();
      }
   }

   @Override
   @Nullable
   public final Properties getObject() throws IOException {
      if (this.singleton) {
         return this.singletonInstance;
      }
      else {
         return createProperties();
      }
   }

   @Override
   public Class<Properties> getObjectType() {
      return Properties.class;
   }

   /**
    * Template method that subclasses may override to construct the object
    * returned by this factory. The default implementation returns the
    * plain merged Properties instance.
    * <p>Invoked on initialization of this FactoryArtifact in case of a
    * shared singleton; else, on each {@link #getObject()} call.
    * @return the object returned by this factory
    * @throws IOException if an exception occurred during properties loading
    * @see #mergeProperties()
    */
   protected Properties createProperties() throws IOException {
      return mergeProperties();
   }
}
