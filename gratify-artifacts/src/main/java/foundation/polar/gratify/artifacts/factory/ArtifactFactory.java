package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.core.ResolvableType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The root interface for accessing a Gratify bean container.
 * This is the basic client view of a bean container;
 * further interfaces such as {@link ListableArtifactFactory} and
 * {@link foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory}
 * are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same. Since Gratify
 * 2.0, further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the ArtifactFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * ArtifactFactory lookup. Gratify's Dependency Injection functionality is
 * implemented using this ArtifactFactory interface and its subinterfaces.
 *
 * <p>Normally a ArtifactFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@codefoundation.polar.gratify.artifacts}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 *
 * <p>In contrast to the methods in {@link ListableArtifactFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalArtifactFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Artifacts in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Artifact factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>ArtifactNameAware's {@code setArtifactName}
 * <li>ArtifactClassLoaderAware's {@code setArtifactClassLoader}
 * <li>ArtifactFactoryAware's {@code setArtifactFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>EmbeddedValueResolverAware's {@code setEmbeddedValueResolver}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>MessageSourceAware's {@code setMessageSource}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of ArtifactPostProcessors
 * <li>InitializingArtifact's {@code afterPropertiesSet}
 * <li>a custom init-method definition
 * <li>{@code postProcessAfterInitialization} methods of ArtifactPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareArtifactPostProcessors
 * <li>DisposableArtifact's {@code destroy}
 * <li>a custom destroy-method definition
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see ArtifactNameAware#setArtifactName
 * @see ArtifactClassLoaderAware#setArtifactClassLoader
 * @see ArtifactFactoryAware#setArtifactFactory
 * @see foundation.polar.gratify.di.ResourceLoaderAware#setResourceLoader
 * @see foundation.polar.gratify.di.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see foundation.polar.gratify.di.MessageSourceAware#setMessageSource
 * @see foundation.polar.gratify.di.ApplicationContextAware#setApplicationContext
 * @see foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor#postProcessBeforeInitialization
 * @see InitializingArtifact#afterPropertiesSet
 * @see foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition#getInitMethodName
 * @see foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor#postProcessAfterInitialization
 * @see DisposableArtifact#destroy
 * @see foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition#getDestroyMethodName
 */
public interface ArtifactFactory {
   /**
    * Used to dereference a {@link FactoryArtifact} instance and distinguish it from
    * beans <i>created</i> by the FactoryArtifact. For example, if the bean named
    * {@code myJndiObject} is a FactoryArtifact, getting {@code &myJndiObject}
    * will return the factory, not the instance returned by the factory.
    */
   String FACTORY_BEAN_PREFIX = "&";

   /**
    * Return an instance, which may be shared or independent, of the specified bean.
    * <p>This method allows a Gratify ArtifactFactory to be used as a replacement for the
    * Singleton or Prototype design pattern. Callers may retain references to
    * returned objects in the case of Singleton beans.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to retrieve
    * @return an instance of the bean
    * @throws NoSuchArtifactDefinitionException if there is no bean with the specified name
    * @throws ArtifactsException if the bean could not be obtained
    */
   Object getArtifact(String name) throws ArtifactsException;

   /**
    * Return an instance, which may be shared or independent, of the specified bean.
    * <p>Behaves the same as {@link #getArtifact(String)}, but provides a measure of type
    * safety by throwing a ArtifactNotOfRequiredTypeException if the bean is not of the
    * required type. This means that ClassCastException can't be thrown on casting
    * the result correctly, as can happen with {@link #getArtifact(String)}.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to retrieve
    * @param requiredType type the bean must match; can be an interface or superclass
    * @return an instance of the bean
    * @throws NoSuchArtifactDefinitionException if there is no such bean definition
    * @throws ArtifactNotOfRequiredTypeException if the bean is not of the required type
    * @throws ArtifactsException if the bean could not be created
    */
   <T> T getArtifact(String name, Class<T> requiredType) throws ArtifactsException;

   /**
    * Return an instance, which may be shared or independent, of the specified bean.
    * <p>Allows for specifying explicit constructor arguments / factory method arguments,
    * overriding the specified default arguments (if any) in the bean definition.
    * @param name the name of the bean to retrieve
    * @param args arguments to use when creating a bean instance using explicit arguments
    * (only applied when creating a new instance as opposed to retrieving an existing one)
    * @return an instance of the bean
    * @throws NoSuchArtifactDefinitionException if there is no such bean definition
    * @throws ArtifactDefinitionStoreException if arguments have been given but
    * the affected bean isn't a prototype
    * @throws ArtifactsException if the bean could not be created
    */
   Object getArtifact(String name, Object... args) throws ArtifactsException;

   /**
    * Return the bean instance that uniquely matches the given object type, if any.
    * <p>This method goes into {@link ListableArtifactFactory} by-type lookup territory
    * but may also be translated into a conventional by-name lookup based on the name
    * of the given type. For more extensive retrieval operations across sets of beans,
    * use {@link ListableArtifactFactory} and/or {@link ArtifactFactoryUtils}.
    * @param requiredType type the bean must match; can be an interface or superclass
    * @return an instance of the single bean matching the required type
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    * @throws ArtifactsException if the bean could not be created
    * @see ListableArtifactFactory
    */
   <T> T getArtifact(Class<T> requiredType) throws ArtifactsException;

   /**
    * Return an instance, which may be shared or independent, of the specified bean.
    * <p>Allows for specifying explicit constructor arguments / factory method arguments,
    * overriding the specified default arguments (if any) in the bean definition.
    * <p>This method goes into {@link ListableArtifactFactory} by-type lookup territory
    * but may also be translated into a conventional by-name lookup based on the name
    * of the given type. For more extensive retrieval operations across sets of beans,
    * use {@link ListableArtifactFactory} and/or {@link ArtifactFactoryUtils}.
    * @param requiredType type the bean must match; can be an interface or superclass
    * @param args arguments to use when creating a bean instance using explicit arguments
    * (only applied when creating a new instance as opposed to retrieving an existing one)
    * @return an instance of the bean
    * @throws NoSuchArtifactDefinitionException if there is no such bean definition
    * @throws ArtifactDefinitionStoreException if arguments have been given but
    * the affected bean isn't a prototype
    * @throws ArtifactsException if the bean could not be created
    */
   <T> T getArtifact(Class<T> requiredType, Object... args) throws ArtifactsException;

   /**
    * Return a provider for the specified bean, allowing for lazy on-demand retrieval
    * of instances, including availability and uniqueness options.
    * @param requiredType type the bean must match; can be an interface or superclass
    * @return a corresponding provider handle
    * @see #getArtifactProvider(ResolvableType)
    */
   <T> ObjectProvider<T> getArtifactProvider(Class<T> requiredType);

   /**
    * Return a provider for the specified bean, allowing for lazy on-demand retrieval
    * of instances, including availability and uniqueness options.
    * @param requiredType type the bean must match; can be a generic type declaration.
    * Note that collection types are not supported here, in contrast to reflective
    * injection points. For programmatically retrieving a list of beans matching a
    * specific type, specify the actual bean type as an argument here and subsequently
    * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
    * @return a corresponding provider handle
    * @see ObjectProvider#iterator()
    * @see ObjectProvider#stream()
    * @see ObjectProvider#orderedStream()
    */
   <T> ObjectProvider<T> getArtifactProvider(ResolvableType requiredType);

   /**
    * Does this bean factory contain a bean definition or externally registered singleton
    * instance with the given name?
    * <p>If the given name is an alias, it will be translated back to the corresponding
    * canonical bean name.
    * <p>If this factory is hierarchical, will ask any parent factory if the bean cannot
    * be found in this factory instance.
    * <p>If a bean definition or singleton instance matching the given name is found,
    * this method will return {@code true} whether the named bean definition is concrete
    * or abstract, lazy or eager, in scope or not. Therefore, note that a {@code true}
    * return value from this method does not necessarily indicate that {@link #getArtifact}
    * will be able to obtain an instance for the same name.
    * @param name the name of the bean to query
    * @return whether a bean with the given name is present
    */
   boolean containsArtifact(String name);

   /**
    * Is this bean a shared singleton? That is, will {@link #getArtifact} always
    * return the same instance?
    * <p>Note: This method returning {@code false} does not clearly indicate
    * independent instances. It indicates non-singleton instances, which may correspond
    * to a scoped bean as well. Use the {@link #isPrototype} operation to explicitly
    * check for independent instances.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @return whether this bean corresponds to a singleton instance
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    * @see #getArtifact
    * @see #isPrototype
    */
   boolean isSingleton(String name) throws NoSuchArtifactDefinitionException;

   /**
    * Is this bean a prototype? That is, will {@link #getArtifact} always return
    * independent instances?
    * <p>Note: This method returning {@code false} does not clearly indicate
    * a singleton object. It indicates non-independent instances, which may correspond
    * to a scoped bean as well. Use the {@link #isSingleton} operation to explicitly
    * check for a shared singleton instance.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @return whether this bean will always deliver independent instances
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    *
    * @see #getArtifact
    * @see #isSingleton
    */
   boolean isPrototype(String name) throws NoSuchArtifactDefinitionException;

   /**
    * Check whether the bean with the given name matches the specified type.
    * More specifically, check whether a {@link #getArtifact} call for the given name
    * would return an object that is assignable to the specified target type.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @param typeToMatch the type to match against (as a {@code ResolvableType})
    * @return {@code true} if the bean type matches,
    * {@code false} if it doesn't match or cannot be determined yet
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    *
    * @see #getArtifact
    * @see #getType
    */
   boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchArtifactDefinitionException;

   /**
    * Check whether the bean with the given name matches the specified type.
    * More specifically, check whether a {@link #getArtifact} call for the given name
    * would return an object that is assignable to the specified target type.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @param typeToMatch the type to match against (as a {@code Class})
    * @return {@code true} if the bean type matches,
    * {@code false} if it doesn't match or cannot be determined yet
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    *
    * @see #getArtifact
    * @see #getType
    */
   boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchArtifactDefinitionException;

   /**
    * Determine the type of the bean with the given name. More specifically,
    * determine the type of object that {@link #getArtifact} would return for the given name.
    * <p>For a {@link FactoryArtifact}, return the type of object that the FactoryArtifact creates,
    * as exposed by {@link FactoryArtifact#getObjectType()}. This may lead to the initialization
    * of a previously uninitialized {@code FactoryArtifact} (see {@link #getType(String, boolean)}).
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @return the type of the bean, or {@code null} if not determinable
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    *
    * @see #getArtifact
    * @see #isTypeMatch
    */
   @Nullable
   Class<?> getType(String name) throws NoSuchArtifactDefinitionException;

   /**
    * Determine the type of the bean with the given name. More specifically,
    * determine the type of object that {@link #getArtifact} would return for the given name.
    * <p>For a {@link FactoryArtifact}, return the type of object that the FactoryArtifact creates,
    * as exposed by {@link FactoryArtifact#getObjectType()}. Depending on the
    * {@code allowFactoryArtifactInit} flag, this may lead to the initialization of a previously
    * uninitialized {@code FactoryArtifact} if no early type information is available.
    * <p>Translates aliases back to the corresponding canonical bean name.
    * Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the name of the bean to query
    * @param allowFactoryArtifactInit whether a {@code FactoryArtifact} may get initialized
    * just for the purpose of determining its object type
    * @return the type of the bean, or {@code null} if not determinable
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    * @see #getArtifact
    * @see #isTypeMatch
    */
   @Nullable
   Class<?> getType(String name, boolean allowFactoryArtifactInit) throws NoSuchArtifactDefinitionException;

   /**
    * Return the aliases for the given bean name, if any.
    * All of those aliases point to the same bean when used in a {@link #getArtifact} call.
    * <p>If the given name is an alias, the corresponding original bean name
    * and other aliases (if any) will be returned, with the original bean name
    * being the first element in the array.
    * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
    * @param name the bean name to check for aliases
    * @return the aliases, or an empty array if none
    * @see #getArtifact
    */
   String[] getAliases(String name);
}
