package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * Extension of the {@link org.springframework.Artifacts.factory.ArtifactFactory}
 * interface to be implemented by Artifact factories that are capable of
 * autowiring, provided that they want to expose this functionality for
 * existing Artifact instances.
 *
 * <p>This subinterface of ArtifactFactory is not meant to be used in normal
 * application code: stick to {@link org.springframework.Artifacts.factory.ArtifactFactory}
 * or {@link org.springframework.Artifacts.factory.ListableArtifactFactory} for
 * typical use cases.
 *
 * <p>Integration code for other frameworks can leverage this interface to
 * wire and populate existing Artifact instances that Spring does not control
 * the lifecycle of. This is particularly useful for WebWork Actions and
 * Tapestry Page objects, for example.
 *
 * <p>Note that this interface is not implemented by
 * {@link org.springframework.context.ApplicationContext} facades,
 * as it is hardly ever used by application code. That said, it is available
 * from an application context too, accessible through ApplicationContext's
 * {@link org.springframework.context.ApplicationContext#getAutowireCapableArtifactFactory()}
 * method.
 *
 * <p>You may also implement the {@link org.springframework.Artifacts.factory.ArtifactFactoryAware}
 * interface, which exposes the internal ArtifactFactory even when running in an
 * ApplicationContext, to get access to an AutowireCapableArtifactFactory:
 * simply cast the passed-in ArtifactFactory to AutowireCapableArtifactFactory.
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see org.springframework.Artifacts.factory.ArtifactFactoryAware
 * @see org.springframework.Artifacts.factory.config.ConfigurableListableArtifactFactory
 * @see org.springframework.context.ApplicationContext#getAutowireCapableArtifactFactory()
 */
public interface AutowireCapableArtifactFactory extends ArtifactFactory {

   /**
    * Constant that indicates no externally defined autowiring. Note that
    * ArtifactFactoryAware etc and annotation-driven injection will still be applied.
    * @see #createArtifact
    * @see #autowire
    * @see #autowireArtifactProperties
    */
   int AUTOWIRE_NO = 0;

   /**
    * Constant that indicates autowiring Artifact properties by name
    * (applying to all Artifact property setters).
    * @see #createArtifact
    * @see #autowire
    * @see #autowireArtifactProperties
    */
   int AUTOWIRE_BY_NAME = 1;

   /**
    * Constant that indicates autowiring Artifact properties by type
    * (applying to all Artifact property setters).
    * @see #createArtifact
    * @see #autowire
    * @see #autowireArtifactProperties
    */
   int AUTOWIRE_BY_TYPE = 2;

   /**
    * Constant that indicates autowiring the greediest constructor that
    * can be satisfied (involves resolving the appropriate constructor).
    * @see #createArtifact
    * @see #autowire
    */
   int AUTOWIRE_CONSTRUCTOR = 3;

   /**
    * Constant that indicates determining an appropriate autowire strategy
    * through introspection of the Artifact class.
    * @see #createArtifact
    * @see #autowire
    * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
    * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
    */
   @Deprecated
   int AUTOWIRE_AUTODETECT = 4;

   /**
    * Suffix for the "original instance" convention when initializing an existing
    * Artifact instance: to be appended to the fully-qualified Artifact class name,
    * e.g. "com.mypackage.MyClass.ORIGINAL", in order to enforce the given instance
    * to be returned, i.e. no proxies etc.
    * @since 5.1
    * @see #initializeArtifact(Object, String)
    * @see #applyArtifactPostProcessorsBeforeInitialization(Object, String)
    * @see #applyArtifactPostProcessorsAfterInitialization(Object, String)
    */
   String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


   //-------------------------------------------------------------------------
   // Typical methods for creating and populating external Artifact instances
   //-------------------------------------------------------------------------

   /**
    * Fully create a new Artifact instance of the given class.
    * <p>Performs full initialization of the Artifact, including all applicable
    * {@link ArtifactPostProcessor ArtifactPostProcessors}.
    * <p>Note: This is intended for creating a fresh instance, populating annotated
    * fields and methods as well as applying all standard Artifact initialization callbacks.
    * It does <i>not</i> imply traditional by-name or by-type autowiring of properties;
    * use {@link #createArtifact(Class, int, boolean)} for those purposes.
    * @param ArtifactClass the class of the Artifact to create
    * @return the new Artifact instance
    * @throws ArtifactsException if instantiation or wiring failed
    */
   <T> T createArtifact(Class<T> ArtifactClass) throws ArtifactsException;

   /**
    * Populate the given Artifact instance through applying after-instantiation callbacks
    * and Artifact property post-processing (e.g. for annotation-driven injection).
    * <p>Note: This is essentially intended for (re-)populating annotated fields and
    * methods, either for new instances or for deserialized instances. It does
    * <i>not</i> imply traditional by-name or by-type autowiring of properties;
    * use {@link #autowireArtifactProperties} for those purposes.
    * @param existingArtifact the existing Artifact instance
    * @throws ArtifactsException if wiring failed
    */
   void autowireArtifact(Object existingArtifact) throws ArtifactsException;

   /**
    * Configure the given raw Artifact: autowiring Artifact properties, applying
    * Artifact property values, applying factory callbacks such as {@code setArtifactName}
    * and {@code setArtifactFactory}, and also applying all Artifact post processors
    * (including ones which might wrap the given raw Artifact).
    * <p>This is effectively a superset of what {@link #initializeArtifact} provides,
    * fully applying the configuration specified by the corresponding Artifact definition.
    * <b>Note: This method requires a Artifact definition for the given name!</b>
    * @param existingArtifact the existing Artifact instance
    * @param ArtifactName the name of the Artifact, to be passed to it if necessary
    * (a Artifact definition of that name has to be available)
    * @return the Artifact instance to use, either the original or a wrapped one
    * @throws org.springframework.Artifacts.factory.NoSuchArtifactDefinitionException
    * if there is no Artifact definition with the given name
    * @throws ArtifactsException if the initialization failed
    * @see #initializeArtifact
    */
   Object configureArtifact(Object existingArtifact, String ArtifactName) throws ArtifactsException;


   //-------------------------------------------------------------------------
   // Specialized methods for fine-grained control over the Artifact lifecycle
   //-------------------------------------------------------------------------

   /**
    * Fully create a new Artifact instance of the given class with the specified
    * autowire strategy. All constants defined in this interface are supported here.
    * <p>Performs full initialization of the Artifact, including all applicable
    * {@link ArtifactPostProcessor ArtifactPostProcessors}. This is effectively a superset
    * of what {@link #autowire} provides, adding {@link #initializeArtifact} behavior.
    * @param ArtifactClass the class of the Artifact to create
    * @param autowireMode by name or type, using the constants in this interface
    * @param dependencyCheck whether to perform a dependency check for objects
    * (not applicable to autowiring a constructor, thus ignored there)
    * @return the new Artifact instance
    * @throws ArtifactsException if instantiation or wiring failed
    * @see #AUTOWIRE_NO
    * @see #AUTOWIRE_BY_NAME
    * @see #AUTOWIRE_BY_TYPE
    * @see #AUTOWIRE_CONSTRUCTOR
    */
   Object createArtifact(Class<?> ArtifactClass, int autowireMode, boolean dependencyCheck) throws ArtifactsException;

   /**
    * Instantiate a new Artifact instance of the given class with the specified autowire
    * strategy. All constants defined in this interface are supported here.
    * Can also be invoked with {@code AUTOWIRE_NO} in order to just apply
    * before-instantiation callbacks (e.g. for annotation-driven injection).
    * <p>Does <i>not</i> apply standard {@link ArtifactPostProcessor ArtifactPostProcessors}
    * callbacks or perform any further initialization of the Artifact. This interface
    * offers distinct, fine-grained operations for those purposes, for example
    * {@link #initializeArtifact}. However, {@link InstantiationAwareArtifactPostProcessor}
    * callbacks are applied, if applicable to the construction of the instance.
    * @param ArtifactClass the class of the Artifact to instantiate
    * @param autowireMode by name or type, using the constants in this interface
    * @param dependencyCheck whether to perform a dependency check for object
    * references in the Artifact instance (not applicable to autowiring a constructor,
    * thus ignored there)
    * @return the new Artifact instance
    * @throws ArtifactsException if instantiation or wiring failed
    * @see #AUTOWIRE_NO
    * @see #AUTOWIRE_BY_NAME
    * @see #AUTOWIRE_BY_TYPE
    * @see #AUTOWIRE_CONSTRUCTOR
    * @see #AUTOWIRE_AUTODETECT
    * @see #initializeArtifact
    * @see #applyArtifactPostProcessorsBeforeInitialization
    * @see #applyArtifactPostProcessorsAfterInitialization
    */
   Object autowire(Class<?> ArtifactClass, int autowireMode, boolean dependencyCheck) throws ArtifactsException;

   /**
    * Autowire the Artifact properties of the given Artifact instance by name or type.
    * Can also be invoked with {@code AUTOWIRE_NO} in order to just apply
    * after-instantiation callbacks (e.g. for annotation-driven injection).
    * <p>Does <i>not</i> apply standard {@link ArtifactPostProcessor ArtifactPostProcessors}
    * callbacks or perform any further initialization of the Artifact. This interface
    * offers distinct, fine-grained operations for those purposes, for example
    * {@link #initializeArtifact}. However, {@link InstantiationAwareArtifactPostProcessor}
    * callbacks are applied, if applicable to the configuration of the instance.
    * @param existingArtifact the existing Artifact instance
    * @param autowireMode by name or type, using the constants in this interface
    * @param dependencyCheck whether to perform a dependency check for object
    * references in the Artifact instance
    * @throws ArtifactsException if wiring failed
    * @see #AUTOWIRE_BY_NAME
    * @see #AUTOWIRE_BY_TYPE
    * @see #AUTOWIRE_NO
    */
   void autowireArtifactProperties(Object existingArtifact, int autowireMode, boolean dependencyCheck)
      throws ArtifactsException;

   /**
    * Apply the property values of the Artifact definition with the given name to
    * the given Artifact instance. The Artifact definition can either define a fully
    * self-contained Artifact, reusing its property values, or just property values
    * meant to be used for existing Artifact instances.
    * <p>This method does <i>not</i> autowire Artifact properties; it just applies
    * explicitly defined property values. Use the {@link #autowireArtifactProperties}
    * method to autowire an existing Artifact instance.
    * <b>Note: This method requires a Artifact definition for the given name!</b>
    * <p>Does <i>not</i> apply standard {@link ArtifactPostProcessor ArtifactPostProcessors}
    * callbacks or perform any further initialization of the Artifact. This interface
    * offers distinct, fine-grained operations for those purposes, for example
    * {@link #initializeArtifact}. However, {@link InstantiationAwareArtifactPostProcessor}
    * callbacks are applied, if applicable to the configuration of the instance.
    * @param existingArtifact the existing Artifact instance
    * @param ArtifactName the name of the Artifact definition in the Artifact factory
    * (a Artifact definition of that name has to be available)
    * @throws org.springframework.Artifacts.factory.NoSuchArtifactDefinitionException
    * if there is no Artifact definition with the given name
    * @throws ArtifactsException if applying the property values failed
    * @see #autowireArtifactProperties
    */
   void applyArtifactPropertyValues(Object existingArtifact, String ArtifactName) throws ArtifactsException;

   /**
    * Initialize the given raw Artifact, applying factory callbacks
    * such as {@code setArtifactName} and {@code setArtifactFactory},
    * also applying all Artifact post processors (including ones which
    * might wrap the given raw Artifact).
    * <p>Note that no Artifact definition of the given name has to exist
    * in the Artifact factory. The passed-in Artifact name will simply be used
    * for callbacks but not checked against the registered Artifact definitions.
    * @param existingArtifact the existing Artifact instance
    * @param ArtifactName the name of the Artifact, to be passed to it if necessary
    * (only passed to {@link ArtifactPostProcessor ArtifactPostProcessors};
    * can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
    * enforce the given instance to be returned, i.e. no proxies etc)
    * @return the Artifact instance to use, either the original or a wrapped one
    * @throws ArtifactsException if the initialization failed
    * @see #ORIGINAL_INSTANCE_SUFFIX
    */
   Object initializeArtifact(Object existingArtifact, String ArtifactName) throws ArtifactsException;

   /**
    * Apply {@link ArtifactPostProcessor ArtifactPostProcessors} to the given existing Artifact
    * instance, invoking their {@code postProcessBeforeInitialization} methods.
    * The returned Artifact instance may be a wrapper around the original.
    * @param existingArtifact the existing Artifact instance
    * @param ArtifactName the name of the Artifact, to be passed to it if necessary
    * (only passed to {@link ArtifactPostProcessor ArtifactPostProcessors};
    * can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
    * enforce the given instance to be returned, i.e. no proxies etc)
    * @return the Artifact instance to use, either the original or a wrapped one
    * @throws ArtifactsException if any post-processing failed
    * @see ArtifactPostProcessor#postProcessBeforeInitialization
    * @see #ORIGINAL_INSTANCE_SUFFIX
    */
   Object applyArtifactPostProcessorsBeforeInitialization(Object existingArtifact, String ArtifactName)
      throws ArtifactsException;

   /**
    * Apply {@link ArtifactPostProcessor ArtifactPostProcessors} to the given existing Artifact
    * instance, invoking their {@code postProcessAfterInitialization} methods.
    * The returned Artifact instance may be a wrapper around the original.
    * @param existingArtifact the existing Artifact instance
    * @param ArtifactName the name of the Artifact, to be passed to it if necessary
    * (only passed to {@link ArtifactPostProcessor ArtifactPostProcessors};
    * can follow the {@link #ORIGINAL_INSTANCE_SUFFIX} convention in order to
    * enforce the given instance to be returned, i.e. no proxies etc)
    * @return the Artifact instance to use, either the original or a wrapped one
    * @throws ArtifactsException if any post-processing failed
    * @see ArtifactPostProcessor#postProcessAfterInitialization
    * @see #ORIGINAL_INSTANCE_SUFFIX
    */
   Object applyArtifactPostProcessorsAfterInitialization(Object existingArtifact, String ArtifactName)
      throws ArtifactsException;

   /**
    * Destroy the given Artifact instance (typically coming from {@link #createArtifact}),
    * applying the {@link org.springframework.Artifacts.factory.DisposableArtifact} contract as well as
    * registered {@link DestructionAwareArtifactPostProcessor DestructionAwareArtifactPostProcessors}.
    * <p>Any exception that arises during destruction should be caught
    * and logged instead of propagated to the caller of this method.
    * @param existingArtifact the Artifact instance to destroy
    */
   void destroyArtifact(Object existingArtifact);


   //-------------------------------------------------------------------------
   // Delegate methods for resolving injection points
   //-------------------------------------------------------------------------

   /**
    * Resolve the Artifact instance that uniquely matches the given object type, if any,
    * including its Artifact name.
    * <p>This is effectively a variant of {@link #getArtifact(Class)} which preserves the
    * Artifact name of the matching instance.
    * @param requiredType type the Artifact must match; can be an interface or superclass
    * @return the Artifact name plus Artifact instance
    * @throws NoSuchArtifactDefinitionException if no matching Artifact was found
    * @throws NoUniqueArtifactDefinitionException if more than one matching Artifact was found
    * @throws ArtifactsException if the Artifact could not be created
    * @since 4.3.3
    * @see #getArtifact(Class)
    */
   <T> NamedArtifactHolder<T> resolveNamedArtifact(Class<T> requiredType) throws ArtifactsException;

   /**
    * Resolve a Artifact instance for the given Artifact name, providing a dependency descriptor
    * for exposure to target factory methods.
    * <p>This is effectively a variant of {@link #getArtifact(String, Class)} which supports
    * factory methods with an {@link org.springframework.Artifacts.factory.InjectionPoint}
    * argument.
    * @param name the name of the Artifact to look up
    * @param descriptor the dependency descriptor for the requesting injection point
    * @return the corresponding Artifact instance
    * @throws NoSuchArtifactDefinitionException if there is no Artifact with the specified name
    * @throws ArtifactsException if the Artifact could not be created
    * @since 5.1.5
    * @see #getArtifact(String, Class)
    */
   Object resolveArtifactByName(String name, DependencyDescriptor descriptor) throws ArtifactsException;

   /**
    * Resolve the specified dependency against the Artifacts defined in this factory.
    * @param descriptor the descriptor for the dependency (field/method/constructor)
    * @param requestingArtifactName the name of the Artifact which declares the given dependency
    * @return the resolved object, or {@code null} if none found
    * @throws NoSuchArtifactDefinitionException if no matching Artifact was found
    * @throws NoUniqueArtifactDefinitionException if more than one matching Artifact was found
    * @throws ArtifactsException if dependency resolution failed for any other reason
    * @see #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)
    */
   @Nullable
   Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingArtifactName) throws ArtifactsException;

   /**
    * Resolve the specified dependency against the Artifacts defined in this factory.
    * @param descriptor the descriptor for the dependency (field/method/constructor)
    * @param requestingArtifactName the name of the Artifact which declares the given dependency
    * @param autowiredArtifactNames a Set that all names of autowired Artifacts (used for
    * resolving the given dependency) are supposed to be added to
    * @param typeConverter the TypeConverter to use for populating arrays and collections
    * @return the resolved object, or {@code null} if none found
    * @throws NoSuchArtifactDefinitionException if no matching Artifact was found
    * @throws NoUniqueArtifactDefinitionException if more than one matching Artifact was found
    * @throws ArtifactsException if dependency resolution failed for any other reason
    * @see DependencyDescriptor
    */
   @Nullable
   Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingArtifactName,
                            @Nullable Set<String> autowiredArtifactNames, @Nullable TypeConverter typeConverter) throws ArtifactsException;

}
