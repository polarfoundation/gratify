package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ListableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

/**
 * Configuration interface to be implemented by most listable bean factories.
 * In addition to {@link ConfigurableArtifactFactory}, it provides facilities to
 * analyze and modify bean definitions, and to pre-instantiate singletons.
 *
 * <p>This subinterface of {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}
 * is not meant to be used in normal application code: Stick to
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} or
 * {@link foundation.polar.gratify.artifacts.factory.ListableArtifactFactory} for typical
 * use cases. This interface is just meant to allow for framework-internal
 * plug'n'play even when needing access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.context.support.AbstractApplicationContext#getArtifactFactory()
 */
public interface ConfigurableListableArtifactFactory
   extends ListableArtifactFactory, AutowireCapableArtifactFactory, ConfigurableArtifactFactory {
   /**
    * Ignore the given dependency type for autowiring:
    * for example, String. Default is none.
    * @param type the dependency type to ignore
    */
   void ignoreDependencyType(Class<?> type);

   /**
    * Ignore the given dependency interface for autowiring.
    * <p>This will typically be used by application contexts to register
    * dependencies that are resolved in other ways, like ArtifactFactory through
    * ArtifactFactoryAware or ApplicationContext through ApplicationContextAware.
    * <p>By default, only the ArtifactFactoryAware interface is ignored.
    * For further types to ignore, invoke this method for each type.
    * @param ifc the dependency interface to ignore
    * @see foundation.polar.gratify.artifacts.factory.ArtifactFactoryAware
    * @see foundation.polar.gratify.context.ApplicationContextAware
    */
   void ignoreDependencyInterface(Class<?> ifc);

   /**
    * Register a special dependency type with corresponding autowired value.
    * <p>This is intended for factory/context references that are supposed
    * to be autowirable but are not defined as beans in the factory:
    * e.g. a dependency of type ApplicationContext resolved to the
    * ApplicationContext instance that the bean is living in.
    * <p>Note: There are no such default types registered in a plain ArtifactFactory,
    * not even for the ArtifactFactory interface itself.
    * @param dependencyType the dependency type to register. This will typically
    * be a base interface such as ArtifactFactory, with extensions of it resolved
    * as well if declared as an autowiring dependency (e.g. ListableArtifactFactory),
    * as long as the given value actually implements the extended interface.
    * @param autowiredValue the corresponding autowired value. This may also be an
    * implementation of the {@link foundation.polar.gratify.artifacts.factory.ObjectFactory}
    * interface, which allows for lazy resolution of the actual target value.
    */
   void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

   /**
    * Determine whether the specified bean qualifies as an autowire candidate,
    * to be injected into other beans which declare a dependency of matching type.
    * <p>This method checks ancestor factories as well.
    * @param beanName the name of the bean to check
    * @param descriptor the descriptor of the dependency to resolve
    * @return whether the bean should be considered as autowire candidate
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    */
   boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
      throws NoSuchArtifactDefinitionException;

   /**
    * Return the registered ArtifactDefinition for the specified bean, allowing access
    * to its property values and constructor argument value (which can be
    * modified during bean factory post-processing).
    * <p>A returned ArtifactDefinition object should not be a copy but the original
    * definition object as registered in the factory. This means that it should
    * be castable to a more specific implementation type, if necessary.
    * <p><b>NOTE:</b> This method does <i>not</i> consider ancestor factories.
    * It is only meant for accessing local bean definitions of this factory.
    * @param beanName the name of the bean
    * @return the registered ArtifactDefinition
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    * defined in this factory
    */
   ArtifactDefinition getArtifactDefinition(String beanName) throws NoSuchArtifactDefinitionException;

   /**
    * Return a unified view over all bean names managed by this factory.
    * <p>Includes bean definition names as well as names of manually registered
    * singleton instances, with bean definition names consistently coming first,
    * analogous to how type/annotation specific retrieval of bean names works.
    * @return the composite iterator for the bean names view
    * @see #containsArtifactDefinition
    * @see #registerSingleton
    * @see #getArtifactNamesForType
    * @see #getArtifactNamesForAnnotation
    */
   Iterator<String> getArtifactNamesIterator();

   /**
    * Clear the merged bean definition cache, removing entries for beans
    * which are not considered eligible for full metadata caching yet.
    * <p>Typically triggered after changes to the original bean definitions,
    * e.g. after applying a {@link ArtifactFactoryPostProcessor}. Note that metadata
    * for beans which have already been created at this point will be kept around.
    * @see #getArtifactDefinition
    * @see #getMergedArtifactDefinition
    */
   void clearMetadataCache();

   /**
    * Freeze all bean definitions, signalling that the registered bean definitions
    * will not be modified or post-processed any further.
    * <p>This allows the factory to aggressively cache bean definition metadata.
    */
   void freezeConfiguration();

   /**
    * Return whether this factory's bean definitions are frozen,
    * i.e. are not supposed to be modified or post-processed any further.
    * @return {@code true} if the factory's configuration is considered frozen
    */
   boolean isConfigurationFrozen();

   /**
    * Ensure that all non-lazy-init singletons are instantiated, also considering
    * {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact FactoryArtifacts}.
    * Typically invoked at the end of factory setup, if desired.
    * @throws ArtifactsException if one of the singleton beans could not be created.
    * Note: This may have left the factory with some beans already initialized!
    * Call {@link #destroySingletons()} for full cleanup in this case.
    * @see #destroySingletons()
    */
   void preInstantiateSingletons() throws ArtifactsException;
}
