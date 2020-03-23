package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.ResourceLoader;
import foundation.polar.gratify.core.io.support.PathMatchingResourcePatternResolver;
import foundation.polar.gratify.core.io.support.ResourcePatternResolver;
import foundation.polar.gratify.env.Environment;
import foundation.polar.gratify.env.EnvironmentCapable;
import foundation.polar.gratify.env.StandardEnvironment;
import foundation.polar.gratify.utils.AssertUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Abstract base class for bean definition readers which implement
 * the {@link ArtifactDefinitionReader} interface.
 *
 * <p>Provides common properties like the bean factory to work on
 * and the class loader to use for loading bean classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see ArtifactDefinitionReaderUtils
 */
public abstract class AbstractArtifactDefinitionReader implements ArtifactDefinitionReader, EnvironmentCapable {

   /** Logger available to subclasses. */
   protected final Log logger = LogFactory.getLog(getClass());

   private final ArtifactDefinitionRegistry registry;

   @Nullable
   private ResourceLoader resourceLoader;

   @Nullable
   private ClassLoader beanClassLoader;

   private Environment environment;

   private ArtifactNameGenerator beanNameGenerator = DefaultArtifactNameGenerator.INSTANCE;


   /**
    * Create a new AbstractArtifactDefinitionReader for the given bean factory.
    * <p>If the passed-in bean factory does not only implement the ArtifactDefinitionRegistry
    * interface but also the ResourceLoader interface, it will be used as default
    * ResourceLoader as well. This will usually be the case for
    * {@link foundation.polar.gratify.context.ApplicationContext} implementations.
    * <p>If given a plain ArtifactDefinitionRegistry, the default ResourceLoader will be a
    * {@link foundation.polar.gratify.core.io.support.PathMatchingResourcePatternResolver}.
    * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
    * environment will be used by this reader.  Otherwise, the reader will initialize and
    * use a {@link StandardEnvironment}. All ApplicationContext implementations are
    * EnvironmentCapable, while normal ArtifactFactory implementations are not.
    * @param registry the ArtifactFactory to load bean definitions into,
    * in the form of a ArtifactDefinitionRegistry
    * @see #setResourceLoader
    * @see #setEnvironment
    */
   protected AbstractArtifactDefinitionReader(ArtifactDefinitionRegistry registry) {
      AssertUtils.notNull(registry, "ArtifactDefinitionRegistry must not be null");
      this.registry = registry;

      // Determine ResourceLoader to use.
      if (this.registry instanceof ResourceLoader) {
         this.resourceLoader = (ResourceLoader) this.registry;
      }
      else {
         this.resourceLoader = new PathMatchingResourcePatternResolver();
      }

      // Inherit Environment if possible
      if (this.registry instanceof EnvironmentCapable) {
         this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
      }
      else {
         this.environment = new StandardEnvironment();
      }
   }


   public final ArtifactDefinitionRegistry getArtifactFactory() {
      return this.registry;
   }

   @Override
   public final ArtifactDefinitionRegistry getRegistry() {
      return this.registry;
   }

   /**
    * Set the ResourceLoader to use for resource locations.
    * If specifying a ResourcePatternResolver, the bean definition reader
    * will be capable of resolving resource patterns to Resource arrays.
    * <p>Default is PathMatchingResourcePatternResolver, also capable of
    * resource pattern resolving through the ResourcePatternResolver interface.
    * <p>Setting this to {@code null} suggests that absolute resource loading
    * is not available for this bean definition reader.
    * @see foundation.polar.gratify.core.io.support.ResourcePatternResolver
    * @see foundation.polar.gratify.core.io.support.PathMatchingResourcePatternResolver
    */
   public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
      this.resourceLoader = resourceLoader;
   }

   @Override
   @Nullable
   public ResourceLoader getResourceLoader() {
      return this.resourceLoader;
   }

   /**
    * Set the ClassLoader to use for bean classes.
    * <p>Default is {@code null}, which suggests to not load bean classes
    * eagerly but rather to just register bean definitions with class names,
    * with the corresponding Classes to be resolved later (or never).
    * @see Thread#getContextClassLoader()
    */
   public void setArtifactClassLoader(@Nullable ClassLoader beanClassLoader) {
      this.beanClassLoader = beanClassLoader;
   }

   @Override
   @Nullable
   public ClassLoader getArtifactClassLoader() {
      return this.beanClassLoader;
   }

   /**
    * Set the Environment to use when reading bean definitions. Most often used
    * for evaluating profile information to determine which bean definitions
    * should be read and which should be omitted.
    */
   public void setEnvironment(Environment environment) {
      AssertUtils.notNull(environment, "Environment must not be null");
      this.environment = environment;
   }

   @Override
   public Environment getEnvironment() {
      return this.environment;
   }

   /**
    * Set the ArtifactNameGenerator to use for anonymous beans
    * (without explicit bean name specified).
    * <p>Default is a {@link DefaultArtifactNameGenerator}.
    */
   public void setArtifactNameGenerator(@Nullable ArtifactNameGenerator beanNameGenerator) {
      this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : DefaultArtifactNameGenerator.INSTANCE);
   }

   @Override
   public ArtifactNameGenerator getArtifactNameGenerator() {
      return this.beanNameGenerator;
   }

   @Override
   public int loadArtifactDefinitions(Resource... resources) throws ArtifactDefinitionStoreException {
      AssertUtils.notNull(resources, "Resource array must not be null");
      int count = 0;
      for (Resource resource : resources) {
         count += loadArtifactDefinitions(resource);
      }
      return count;
   }

   @Override
   public int loadArtifactDefinitions(String location) throws ArtifactDefinitionStoreException {
      return loadArtifactDefinitions(location, null);
   }

   /**
    * Load bean definitions from the specified resource location.
    * <p>The location can also be a location pattern, provided that the
    * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
    * @param location the resource location, to be loaded with the ResourceLoader
    * (or ResourcePatternResolver) of this bean definition reader
    * @param actualResources a Set to be filled with the actual Resource objects
    * that have been resolved during the loading process. May be {@code null}
    * to indicate that the caller is not interested in those Resource objects.
    * @return the number of bean definitions found
    * @throws ArtifactDefinitionStoreException in case of loading or parsing errors
    * @see #getResourceLoader()
    * @see #loadArtifactDefinitions(foundation.polar.gratify.core.io.Resource)
    * @see #loadArtifactDefinitions(foundation.polar.gratify.core.io.Resource[])
    */
   public int loadArtifactDefinitions(String location, @Nullable Set<Resource> actualResources) throws ArtifactDefinitionStoreException {
      ResourceLoader resourceLoader = getResourceLoader();
      if (resourceLoader == null) {
         throw new ArtifactDefinitionStoreException(
            "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
      }

      if (resourceLoader instanceof ResourcePatternResolver) {
         // Resource pattern matching available.
         try {
            Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
            int count = loadArtifactDefinitions(resources);
            if (actualResources != null) {
               Collections.addAll(actualResources, resources);
            }
            if (logger.isTraceEnabled()) {
               logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
            }
            return count;
         }
         catch (IOException ex) {
            throw new ArtifactDefinitionStoreException(
               "Could not resolve bean definition resource pattern [" + location + "]", ex);
         }
      }
      else {
         // Can only load single resources by absolute URL.
         Resource resource = resourceLoader.getResource(location);
         int count = loadArtifactDefinitions(resource);
         if (actualResources != null) {
            actualResources.add(resource);
         }
         if (logger.isTraceEnabled()) {
            logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
         }
         return count;
      }
   }

   @Override
   public int loadArtifactDefinitions(String... locations) throws ArtifactDefinitionStoreException {
      AssertUtils.notNull(locations, "Location array must not be null");
      int count = 0;
      for (String location : locations) {
         count += loadArtifactDefinitions(location);
      }
      return count;
   }

}
