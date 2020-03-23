package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.PropertyEditorRegistrar;
import foundation.polar.gratify.artifacts.PropertyEditorRegistry;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.HierarchicalArtifactFactory;
import foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.utils.StringValueResolver;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

/**
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}
 * interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} or
 * {@link foundation.polar.gratify.artifacts.factory.ListableArtifactFactory} for typical
 * needs. This extended interface is just meant to allow for framework-internal
 * plug'n'play and for special access to bean factory configuration methods.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory
 * @see foundation.polar.gratify.artifacts.factory.ListableArtifactFactory
 * @see ConfigurableListableArtifactFactory
 */
public interface ConfigurableArtifactFactory extends HierarchicalArtifactFactory, SingletonArtifactRegistry {
   /**
    * Scope identifier for the standard singleton scope: {@value}.
    * <p>Custom scopes can be added via {@code registerScope}.
    * @see #registerScope
    */
   String SCOPE_SINGLETON = "singleton";

   /**
    * Scope identifier for the standard prototype scope: {@value}.
    * <p>Custom scopes can be added via {@code registerScope}.
    * @see #registerScope
    */
   String SCOPE_PROTOTYPE = "prototype";

   /**
    * Set the parent of this bean factory.
    * <p>Note that the parent cannot be changed: It should only be set outside
    * a constructor if it isn't available at the time of factory instantiation.
    * @param parentArtifactFactory the parent ArtifactFactory
    * @throws IllegalStateException if this factory is already associated with
    * a parent ArtifactFactory
    * @see #getParentArtifactFactory()
    */
   void setParentArtifactFactory(ArtifactFactory parentArtifactFactory) throws IllegalStateException;

   /**
    * Set the class loader to use for loading bean classes.
    * Default is the thread context class loader.
    * <p>Note that this class loader will only apply to bean definitions
    * that do not carry a resolved bean class yet. This is the case as of
    * Gratify 2.0 by default: Artifact definitions only carry bean class names,
    * to be resolved once the factory processes the bean definition.
    * @param beanClassLoader the class loader to use,
    * or {@code null} to suggest the default class loader
    */
   void setArtifactClassLoader(@Nullable ClassLoader beanClassLoader);

   /**
    * Return this factory's class loader for loading bean classes
    * (only {@code null} if even the system ClassLoader isn't accessible).
    * @see foundation.polar.gratify.util.ClassUtils#forName(String, ClassLoader)
    */
   @Nullable
   ClassLoader getArtifactClassLoader();

   /**
    * Specify a temporary ClassLoader to use for type matching purposes.
    * Default is none, simply using the standard bean ClassLoader.
    * <p>A temporary ClassLoader is usually just specified if
    * <i>load-time weaving</i> is involved, to make sure that actual bean
    * classes are loaded as lazily as possible. The temporary loader is
    * then removed once the ArtifactFactory completes its bootstrap phase.
    */
   void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

   /**
    * Return the temporary ClassLoader to use for type matching purposes,
    * if any.
    */
   @Nullable
   ClassLoader getTempClassLoader();

   /**
    * Set whether to cache bean metadata such as given bean definitions
    * (in merged fashion) and resolved bean classes. Default is on.
    * <p>Turn this flag off to enable hot-refreshing of bean definition objects
    * and in particular bean classes. If this flag is off, any creation of a bean
    * instance will re-query the bean class loader for newly resolved classes.
    */
   void setCacheArtifactMetadata(boolean cacheArtifactMetadata);

   /**
    * Return whether to cache bean metadata such as given bean definitions
    * (in merged fashion) and resolved bean classes.
    */
   boolean isCacheArtifactMetadata();

   /**
    * Specify the resolution strategy for expressions in bean definition values.
    * <p>There is no expression support active in a ArtifactFactory by default.
    * An ApplicationContext will typically set a standard expression strategy
    * here, supporting "#{...}" expressions in a Unified EL compatible style.
    */
   void setArtifactExpressionResolver(@Nullable ArtifactExpressionResolver resolver);

   /**
    * Return the resolution strategy for expressions in bean definition values.
    */
   @Nullable
   ArtifactExpressionResolver getArtifactExpressionResolver();

   /**
    * Specify a ConversionService to use for converting
    * property values, as an alternative to JavaBeans PropertyEditors.
    */
   void setConversionService(@Nullable ConversionService conversionService);

   /**
    * Return the associated ConversionService, if any.
    */
   @Nullable
   ConversionService getConversionService();

   /**
    * Add a PropertyEditorRegistrar to be applied to all bean creation processes.
    * <p>Such a registrar creates new PropertyEditor instances and registers them
    * on the given registry, fresh for each bean creation attempt. This avoids
    * the need for synchronization on custom editors; hence, it is generally
    * preferable to use this method instead of {@link #registerCustomEditor}.
    * @param registrar the PropertyEditorRegistrar to register
    */
   void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

   /**
    * Register the given custom property editor for all properties of the
    * given type. To be invoked during factory configuration.
    * <p>Note that this method will register a shared custom editor instance;
    * access to that instance will be synchronized for thread-safety. It is
    * generally preferable to use {@link #addPropertyEditorRegistrar} instead
    * of this method, to avoid for the need for synchronization on custom editors.
    * @param requiredType type of the property
    * @param propertyEditorClass the {@link PropertyEditor} class to register
    */
   void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

   /**
    * Initialize the given PropertyEditorRegistry with the custom editors
    * that have been registered with this ArtifactFactory.
    * @param registry the PropertyEditorRegistry to initialize
    */
   void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

   /**
    * Set a custom type converter that this ArtifactFactory should use for converting
    * bean property values, constructor argument values, etc.
    * <p>This will override the default PropertyEditor mechanism and hence make
    * any custom editors or custom editor registrars irrelevant.
    *
    * @see #addPropertyEditorRegistrar
    * @see #registerCustomEditor
    */
   void setTypeConverter(TypeConverter typeConverter);

   /**
    * Obtain a type converter as used by this ArtifactFactory. This may be a fresh
    * instance for each call, since TypeConverters are usually <i>not</i> thread-safe.
    * <p>If the default PropertyEditor mechanism is active, the returned
    * TypeConverter will be aware of all custom editors that have been registered.
    */
   TypeConverter getTypeConverter();

   /**
    * Add a String resolver for embedded values such as annotation attributes.
    * @param valueResolver the String resolver to apply to embedded values
    */
   void addEmbeddedValueResolver(StringValueResolver valueResolver);

   /**
    * Determine whether an embedded value resolver has been registered with this
    * bean factory, to be applied through {@link #resolveEmbeddedValue(String)}.
    */
   boolean hasEmbeddedValueResolver();

   /**
    * Resolve the given embedded value, e.g. an annotation attribute.
    * @param value the value to resolve
    * @return the resolved value (may be the original value as-is)
    */
   @Nullable
   String resolveEmbeddedValue(String value);

   /**
    * Add a new ArtifactPostProcessor that will get applied to beans created
    * by this factory. To be invoked during factory configuration.
    * <p>Note: Post-processors submitted here will be applied in the order of
    * registration; any ordering semantics expressed through implementing the
    * {@link foundation.polar.gratify.core.Ordered} interface will be ignored. Note
    * that autodetected post-processors (e.g. as beans in an ApplicationContext)
    * will always be applied after programmatically registered ones.
    * @param beanPostProcessor the post-processor to register
    */
   void addArtifactPostProcessor(ArtifactPostProcessor beanPostProcessor);

   /**
    * Return the current number of registered ArtifactPostProcessors, if any.
    */
   int getArtifactPostProcessorCount();

   /**
    * Register the given scope, backed by the given Scope implementation.
    * @param scopeName the scope identifier
    * @param scope the backing Scope implementation
    */
   void registerScope(String scopeName, Scope scope);

   /**
    * Return the names of all currently registered scopes.
    * <p>This will only return the names of explicitly registered scopes.
    * Built-in scopes such as "singleton" and "prototype" won't be exposed.
    * @return the array of scope names, or an empty array if none
    * @see #registerScope
    */
   String[] getRegisteredScopeNames();

   /**
    * Return the Scope implementation for the given scope name, if any.
    * <p>This will only return explicitly registered scopes.
    * Built-in scopes such as "singleton" and "prototype" won't be exposed.
    * @param scopeName the name of the scope
    * @return the registered Scope implementation, or {@code null} if none
    * @see #registerScope
    */
   @Nullable
   Scope getRegisteredScope(String scopeName);

   /**
    * Provides a security access control context relevant to this factory.
    * @return the applicable AccessControlContext (never {@code null})
    */
   AccessControlContext getAccessControlContext();

   /**
    * Copy all relevant configuration from the given other factory.
    * <p>Should include all standard configuration settings as well as
    * ArtifactPostProcessors, Scopes, and factory-specific internal settings.
    * Should not include any metadata of actual bean definitions,
    * such as ArtifactDefinition objects and bean name aliases.
    * @param otherFactory the other ArtifactFactory to copy from
    */
   void copyConfigurationFrom(ConfigurableArtifactFactory otherFactory);

   /**
    * Given a bean name, create an alias. We typically use this method to
    * support names that are illegal within XML ids (used for bean names).
    * <p>Typically invoked during factory configuration, but can also be
    * used for runtime registration of aliases. Therefore, a factory
    * implementation should synchronize alias access.
    * @param artifactName the canonical name of the target bean
    * @param alias the alias to be registered for the bean
    * @throws ArtifactDefinitionStoreException if the alias is already in use
    */
   void registerAlias(String artifactName, String alias) throws ArtifactDefinitionStoreException;

   /**
    * Resolve all alias target names and aliases registered in this
    * factory, applying the given StringValueResolver to them.
    * <p>The value resolver may for example resolve placeholders
    * in target bean names and even in alias names.
    * @param valueResolver the StringValueResolver to apply
    */
   void resolveAliases(StringValueResolver valueResolver);

   /**
    * Return a merged ArtifactDefinition for the given bean name,
    * merging a child bean definition with its parent if necessary.
    * Considers bean definitions in ancestor factories as well.
    * @param artifactName the name of the bean to retrieve the merged definition for
    * @return a (potentially merged) ArtifactDefinition for the given bean
    * @throws NoSuchArtifactDefinitionException if there is no bean definition with the given name
    */
   ArtifactDefinition getMergedArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException;

   /**
    * Determine whether the bean with the given name is a FactoryArtifact.
    * @param name the name of the bean to check
    * @return whether the bean is a FactoryArtifact
    * ({@code false} means the bean exists but is not a FactoryArtifact)
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    */
   boolean isFactoryArtifact(String name) throws NoSuchArtifactDefinitionException;

   /**
    * Explicitly control the current in-creation status of the specified bean.
    * For container-internal use only.
    * @param artifactName the name of the bean
    * @param inCreation whether the bean is currently in creation
    */
   void setCurrentlyInCreation(String artifactName, boolean inCreation);

   /**
    * Determine whether the specified bean is currently in creation.
    * @param artifactName the name of the bean
    * @return whether the bean is currently in creation
    */
   boolean isCurrentlyInCreation(String artifactName);

   /**
    * Register a dependent bean for the given bean,
    * to be destroyed before the given bean is destroyed.
    * @param artifactName the name of the bean
    * @param dependentArtifactName the name of the dependent bean
    */
   void registerDependentArtifact(String artifactName, String dependentArtifactName);

   /**
    * Return the names of all beans which depend on the specified bean, if any.
    * @param artifactName the name of the bean
    * @return the array of dependent bean names, or an empty array if none
    */
   String[] getDependentArtifacts(String artifactName);

   /**
    * Return the names of all beans that the specified bean depends on, if any.
    * @param artifactName the name of the bean
    * @return the array of names of beans which the bean depends on,
    * or an empty array if none
    */
   String[] getDependenciesForArtifact(String artifactName);

   /**
    * Destroy the given bean instance (usually a prototype instance
    * obtained from this factory) according to its bean definition.
    * <p>Any exception that arises during destruction should be caught
    * and logged instead of propagated to the caller of this method.
    * @param artifactName the name of the bean definition
    * @param beanInstance the bean instance to destroy
    */
   void destroyArtifact(String artifactName, Object beanInstance);

   /**
    * Destroy the specified scoped bean in the current target scope, if any.
    * <p>Any exception that arises during destruction should be caught
    * and logged instead of propagated to the caller of this method.
    * @param artifactName the name of the scoped bean
    */
   void destroyScopedArtifact(String artifactName);

   /**
    * Destroy all singleton beans in this factory, including inner beans that have
    * been registered as disposable. To be called on shutdown of a factory.
    * <p>Any exception that arises during destruction should be caught
    * and logged instead of propagated to the caller of this method.
    */
   void destroySingletons();
}
