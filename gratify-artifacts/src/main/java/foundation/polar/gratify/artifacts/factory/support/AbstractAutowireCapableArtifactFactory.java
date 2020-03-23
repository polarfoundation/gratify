package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.*;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.*;
import foundation.polar.gratify.core.*;
import foundation.polar.gratify.utils.*;
import org.apache.commons.logging.Log;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Abstract artifact factory superclass that implements default artifact creation,
 * with the full capabilities specified by the {@link RootArtifactDefinition} class.
 * Implements the {@link foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory}
 * interface in addition to AbstractArtifactFactory's {@link #createArtifact} method.
 *
 * <p>Provides artifact creation (with constructor resolution), property population,
 * wiring (including autowiring), and initialization. Handles runtime artifact
 * references, resolves managed collections, calls initialization methods, etc.
 * Supports autowiring constructors, properties by name, and properties by type.
 *
 * <p>The main template method to be implemented by subclasses is
 * {@link #resolveDependency(DependencyDescriptor, String, Set, TypeConverter)},
 * used for autowiring by type. In case of a factory which is capable of searching
 * its artifact definitions, matching artifacts will typically be implemented through such
 * a search. For other factory styles, simplified matching algorithms can be implemented.
 *
 * <p>Note that this class does <i>not</i> assume or implement artifact definition
 * registry capabilities. See {@link DefaultListableArtifactFactory} for an implementation
 * of the {@link foundation.polar.gratify.artifacts.factory.ListableArtifactFactory} and
 * {@link ArtifactDefinitionRegistry} interfaces, which represent the API and SPI
 * view of such a factory, respectively.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Costin Leau
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 *
 * @see RootArtifactDefinition
 * @see DefaultListableArtifactFactory
 * @see ArtifactDefinitionRegistry
 */
public abstract class AbstractAutowireCapableArtifactFactory extends AbstractArtifactFactory
   implements AutowireCapableArtifactFactory {
   /** Strategy for creating artifact instances. */
   private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

   /** Resolver strategy for method parameter names. */
   @Nullable
   private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

   /** Whether to automatically try to resolve circular references between artifacts. */
   private boolean allowCircularReferences = true;

   /**
    * Whether to resort to injecting a raw artifact instance in case of circular reference,
    * even if the injected artifact eventually got wrapped.
    */
   private boolean allowRawInjectionDespiteWrapping = false;

   /**
    * Dependency types to ignore on dependency check and autowire, as Set of
    * Class objects: for example, String. Default is none.
    */
   private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

   /**
    * Dependency interfaces to ignore on dependency check and autowire, as Set of
    * Class objects. By default, only the ArtifactFactory interface is ignored.
    */
   private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

   /**
    * The name of the currently created artifact, for implicit dependency registration
    * on getArtifact etc invocations triggered from a user-specified Supplier callback.
    */
   private final NamedThreadLocal<String> currentlyCreatedArtifact = new NamedThreadLocal<>("Currently created artifact");

   /** Cache of unfinished FactoryArtifact instances: FactoryArtifact name to ArtifactWrapper. */
   private final ConcurrentMap<String, ArtifactWrapper> factoryArtifactInstanceCache = new ConcurrentHashMap<>();

   /** Cache of candidate factory methods per factory class. */
   private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();

   /** Cache of filtered PropertyDescriptors: artifact Class to PropertyDescriptor array. */
   private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache =
      new ConcurrentHashMap<>();

   /**
    * Create a new AbstractAutowireCapableArtifactFactory.
    */
   public AbstractAutowireCapableArtifactFactory() {
      super();
      ignoreDependencyInterface(ArtifactNameAware.class);
      ignoreDependencyInterface(ArtifactFactoryAware.class);
      ignoreDependencyInterface(ArtifactClassLoaderAware.class);
   }

   /**
    * Create a new AbstractAutowireCapableArtifactFactory with the given parent.
    * @param parentArtifactFactory parent artifact factory, or {@code null} if none
    */
   public AbstractAutowireCapableArtifactFactory(@Nullable ArtifactFactory parentArtifactFactory) {
      this();
      setParentArtifactFactory(parentArtifactFactory);
   }

   /**
    * Set the instantiation strategy to use for creating artifact instances.
    * Default is CglibSubclassingInstantiationStrategy.
    * @see CglibSubclassingInstantiationStrategy
    */
   public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
      this.instantiationStrategy = instantiationStrategy;
   }

   /**
    * Return the instantiation strategy to use for creating artifact instances.
    */
   protected InstantiationStrategy getInstantiationStrategy() {
      return this.instantiationStrategy;
   }

   /**
    * Set the ParameterNameDiscoverer to use for resolving method parameter
    * names if needed (e.g. for constructor names).
    * <p>Default is a {@link DefaultParameterNameDiscoverer}.
    */
   public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
      this.parameterNameDiscoverer = parameterNameDiscoverer;
   }

   /**
    * Return the ParameterNameDiscoverer to use for resolving method parameter
    * names if needed.
    */
   @Nullable
   protected ParameterNameDiscoverer getParameterNameDiscoverer() {
      return this.parameterNameDiscoverer;
   }

   /**
    * Set whether to allow circular references between artifacts - and automatically
    * try to resolve them.
    * <p>Note that circular reference resolution means that one of the involved artifacts
    * will receive a reference to another artifact that is not fully initialized yet.
    * This can lead to subtle and not-so-subtle side effects on initialization;
    * it does work fine for many scenarios, though.
    * <p>Default is "true". Turn this off to throw an exception when encountering
    * a circular reference, disallowing them completely.
    * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
    * between your artifacts. Refactor your application logic to have the two artifacts
    * involved delegate to a third artifact that encapsulates their common logic.
    */
   public void setAllowCircularReferences(boolean allowCircularReferences) {
      this.allowCircularReferences = allowCircularReferences;
   }

   /**
    * Set whether to allow the raw injection of a artifact instance into some other
    * artifact's property, despite the injected artifact eventually getting wrapped
    * (for example, through AOP auto-proxying).
    * <p>This will only be used as a last resort in case of a circular reference
    * that cannot be resolved otherwise: essentially, preferring a raw instance
    * getting injected over a failure of the entire artifact wiring process.
    * <p>Default is "false", as of Gratify 2.0. Turn this on to allow for non-wrapped
    * raw artifacts injected into some of your references, which was Gratify 1.2's
    * (arguably unclean) default behavior.
    * <p><b>NOTE:</b> It is generally recommended to not rely on circular references
    * between your artifacts, in particular with auto-proxying involved.
    * @see #setAllowCircularReferences
    */
   public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
      this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
   }

   /**
    * Ignore the given dependency type for autowiring:
    * for example, String. Default is none.
    */
   public void ignoreDependencyType(Class<?> type) {
      this.ignoredDependencyTypes.add(type);
   }

   /**
    * Ignore the given dependency interface for autowiring.
    * <p>This will typically be used by application contexts to register
    * dependencies that are resolved in other ways, like ArtifactFactory through
    * ArtifactFactoryAware or ApplicationContext through ApplicationContextAware.
    * <p>By default, only the ArtifactFactoryAware interface is ignored.
    * For further types to ignore, invoke this method for each type.
    * @see foundation.polar.gratify.artifacts.factory.ArtifactFactoryAware
    * @see foundation.polar.gratify.context.ApplicationContextAware
    */
   public void ignoreDependencyInterface(Class<?> ifc) {
      this.ignoredDependencyInterfaces.add(ifc);
   }

   @Override
   public void copyConfigurationFrom(ConfigurableArtifactFactory otherFactory) {
      super.copyConfigurationFrom(otherFactory);
      if (otherFactory instanceof AbstractAutowireCapableArtifactFactory) {
         AbstractAutowireCapableArtifactFactory otherAutowireFactory =
            (AbstractAutowireCapableArtifactFactory) otherFactory;
         this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
         this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
         this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
         this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
      }
   }

   //-------------------------------------------------------------------------
   // Typical methods for creating and populating external artifact instances
   //-------------------------------------------------------------------------

   @Override
   @SuppressWarnings("unchecked")
   public <T> T createArtifact(Class<T> artifactClass) throws ArtifactsException {
      // Use prototype artifact definition, to avoid registering artifact as dependent artifact.
      RootArtifactDefinition bd = new RootArtifactDefinition(artifactClass);
      bd.setScope(SCOPE_PROTOTYPE);
      bd.allowCaching = ClassUtils.isCacheSafe(artifactClass, getArtifactClassLoader());
      return (T) createArtifact(artifactClass.getName(), bd, null);
   }

   @Override
   public void autowireArtifact(Object existingArtifact) {
      // Use non-singleton artifact definition, to avoid registering artifact as dependent artifact.
      RootArtifactDefinition bd = new RootArtifactDefinition(ClassUtils.getUserClass(existingArtifact));
      bd.setScope(SCOPE_PROTOTYPE);
      bd.allowCaching = ClassUtils.isCacheSafe(bd.getArtifactClass(), getArtifactClassLoader());
      ArtifactWrapper bw = new ArtifactWrapperImpl(existingArtifact);
      initArtifactWrapper(bw);
      populateArtifact(bd.getArtifactClass().getName(), bd, bw);
   }

   @Override
   public Object configureArtifact(Object existingArtifact, String artifactName) throws ArtifactsException {
      markArtifactAsCreated(artifactName);
      ArtifactDefinition mbd = getMergedArtifactDefinition(artifactName);
      RootArtifactDefinition bd = null;
      if (mbd instanceof RootArtifactDefinition) {
         RootArtifactDefinition rbd = (RootArtifactDefinition) mbd;
         bd = (rbd.isPrototype() ? rbd : rbd.cloneArtifactDefinition());
      }
      if (bd == null) {
         bd = new RootArtifactDefinition(mbd);
      }
      if (!bd.isPrototype()) {
         bd.setScope(SCOPE_PROTOTYPE);
         bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingArtifact), getArtifactClassLoader());
      }
      ArtifactWrapper bw = new ArtifactWrapperImpl(existingArtifact);
      initArtifactWrapper(bw);
      populateArtifact(artifactName, bd, bw);
      return initializeArtifact(artifactName, existingArtifact, bd);
   }

   //-------------------------------------------------------------------------
   // Specialized methods for fine-grained control over the artifact lifecycle
   //-------------------------------------------------------------------------

   @Override
   public Object createArtifact(Class<?> artifactClass, int autowireMode, boolean dependencyCheck) throws ArtifactsException {
      // Use non-singleton artifact definition, to avoid registering artifact as dependent artifact.
      RootArtifactDefinition bd = new RootArtifactDefinition(artifactClass, autowireMode, dependencyCheck);
      bd.setScope(SCOPE_PROTOTYPE);
      return createArtifact(artifactClass.getName(), bd, null);
   }

   @Override
   public Object autowire(Class<?> artifactClass, int autowireMode, boolean dependencyCheck) throws ArtifactsException {
      // Use non-singleton artifact definition, to avoid registering artifact as dependent artifact.
      final RootArtifactDefinition bd = new RootArtifactDefinition(artifactClass, autowireMode, dependencyCheck);
      bd.setScope(SCOPE_PROTOTYPE);
      if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
         return autowireConstructor(artifactClass.getName(), bd, null, null).getWrappedInstance();
      }
      else {
         Object artifact;
         final ArtifactFactory parent = this;
         if (System.getSecurityManager() != null) {
            artifact = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                  getInstantiationStrategy().instantiate(bd, null, parent),
               getAccessControlContext());
         }
         else {
            artifact = getInstantiationStrategy().instantiate(bd, null, parent);
         }
         populateArtifact(artifactClass.getName(), bd, new ArtifactWrapperImpl(artifact));
         return artifact;
      }
   }

   @Override
   public void autowireArtifactProperties(Object existingArtifact, int autowireMode, boolean dependencyCheck)
      throws ArtifactsException {

      if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
         throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing artifact instance");
      }
      // Use non-singleton artifact definition, to avoid registering artifact as dependent artifact.
      RootArtifactDefinition bd =
         new RootArtifactDefinition(ClassUtils.getUserClass(existingArtifact), autowireMode, dependencyCheck);
      bd.setScope(SCOPE_PROTOTYPE);
      ArtifactWrapper bw = new ArtifactWrapperImpl(existingArtifact);
      initArtifactWrapper(bw);
      populateArtifact(bd.getArtifactClass().getName(), bd, bw);
   }

   @Override
   public void applyArtifactPropertyValues(Object existingArtifact, String artifactName) throws ArtifactsException {
      markArtifactAsCreated(artifactName);
      ArtifactDefinition bd = getMergedArtifactDefinition(artifactName);
      ArtifactWrapper bw = new ArtifactWrapperImpl(existingArtifact);
      initArtifactWrapper(bw);
      applyPropertyValues(artifactName, bd, bw, bd.getPropertyValues());
   }

   @Override
   public Object initializeArtifact(Object existingArtifact, String artifactName) {
      return initializeArtifact(artifactName, existingArtifact, null);
   }

   @Override
   public Object applyArtifactPostProcessorsBeforeInitialization(Object existingArtifact, String artifactName)
      throws ArtifactsException {

      Object result = existingArtifact;
      for (ArtifactPostProcessor processor : getArtifactPostProcessors()) {
         Object current = processor.postProcessBeforeInitialization(result, artifactName);
         if (current == null) {
            return result;
         }
         result = current;
      }
      return result;
   }

   @Override
   public Object applyArtifactPostProcessorsAfterInitialization(Object existingArtifact, String artifactName)
      throws ArtifactsException {

      Object result = existingArtifact;
      for (ArtifactPostProcessor processor : getArtifactPostProcessors()) {
         Object current = processor.postProcessAfterInitialization(result, artifactName);
         if (current == null) {
            return result;
         }
         result = current;
      }
      return result;
   }

   @Override
   public void destroyArtifact(Object existingArtifact) {
      new DisposableArtifactAdapter(existingArtifact, getArtifactPostProcessors(), getAccessControlContext()).destroy();
   }

   //-------------------------------------------------------------------------
   // Delegate methods for resolving injection points
   //-------------------------------------------------------------------------

   @Override
   public Object resolveArtifactByName(String name, DependencyDescriptor descriptor) {
      InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
      try {
         return getArtifact(name, descriptor.getDependencyType());
      }
      finally {
         ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
      }
   }

   @Override
   @Nullable
   public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingArtifactName) throws ArtifactsException {
      return resolveDependency(descriptor, requestingArtifactName, null, null);
   }

   //---------------------------------------------------------------------
   // Implementation of relevant AbstractArtifactFactory template methods
   //---------------------------------------------------------------------

   /**
    * Central method of this class: creates a artifact instance,
    * populates the artifact instance, applies post-processors, etc.
    * @see #doCreateArtifact
    */
   @Override
   protected Object createArtifact(String artifactName, RootArtifactDefinition mbd, @Nullable Object[] args)
      throws ArtifactCreationException {

      if (logger.isTraceEnabled()) {
         logger.trace("Creating instance of artifact '" + artifactName + "'");
      }
      RootArtifactDefinition mbdToUse = mbd;

      // Make sure artifact class is actually resolved at this point, and
      // clone the artifact definition in case of a dynamically resolved Class
      // which cannot be stored in the shared merged artifact definition.
      Class<?> resolvedClass = resolveArtifactClass(mbd, artifactName);
      if (resolvedClass != null && !mbd.hasArtifactClass() && mbd.getArtifactClassName() != null) {
         mbdToUse = new RootArtifactDefinition(mbd);
         mbdToUse.setArtifactClass(resolvedClass);
      }

      // Prepare method overrides.
      try {
         mbdToUse.prepareMethodOverrides();
      }
      catch (ArtifactDefinitionValidationException ex) {
         throw new ArtifactDefinitionStoreException(mbdToUse.getResourceDescription(),
            artifactName, "Validation of method overrides failed", ex);
      }

      try {
         // Give ArtifactPostProcessors a chance to return a proxy instead of the target artifact instance.
         Object artifact = resolveBeforeInstantiation(artifactName, mbdToUse);
         if (artifact != null) {
            return artifact;
         }
      }
      catch (Throwable ex) {
         throw new ArtifactCreationException(mbdToUse.getResourceDescription(), artifactName,
            "ArtifactPostProcessor before instantiation of artifact failed", ex);
      }

      try {
         Object artifactInstance = doCreateArtifact(artifactName, mbdToUse, args);
         if (logger.isTraceEnabled()) {
            logger.trace("Finished creating instance of artifact '" + artifactName + "'");
         }
         return artifactInstance;
      }
      catch (ArtifactCreationException | ImplicitlyAppearedSingletonException ex) {
         // A previously detected exception with proper artifact creation context already,
         // or illegal singleton state to be communicated up to DefaultSingletonArtifactRegistry.
         throw ex;
      }
      catch (Throwable ex) {
         throw new ArtifactCreationException(
            mbdToUse.getResourceDescription(), artifactName, "Unexpected exception during artifact creation", ex);
      }
   }

   /**
    * Actually create the specified artifact. Pre-creation processing has already happened
    * at this point, e.g. checking {@code postProcessBeforeInstantiation} callbacks.
    * <p>Differentiates between default artifact instantiation, use of a
    * factory method, and autowiring a constructor.
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition for the artifact
    * @param args explicit arguments to use for constructor or factory method invocation
    * @return a new instance of the artifact
    * @throws ArtifactCreationException if the artifact could not be created
    * @see #instantiateArtifact
    * @see #instantiateUsingFactoryMethod
    * @see #autowireConstructor
    */
   protected Object doCreateArtifact(final String artifactName, final RootArtifactDefinition mbd, final @Nullable Object[] args)
      throws ArtifactCreationException {

      // Instantiate the artifact.
      ArtifactWrapper instanceWrapper = null;
      if (mbd.isSingleton()) {
         instanceWrapper = this.factoryArtifactInstanceCache.remove(artifactName);
      }
      if (instanceWrapper == null) {
         instanceWrapper = createArtifactInstance(artifactName, mbd, args);
      }
      final Object artifact = instanceWrapper.getWrappedInstance();
      Class<?> artifactType = instanceWrapper.getWrappedClass();
      if (artifactType != NullArtifact.class) {
         mbd.resolvedTargetType = artifactType;
      }

      // Allow post-processors to modify the merged artifact definition.
      synchronized (mbd.postProcessingLock) {
         if (!mbd.postProcessed) {
            try {
               applyMergedArtifactDefinitionPostProcessors(mbd, artifactType, artifactName);
            }
            catch (Throwable ex) {
               throw new ArtifactCreationException(mbd.getResourceDescription(), artifactName,
                  "Post-processing of merged artifact definition failed", ex);
            }
            mbd.postProcessed = true;
         }
      }

      // Eagerly cache singletons to be able to resolve circular references
      // even when triggered by lifecycle interfaces like ArtifactFactoryAware.
      boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(artifactName));
      if (earlySingletonExposure) {
         if (logger.isTraceEnabled()) {
            logger.trace("Eagerly caching artifact '" + artifactName +
               "' to allow for resolving potential circular references");
         }
         addSingletonFactory(artifactName, () -> getEarlyArtifactReference(artifactName, mbd, artifact));
      }

      // Initialize the artifact instance.
      Object exposedObject = artifact;
      try {
         populateArtifact(artifactName, mbd, instanceWrapper);
         exposedObject = initializeArtifact(artifactName, exposedObject, mbd);
      }
      catch (Throwable ex) {
         if (ex instanceof ArtifactCreationException && artifactName.equals(((ArtifactCreationException) ex).getArtifactName())) {
            throw (ArtifactCreationException) ex;
         }
         else {
            throw new ArtifactCreationException(
               mbd.getResourceDescription(), artifactName, "Initialization of artifact failed", ex);
         }
      }

      if (earlySingletonExposure) {
         Object earlySingletonReference = getSingleton(artifactName, false);
         if (earlySingletonReference != null) {
            if (exposedObject == artifact) {
               exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentArtifact(artifactName)) {
               String[] dependentArtifacts = getDependentArtifacts(artifactName);
               Set<String> actualDependentArtifacts = new LinkedHashSet<>(dependentArtifacts.length);
               for (String dependentArtifact : dependentArtifacts) {
                  if (!removeSingletonIfCreatedForTypeCheckOnly(dependentArtifact)) {
                     actualDependentArtifacts.add(dependentArtifact);
                  }
               }
               if (!actualDependentArtifacts.isEmpty()) {
                  throw new ArtifactCurrentlyInCreationException(artifactName,
                     "Artifact with name '" + artifactName + "' has been injected into other artifacts [" +
                        StringUtils.collectionToCommaDelimitedString(actualDependentArtifacts) +
                        "] in its raw version as part of a circular reference, but has eventually been " +
                        "wrapped. This means that said other artifacts do not use the final version of the " +
                        "artifact. This is often the result of over-eager type matching - consider using " +
                        "'getArtifactNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
               }
            }
         }
      }

      // Register artifact as disposable.
      try {
         registerDisposableArtifactIfNecessary(artifactName, artifact, mbd);
      }
      catch (ArtifactDefinitionValidationException ex) {
         throw new ArtifactCreationException(
            mbd.getResourceDescription(), artifactName, "Invalid destruction signature", ex);
      }

      return exposedObject;
   }

   @Override
   @Nullable
   protected Class<?> predictArtifactType(String artifactName, RootArtifactDefinition mbd, Class<?>... typesToMatch) {
      Class<?> targetType = determineTargetType(artifactName, mbd, typesToMatch);
      // Apply SmartInstantiationAwareArtifactPostProcessors to predict the
      // eventual type after a before-instantiation shortcut.
      if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareArtifactPostProcessors()) {
         boolean matchingOnlyFactoryArtifact = typesToMatch.length == 1 && typesToMatch[0] == FactoryArtifact.class;
         for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareArtifactPostProcessor) {
               SmartInstantiationAwareArtifactPostProcessor ibp = (SmartInstantiationAwareArtifactPostProcessor) bp;
               Class<?> predicted = ibp.predictArtifactType(targetType, artifactName);
               if (predicted != null &&
                  (!matchingOnlyFactoryArtifact || FactoryArtifact.class.isAssignableFrom(predicted))) {
                  return predicted;
               }
            }
         }
      }
      return targetType;
   }

   /**
    * Determine the target type for the given artifact definition.
    * @param artifactName the name of the artifact (for error handling purposes)
    * @param mbd the merged artifact definition for the artifact
    * @param typesToMatch the types to match in case of internal type matching purposes
    * (also signals that the returned {@code Class} will never be exposed to application code)
    * @return the type for the artifact if determinable, or {@code null} otherwise
    */
   @Nullable
   protected Class<?> determineTargetType(String artifactName, RootArtifactDefinition mbd, Class<?>... typesToMatch) {
      Class<?> targetType = mbd.getTargetType();
      if (targetType == null) {
         targetType = (mbd.getFactoryMethodName() != null ?
            getTypeForFactoryMethod(artifactName, mbd, typesToMatch) :
            resolveArtifactClass(mbd, artifactName, typesToMatch));
         if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
            mbd.resolvedTargetType = targetType;
         }
      }
      return targetType;
   }

   /**
    * Determine the target type for the given artifact definition which is based on
    * a factory method. Only called if there is no singleton instance registered
    * for the target artifact already.
    * <p>This implementation determines the type matching {@link #createArtifact}'s
    * different creation strategies. As far as possible, we'll perform static
    * type checking to avoid creation of the target artifact.
    * @param artifactName the name of the artifact (for error handling purposes)
    * @param mbd the merged artifact definition for the artifact
    * @param typesToMatch the types to match in case of internal type matching purposes
    * (also signals that the returned {@code Class} will never be exposed to application code)
    * @return the type for the artifact if determinable, or {@code null} otherwise
    * @see #createArtifact
    */
   @Nullable
   protected Class<?> getTypeForFactoryMethod(String artifactName, RootArtifactDefinition mbd, Class<?>... typesToMatch) {
      ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
      if (cachedReturnType != null) {
         return cachedReturnType.resolve();
      }

      Class<?> commonType = null;
      Method uniqueCandidate = mbd.factoryMethodToIntrospect;

      if (uniqueCandidate == null) {
         Class<?> factoryClass;
         boolean isStatic = true;

         String factoryArtifactName = mbd.getFactoryArtifactName();
         if (factoryArtifactName != null) {
            if (factoryArtifactName.equals(artifactName)) {
               throw new ArtifactDefinitionStoreException(mbd.getResourceDescription(), artifactName,
                  "factory-artifact reference points back to the same artifact definition");
            }
            // Check declared factory method return type on factory class.
            factoryClass = getType(factoryArtifactName);
            isStatic = false;
         }
         else {
            // Check declared factory method return type on artifact class.
            factoryClass = resolveArtifactClass(mbd, artifactName, typesToMatch);
         }

         if (factoryClass == null) {
            return null;
         }
         factoryClass = ClassUtils.getUserClass(factoryClass);

         // If all factory methods have the same return type, return that type.
         // Can't clearly figure out exact method due to type converting / autowiring!
         int minNrOfArgs =
            (mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
         Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
            clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));

         for (Method candidate : candidates) {
            if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
               candidate.getParameterCount() >= minNrOfArgs) {
               // Declared type variables to inspect?
               if (candidate.getTypeParameters().length > 0) {
                  try {
                     // Fully resolve parameter names and argument values.
                     Class<?>[] paramTypes = candidate.getParameterTypes();
                     String[] paramNames = null;
                     ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                     if (pnd != null) {
                        paramNames = pnd.getParameterNames(candidate);
                     }
                     ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
                     Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
                     Object[] args = new Object[paramTypes.length];
                     for (int i = 0; i < args.length; i++) {
                        ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
                           i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
                        if (valueHolder == null) {
                           valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                        }
                        if (valueHolder != null) {
                           args[i] = valueHolder.getValue();
                           usedValueHolders.add(valueHolder);
                        }
                     }
                     Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
                        candidate, args, getArtifactClassLoader());
                     uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ?
                        candidate : null);
                     commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
                     if (commonType == null) {
                        // Ambiguous return types found: return null to indicate "not determinable".
                        return null;
                     }
                  }
                  catch (Throwable ex) {
                     if (logger.isDebugEnabled()) {
                        logger.debug("Failed to resolve generic return type for factory method: " + ex);
                     }
                  }
               }
               else {
                  uniqueCandidate = (commonType == null ? candidate : null);
                  commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
                  if (commonType == null) {
                     // Ambiguous return types found: return null to indicate "not determinable".
                     return null;
                  }
               }
            }
         }

         mbd.factoryMethodToIntrospect = uniqueCandidate;
         if (commonType == null) {
            return null;
         }
      }

      // Common return type found: all factory methods return same type. For a non-parameterized
      // unique candidate, cache the full type declaration context of the target factory method.
      cachedReturnType = (uniqueCandidate != null ?
         ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
      mbd.factoryMethodReturnType = cachedReturnType;
      return cachedReturnType.resolve();
   }

   /**
    * This implementation attempts to query the FactoryArtifact's generic parameter metadata
    * if present to determine the object type. If not present, i.e. the FactoryArtifact is
    * declared as a raw type, checks the FactoryArtifact's {@code getObjectType} method
    * on a plain instance of the FactoryArtifact, without artifact properties applied yet.
    * If this doesn't return a type yet, and {@code allowInit} is {@code true} a
    * full creation of the FactoryArtifact is used as fallback (through delegation to the
    * superclass's implementation).
    * <p>The shortcut check for a FactoryArtifact is only applied in case of a singleton
    * FactoryArtifact. If the FactoryArtifact instance itself is not kept as singleton,
    * it will be fully created to check the type of its exposed object.
    */
   @Override
   protected ResolvableType getTypeForFactoryArtifact(String artifactName, RootArtifactDefinition mbd, boolean allowInit) {
      // Check if the artifact definition itself has defined the type with an attribute
      ResolvableType result = getTypeForFactoryArtifactFromAttributes(mbd);
      if (result != ResolvableType.NONE) {
         return result;
      }

      ResolvableType artifactType =
         (mbd.hasArtifactClass() ? ResolvableType.forClass(mbd.getArtifactClass()) : ResolvableType.NONE);

      // For instance supplied artifacts try the target type and artifact class
      if (mbd.getInstanceSupplier() != null) {
         result = getFactoryArtifactGeneric(mbd.targetType);
         if (result.resolve() != null) {
            return result;
         }
         result = getFactoryArtifactGeneric(artifactType);
         if (result.resolve() != null) {
            return result;
         }
      }

      // Consider factory methods
      String factoryArtifactName = mbd.getFactoryArtifactName();
      String factoryMethodName = mbd.getFactoryMethodName();

      // Scan the factory artifact methods
      if (factoryArtifactName != null) {
         if (factoryMethodName != null) {
            // Try to obtain the FactoryArtifact's object type from its factory method
            // declaration without instantiating the containing artifact at all.
            ArtifactDefinition factoryArtifactDefinition = getArtifactDefinition(factoryArtifactName);
            Class<?> factoryArtifactClass;
            if (factoryArtifactDefinition instanceof AbstractArtifactDefinition &&
               ((AbstractArtifactDefinition) factoryArtifactDefinition).hasArtifactClass()) {
               factoryArtifactClass = ((AbstractArtifactDefinition) factoryArtifactDefinition).getArtifactClass();
            }
            else {
               RootArtifactDefinition fbmbd = getMergedArtifactDefinition(factoryArtifactName, factoryArtifactDefinition);
               factoryArtifactClass = determineTargetType(factoryArtifactName, fbmbd);
            }
            if (factoryArtifactClass != null) {
               result = getTypeForFactoryArtifactFromMethod(factoryArtifactClass, factoryMethodName);
               if (result.resolve() != null) {
                  return result;
               }
            }
         }
         // If not resolvable above and the referenced factory artifact doesn't exist yet,
         // exit here - we don't want to force the creation of another artifact just to
         // obtain a FactoryArtifact's object type...
         if (!isArtifactEligibleForMetadataCaching(factoryArtifactName)) {
            return ResolvableType.NONE;
         }
      }

      // If we're allowed, we can create the factory artifact and call getObjectType() early
      if (allowInit) {
         FactoryArtifact<?> factoryArtifact = (mbd.isSingleton() ?
            getSingletonFactoryArtifactForTypeCheck(artifactName, mbd) :
            getNonSingletonFactoryArtifactForTypeCheck(artifactName, mbd));
         if (factoryArtifact != null) {
            // Try to obtain the FactoryArtifact's object type from this early stage of the instance.
            Class<?> type = getTypeForFactoryArtifact(factoryArtifact);
            if (type != null) {
               return ResolvableType.forClass(type);
            }
            // No type found for shortcut FactoryArtifact instance:
            // fall back to full creation of the FactoryArtifact instance.
            return super.getTypeForFactoryArtifact(artifactName, mbd, true);
         }
      }

      if (factoryArtifactName == null && mbd.hasArtifactClass() && factoryMethodName != null) {
         // No early artifact instantiation possible: determine FactoryArtifact's type from
         // static factory method signature or from class inheritance hierarchy...
         return getTypeForFactoryArtifactFromMethod(mbd.getArtifactClass(), factoryMethodName);
      }
      result = getFactoryArtifactGeneric(artifactType);
      if (result.resolve() != null) {
         return result;
      }
      return ResolvableType.NONE;
   }

   private ResolvableType getFactoryArtifactGeneric(@Nullable ResolvableType type) {
      if (type == null) {
         return ResolvableType.NONE;
      }
      return type.as(FactoryArtifact.class).getGeneric();
   }

   /**
    * Introspect the factory method signatures on the given artifact class,
    * trying to find a common {@code FactoryArtifact} object type declared there.
    * @param artifactClass the artifact class to find the factory method on
    * @param factoryMethodName the name of the factory method
    * @return the common {@code FactoryArtifact} object type, or {@code null} if none
    */
   private ResolvableType getTypeForFactoryArtifactFromMethod(Class<?> artifactClass, String factoryMethodName) {
      // CGLIB subclass methods hide generic parameters; look at the original user class.
      Class<?> factoryArtifactClass = ClassUtils.getUserClass(artifactClass);
      FactoryArtifactMethodTypeFinder finder = new FactoryArtifactMethodTypeFinder(factoryMethodName);
      ReflectionUtils.doWithMethods(factoryArtifactClass, finder, ReflectionUtils.USER_DECLARED_METHODS);
      return finder.getResult();
   }

   /**
    * This implementation attempts to query the FactoryArtifact's generic parameter metadata
    * if present to determine the object type. If not present, i.e. the FactoryArtifact is
    * declared as a raw type, checks the FactoryArtifact's {@code getObjectType} method
    * on a plain instance of the FactoryArtifact, without artifact properties applied yet.
    * If this doesn't return a type yet, a full creation of the FactoryArtifact is
    * used as fallback (through delegation to the superclass's implementation).
    * <p>The shortcut check for a FactoryArtifact is only applied in case of a singleton
    * FactoryArtifact. If the FactoryArtifact instance itself is not kept as singleton,
    * it will be fully created to check the type of its exposed object.
    */
   @Override
   @Deprecated
   @Nullable
   protected Class<?> getTypeForFactoryArtifact(String artifactName, RootArtifactDefinition mbd) {
      return getTypeForFactoryArtifact(artifactName, mbd, true).resolve();
   }

   /**
    * Obtain a reference for early access to the specified artifact,
    * typically for the purpose of resolving a circular reference.
    * @param artifactName the name of the artifact (for error handling purposes)
    * @param mbd the merged artifact definition for the artifact
    * @param artifact the raw artifact instance
    * @return the object to expose as artifact reference
    */
   protected Object getEarlyArtifactReference(String artifactName, RootArtifactDefinition mbd, Object artifact) {
      Object exposedObject = artifact;
      if (!mbd.isSynthetic() && hasInstantiationAwareArtifactPostProcessors()) {
         for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareArtifactPostProcessor) {
               SmartInstantiationAwareArtifactPostProcessor ibp = (SmartInstantiationAwareArtifactPostProcessor) bp;
               exposedObject = ibp.getEarlyArtifactReference(exposedObject, artifactName);
            }
         }
      }
      return exposedObject;
   }


   //---------------------------------------------------------------------
   // Implementation methods
   //---------------------------------------------------------------------

   /**
    * Obtain a "shortcut" singleton FactoryArtifact instance to use for a
    * {@code getObjectType()} call, without full initialization of the FactoryArtifact.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @return the FactoryArtifact instance, or {@code null} to indicate
    * that we couldn't obtain a shortcut FactoryArtifact instance
    */
   @Nullable
   private FactoryArtifact<?> getSingletonFactoryArtifactForTypeCheck(String artifactName, RootArtifactDefinition mbd) {
      synchronized (getSingletonMutex()) {
         ArtifactWrapper bw = this.factoryArtifactInstanceCache.get(artifactName);
         if (bw != null) {
            return (FactoryArtifact<?>) bw.getWrappedInstance();
         }
         Object artifactInstance = getSingleton(artifactName, false);
         if (artifactInstance instanceof FactoryArtifact) {
            return (FactoryArtifact<?>) artifactInstance;
         }
         if (isSingletonCurrentlyInCreation(artifactName) ||
            (mbd.getFactoryArtifactName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryArtifactName()))) {
            return null;
         }

         Object instance;
         try {
            // Mark this artifact as currently in creation, even if just partially.
            beforeSingletonCreation(artifactName);
            // Give ArtifactPostProcessors a chance to return a proxy instead of the target artifact instance.
            instance = resolveBeforeInstantiation(artifactName, mbd);
            if (instance == null) {
               bw = createArtifactInstance(artifactName, mbd, null);
               instance = bw.getWrappedInstance();
            }
         }
         catch (UnsatisfiedDependencyException ex) {
            // Don't swallow, probably misconfiguration...
            throw ex;
         }
         catch (ArtifactCreationException ex) {
            // Instantiation failure, maybe too early...
            if (logger.isDebugEnabled()) {
               logger.debug("Artifact creation exception on singleton FactoryArtifact type check: " + ex);
            }
            onSuppressedException(ex);
            return null;
         }
         finally {
            // Finished partial creation of this artifact.
            afterSingletonCreation(artifactName);
         }

         FactoryArtifact<?> fb = getFactoryArtifact(artifactName, instance);
         if (bw != null) {
            this.factoryArtifactInstanceCache.put(artifactName, bw);
         }
         return fb;
      }
   }

   /**
    * Obtain a "shortcut" non-singleton FactoryArtifact instance to use for a
    * {@code getObjectType()} call, without full initialization of the FactoryArtifact.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @return the FactoryArtifact instance, or {@code null} to indicate
    * that we couldn't obtain a shortcut FactoryArtifact instance
    */
   @Nullable
   private FactoryArtifact<?> getNonSingletonFactoryArtifactForTypeCheck(String artifactName, RootArtifactDefinition mbd) {
      if (isPrototypeCurrentlyInCreation(artifactName)) {
         return null;
      }

      Object instance;
      try {
         // Mark this artifact as currently in creation, even if just partially.
         beforePrototypeCreation(artifactName);
         // Give ArtifactPostProcessors a chance to return a proxy instead of the target artifact instance.
         instance = resolveBeforeInstantiation(artifactName, mbd);
         if (instance == null) {
            ArtifactWrapper bw = createArtifactInstance(artifactName, mbd, null);
            instance = bw.getWrappedInstance();
         }
      }
      catch (UnsatisfiedDependencyException ex) {
         // Don't swallow, probably misconfiguration...
         throw ex;
      }
      catch (ArtifactCreationException ex) {
         // Instantiation failure, maybe too early...
         if (logger.isDebugEnabled()) {
            logger.debug("Artifact creation exception on non-singleton FactoryArtifact type check: " + ex);
         }
         onSuppressedException(ex);
         return null;
      }
      finally {
         // Finished partial creation of this artifact.
         afterPrototypeCreation(artifactName);
      }

      return getFactoryArtifact(artifactName, instance);
   }

   /**
    * Apply MergedArtifactDefinitionPostProcessors to the specified artifact definition,
    * invoking their {@code postProcessMergedArtifactDefinition} methods.
    * @param mbd the merged artifact definition for the artifact
    * @param artifactType the actual type of the managed artifact instance
    * @param artifactName the name of the artifact
    * @see MergedArtifactDefinitionPostProcessor#postProcessMergedArtifactDefinition
    */
   protected void applyMergedArtifactDefinitionPostProcessors(RootArtifactDefinition mbd, Class<?> artifactType, String artifactName) {
      for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
         if (bp instanceof MergedArtifactDefinitionPostProcessor) {
            MergedArtifactDefinitionPostProcessor bdp = (MergedArtifactDefinitionPostProcessor) bp;
            bdp.postProcessMergedArtifactDefinition(mbd, artifactType, artifactName);
         }
      }
   }

   /**
    * Apply before-instantiation post-processors, resolving whether there is a
    * before-instantiation shortcut for the specified artifact.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @return the shortcut-determined artifact instance, or {@code null} if none
    */
   @Nullable
   protected Object resolveBeforeInstantiation(String artifactName, RootArtifactDefinition mbd) {
      Object artifact = null;
      if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
         // Make sure artifact class is actually resolved at this point.
         if (!mbd.isSynthetic() && hasInstantiationAwareArtifactPostProcessors()) {
            Class<?> targetType = determineTargetType(artifactName, mbd);
            if (targetType != null) {
               artifact = applyArtifactPostProcessorsBeforeInstantiation(targetType, artifactName);
               if (artifact != null) {
                  artifact = applyArtifactPostProcessorsAfterInitialization(artifact, artifactName);
               }
            }
         }
         mbd.beforeInstantiationResolved = (artifact != null);
      }
      return artifact;
   }

   /**
    * Apply InstantiationAwareArtifactPostProcessors to the specified artifact definition
    * (by class and name), invoking their {@code postProcessBeforeInstantiation} methods.
    * <p>Any returned object will be used as the artifact instead of actually instantiating
    * the target artifact. A {@code null} return value from the post-processor will
    * result in the target artifact being instantiated.
    * @param artifactClass the class of the artifact to be instantiated
    * @param artifactName the name of the artifact
    * @return the artifact object to use instead of a default instance of the target artifact, or {@code null}
    * @see InstantiationAwareArtifactPostProcessor#postProcessBeforeInstantiation
    */
   @Nullable
   protected Object applyArtifactPostProcessorsBeforeInstantiation(Class<?> artifactClass, String artifactName) {
      for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
         if (bp instanceof InstantiationAwareArtifactPostProcessor) {
            InstantiationAwareArtifactPostProcessor ibp = (InstantiationAwareArtifactPostProcessor) bp;
            Object result = ibp.postProcessBeforeInstantiation(artifactClass, artifactName);
            if (result != null) {
               return result;
            }
         }
      }
      return null;
   }

   /**
    * Create a new instance for the specified artifact, using an appropriate instantiation strategy:
    * factory method, constructor autowiring, or simple instantiation.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @param args explicit arguments to use for constructor or factory method invocation
    * @return a ArtifactWrapper for the new instance
    * @see #obtainFromSupplier
    * @see #instantiateUsingFactoryMethod
    * @see #autowireConstructor
    * @see #instantiateArtifact
    */
   protected ArtifactWrapper createArtifactInstance(String artifactName, RootArtifactDefinition mbd, @Nullable Object[] args) {
      // Make sure artifact class is actually resolved at this point.
      Class<?> artifactClass = resolveArtifactClass(mbd, artifactName);

      if (artifactClass != null && !Modifier.isPublic(artifactClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
         throw new ArtifactCreationException(mbd.getResourceDescription(), artifactName,
            "Artifact class isn't public, and non-public access not allowed: " + artifactClass.getName());
      }

      Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
      if (instanceSupplier != null) {
         return obtainFromSupplier(instanceSupplier, artifactName);
      }

      if (mbd.getFactoryMethodName() != null) {
         return instantiateUsingFactoryMethod(artifactName, mbd, args);
      }

      // Shortcut when re-creating the same artifact...
      boolean resolved = false;
      boolean autowireNecessary = false;
      if (args == null) {
         synchronized (mbd.constructorArgumentLock) {
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
               resolved = true;
               autowireNecessary = mbd.constructorArgumentsResolved;
            }
         }
      }
      if (resolved) {
         if (autowireNecessary) {
            return autowireConstructor(artifactName, mbd, null, null);
         }
         else {
            return instantiateArtifact(artifactName, mbd);
         }
      }

      // Candidate constructors for autowiring?
      Constructor<?>[] ctors = determineConstructorsFromArtifactPostProcessors(artifactClass, artifactName);
      if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
         mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
         return autowireConstructor(artifactName, mbd, ctors, args);
      }

      // Preferred constructors for default construction?
      ctors = mbd.getPreferredConstructors();
      if (ctors != null) {
         return autowireConstructor(artifactName, mbd, ctors, null);
      }

      // No special handling: simply use no-arg constructor.
      return instantiateArtifact(artifactName, mbd);
   }

   /**
    * Obtain a artifact instance from the given supplier.
    * @param instanceSupplier the configured supplier
    * @param artifactName the corresponding artifact name
    * @return a ArtifactWrapper for the new instance
    * @see #getObjectForArtifactInstance
    */
   protected ArtifactWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String artifactName) {
      Object instance;

      String outerArtifact = this.currentlyCreatedArtifact.get();
      this.currentlyCreatedArtifact.set(artifactName);
      try {
         instance = instanceSupplier.get();
      }
      finally {
         if (outerArtifact != null) {
            this.currentlyCreatedArtifact.set(outerArtifact);
         }
         else {
            this.currentlyCreatedArtifact.remove();
         }
      }

      if (instance == null) {
         instance = new NullArtifact();
      }
      ArtifactWrapper bw = new ArtifactWrapperImpl(instance);
      initArtifactWrapper(bw);
      return bw;
   }

   /**
    * Overridden in order to implicitly register the currently created artifact as
    * dependent on further artifacts getting programmatically retrieved during a
    * {@link Supplier} callback.
    * @see #obtainFromSupplier
    */
   @Override
   protected Object getObjectForArtifactInstance(
      Object artifactInstance, String name, String artifactName, @Nullable RootArtifactDefinition mbd) {

      String currentlyCreatedArtifact = this.currentlyCreatedArtifact.get();
      if (currentlyCreatedArtifact != null) {
         registerDependentArtifact(artifactName, currentlyCreatedArtifact);
      }

      return super.getObjectForArtifactInstance(artifactInstance, name, artifactName, mbd);
   }

   /**
    * Determine candidate constructors to use for the given artifact, checking all registered
    * {@link SmartInstantiationAwareArtifactPostProcessor SmartInstantiationAwareArtifactPostProcessors}.
    * @param artifactClass the raw class of the artifact
    * @param artifactName the name of the artifact
    * @return the candidate constructors, or {@code null} if none specified
    * @throws foundation.polar.gratify.artifacts.ArtifactsException in case of errors
    * @see foundation.polar.gratify.artifacts.factory.config.SmartInstantiationAwareArtifactPostProcessor#determineCandidateConstructors
    */
   @Nullable
   protected Constructor<?>[] determineConstructorsFromArtifactPostProcessors(@Nullable Class<?> artifactClass, String artifactName)
      throws ArtifactsException {

      if (artifactClass != null && hasInstantiationAwareArtifactPostProcessors()) {
         for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareArtifactPostProcessor) {
               SmartInstantiationAwareArtifactPostProcessor ibp = (SmartInstantiationAwareArtifactPostProcessor) bp;
               Constructor<?>[] ctors = ibp.determineCandidateConstructors(artifactClass, artifactName);
               if (ctors != null) {
                  return ctors;
               }
            }
         }
      }
      return null;
   }

   /**
    * Instantiate the given artifact using its default constructor.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @return a ArtifactWrapper for the new instance
    */
   protected ArtifactWrapper instantiateArtifact(final String artifactName, final RootArtifactDefinition mbd) {
      try {
         Object artifactInstance;
         final ArtifactFactory parent = this;
         if (System.getSecurityManager() != null) {
            artifactInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                  getInstantiationStrategy().instantiate(mbd, artifactName, parent),
               getAccessControlContext());
         }
         else {
            artifactInstance = getInstantiationStrategy().instantiate(mbd, artifactName, parent);
         }
         ArtifactWrapper bw = new ArtifactWrapperImpl(artifactInstance);
         initArtifactWrapper(bw);
         return bw;
      }
      catch (Throwable ex) {
         throw new ArtifactCreationException(
            mbd.getResourceDescription(), artifactName, "Instantiation of artifact failed", ex);
      }
   }

   /**
    * Instantiate the artifact using a named factory method. The method may be static, if the
    * mbd parameter specifies a class, rather than a factoryArtifact, or an instance variable
    * on a factory object itself configured using Dependency Injection.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @param explicitArgs argument values passed in programmatically via the getArtifact method,
    * or {@code null} if none (-> use constructor argument values from artifact definition)
    * @return a ArtifactWrapper for the new instance
    * @see #getArtifact(String, Object[])
    */
   protected ArtifactWrapper instantiateUsingFactoryMethod(
      String artifactName, RootArtifactDefinition mbd, @Nullable Object[] explicitArgs) {

      return new ConstructorResolver(this).instantiateUsingFactoryMethod(artifactName, mbd, explicitArgs);
   }

   /**
    * "autowire constructor" (with constructor arguments by type) behavior.
    * Also applied if explicit constructor argument values are specified,
    * matching all remaining arguments with artifacts from the artifact factory.
    * <p>This corresponds to constructor injection: In this mode, a Gratify
    * artifact factory is able to host components that expect constructor-based
    * dependency resolution.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @param ctors the chosen candidate constructors
    * @param explicitArgs argument values passed in programmatically via the getArtifact method,
    * or {@code null} if none (-> use constructor argument values from artifact definition)
    * @return a ArtifactWrapper for the new instance
    */
   protected ArtifactWrapper autowireConstructor(
      String artifactName, RootArtifactDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

      return new ConstructorResolver(this).autowireConstructor(artifactName, mbd, ctors, explicitArgs);
   }

   /**
    * Populate the artifact instance in the given ArtifactWrapper with the property values
    * from the artifact definition.
    * @param artifactName the name of the artifact
    * @param mbd the artifact definition for the artifact
    * @param bw the ArtifactWrapper with artifact instance
    */
   @SuppressWarnings("deprecation")  // for postProcessPropertyValues
   protected void populateArtifact(String artifactName, RootArtifactDefinition mbd, @Nullable ArtifactWrapper bw) {
      if (bw == null) {
         if (mbd.hasPropertyValues()) {
            throw new ArtifactCreationException(
               mbd.getResourceDescription(), artifactName, "Cannot apply property values to null instance");
         }
         else {
            // Skip property population phase for null instance.
            return;
         }
      }

      // Give any InstantiationAwareArtifactPostProcessors the opportunity to modify the
      // state of the artifact before properties are set. This can be used, for example,
      // to support styles of field injection.
      if (!mbd.isSynthetic() && hasInstantiationAwareArtifactPostProcessors()) {
         for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
            if (bp instanceof InstantiationAwareArtifactPostProcessor) {
               InstantiationAwareArtifactPostProcessor ibp = (InstantiationAwareArtifactPostProcessor) bp;
               if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), artifactName)) {
                  return;
               }
            }
         }
      }

      PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

      int resolvedAutowireMode = mbd.getResolvedAutowireMode();
      if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
         MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
         // Add property values based on autowire by name if applicable.
         if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
            autowireByName(artifactName, mbd, bw, newPvs);
         }
         // Add property values based on autowire by type if applicable.
         if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            autowireByType(artifactName, mbd, bw, newPvs);
         }
         pvs = newPvs;
      }

      boolean hasInstAwareBpps = hasInstantiationAwareArtifactPostProcessors();
      boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractArtifactDefinition.DEPENDENCY_CHECK_NONE);

      PropertyDescriptor[] filteredPds = null;
      if (hasInstAwareBpps) {
         if (pvs == null) {
            pvs = mbd.getPropertyValues();
         }
         for (ArtifactPostProcessor bp : getArtifactPostProcessors()) {
            if (bp instanceof InstantiationAwareArtifactPostProcessor) {
               InstantiationAwareArtifactPostProcessor ibp = (InstantiationAwareArtifactPostProcessor) bp;
               PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), artifactName);
               if (pvsToUse == null) {
                  if (filteredPds == null) {
                     filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                  }
                  pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), artifactName);
                  if (pvsToUse == null) {
                     return;
                  }
               }
               pvs = pvsToUse;
            }
         }
      }
      if (needsDepCheck) {
         if (filteredPds == null) {
            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
         }
         checkDependencies(artifactName, mbd, filteredPds, pvs);
      }

      if (pvs != null) {
         applyPropertyValues(artifactName, mbd, bw, pvs);
      }
   }

   /**
    * Fill in any missing property values with references to
    * other artifacts in this factory if autowire is set to "byName".
    * @param artifactName the name of the artifact we're wiring up.
    * Useful for debugging messages; not used functionally.
    * @param mbd artifact definition to update through autowiring
    * @param bw the ArtifactWrapper from which we can obtain information about the artifact
    * @param pvs the PropertyValues to register wired objects with
    */
   protected void autowireByName(
      String artifactName, AbstractArtifactDefinition mbd, ArtifactWrapper bw, MutablePropertyValues pvs) {

      String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
      for (String propertyName : propertyNames) {
         if (containsArtifact(propertyName)) {
            Object artifact = getArtifact(propertyName);
            pvs.add(propertyName, artifact);
            registerDependentArtifact(propertyName, artifactName);
            if (logger.isTraceEnabled()) {
               logger.trace("Added autowiring by name from artifact name '" + artifactName +
                  "' via property '" + propertyName + "' to artifact named '" + propertyName + "'");
            }
         }
         else {
            if (logger.isTraceEnabled()) {
               logger.trace("Not autowiring property '" + propertyName + "' of artifact '" + artifactName +
                  "' by name: no matching artifact found");
            }
         }
      }
   }

   /**
    * Abstract method defining "autowire by type" (artifact properties by type) behavior.
    * <p>This is like PicoContainer default, in which there must be exactly one artifact
    * of the property type in the artifact factory. This makes artifact factories simple to
    * configure for small namespaces, but doesn't work as well as standard Gratify
    * behavior for bigger applications.
    * @param artifactName the name of the artifact to autowire by type
    * @param mbd the merged artifact definition to update through autowiring
    * @param bw the ArtifactWrapper from which we can obtain information about the artifact
    * @param pvs the PropertyValues to register wired objects with
    */
   protected void autowireByType(
      String artifactName, AbstractArtifactDefinition mbd, ArtifactWrapper bw, MutablePropertyValues pvs) {

      TypeConverter converter = getCustomTypeConverter();
      if (converter == null) {
         converter = bw;
      }

      Set<String> autowiredArtifactNames = new LinkedHashSet<>(4);
      String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
      for (String propertyName : propertyNames) {
         try {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            // Don't try autowiring by type for type Object: never makes sense,
            // even if it technically is a unsatisfied, non-simple property.
            if (Object.class != pd.getPropertyType()) {
               MethodParameter methodParam = ArtifactUtils.getWriteMethodParameter(pd);
               // Do not allow eager init for type matching in case of a prioritized post-processor.
               boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
               DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
               Object autowiredArgument = resolveDependency(desc, artifactName, autowiredArtifactNames, converter);
               if (autowiredArgument != null) {
                  pvs.add(propertyName, autowiredArgument);
               }
               for (String autowiredArtifactName : autowiredArtifactNames) {
                  registerDependentArtifact(autowiredArtifactName, artifactName);
                  if (logger.isTraceEnabled()) {
                     logger.trace("Autowiring by type from artifact name '" + artifactName + "' via property '" +
                        propertyName + "' to artifact named '" + autowiredArtifactName + "'");
                  }
               }
               autowiredArtifactNames.clear();
            }
         }
         catch (ArtifactsException ex) {
            throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), artifactName, propertyName, ex);
         }
      }
   }


   /**
    * Return an array of non-simple artifact properties that are unsatisfied.
    * These are probably unsatisfied references to other artifacts in the
    * factory. Does not include simple properties like primitives or Strings.
    * @param mbd the merged artifact definition the artifact was created with
    * @param bw the ArtifactWrapper the artifact was created with
    * @return an array of artifact property names
    * @see foundation.polar.gratify.artifacts.ArtifactUtils#isSimpleProperty
    */
   protected String[] unsatisfiedNonSimpleProperties(AbstractArtifactDefinition mbd, ArtifactWrapper bw) {
      Set<String> result = new TreeSet<>();
      PropertyValues pvs = mbd.getPropertyValues();
      PropertyDescriptor[] pds = bw.getPropertyDescriptors();
      for (PropertyDescriptor pd : pds) {
         if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
            !ArtifactUtils.isSimpleProperty(pd.getPropertyType())) {
            result.add(pd.getName());
         }
      }
      return StringUtils.toStringArray(result);
   }

   /**
    * Extract a filtered set of PropertyDescriptors from the given ArtifactWrapper,
    * excluding ignored dependency types or properties defined on ignored dependency interfaces.
    * @param bw the ArtifactWrapper the artifact was created with
    * @param cache whether to cache filtered PropertyDescriptors for the given artifact Class
    * @return the filtered PropertyDescriptors
    * @see #isExcludedFromDependencyCheck
    * @see #filterPropertyDescriptorsForDependencyCheck(foundation.polar.gratify.artifacts.ArtifactWrapper)
    */
   protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(ArtifactWrapper bw, boolean cache) {
      PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
      if (filtered == null) {
         filtered = filterPropertyDescriptorsForDependencyCheck(bw);
         if (cache) {
            PropertyDescriptor[] existing =
               this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
            if (existing != null) {
               filtered = existing;
            }
         }
      }
      return filtered;
   }

   /**
    * Extract a filtered set of PropertyDescriptors from the given ArtifactWrapper,
    * excluding ignored dependency types or properties defined on ignored dependency interfaces.
    * @param bw the ArtifactWrapper the artifact was created with
    * @return the filtered PropertyDescriptors
    * @see #isExcludedFromDependencyCheck
    */
   protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(ArtifactWrapper bw) {
      List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
      pds.removeIf(this::isExcludedFromDependencyCheck);
      return pds.toArray(new PropertyDescriptor[0]);
   }

   /**
    * Determine whether the given artifact property is excluded from dependency checks.
    * <p>This implementation excludes properties defined by CGLIB and
    * properties whose type matches an ignored dependency type or which
    * are defined by an ignored dependency interface.
    * @param pd the PropertyDescriptor of the artifact property
    * @return whether the artifact property is excluded
    * @see #ignoreDependencyType(Class)
    * @see #ignoreDependencyInterface(Class)
    */
   protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
      return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
         this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
         AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
   }

   /**
    * Perform a dependency check that all properties exposed have been set,
    * if desired. Dependency checks can be objects (collaborating artifacts),
    * simple (primitives and String), or all (both).
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition the artifact was created with
    * @param pds the relevant property descriptors for the target artifact
    * @param pvs the property values to be applied to the artifact
    * @see #isExcludedFromDependencyCheck(java.artifacts.PropertyDescriptor)
    */
   protected void checkDependencies(
      String artifactName, AbstractArtifactDefinition mbd, PropertyDescriptor[] pds, @Nullable PropertyValues pvs)
      throws UnsatisfiedDependencyException {

      int dependencyCheck = mbd.getDependencyCheck();
      for (PropertyDescriptor pd : pds) {
         if (pd.getWriteMethod() != null && (pvs == null || !pvs.contains(pd.getName()))) {
            boolean isSimple = ArtifactUtils.isSimpleProperty(pd.getPropertyType());
            boolean unsatisfied = (dependencyCheck == AbstractArtifactDefinition.DEPENDENCY_CHECK_ALL) ||
               (isSimple && dependencyCheck == AbstractArtifactDefinition.DEPENDENCY_CHECK_SIMPLE) ||
               (!isSimple && dependencyCheck == AbstractArtifactDefinition.DEPENDENCY_CHECK_OBJECTS);
            if (unsatisfied) {
               throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), artifactName, pd.getName(),
                  "Set this property value or disable dependency checking for this artifact.");
            }
         }
      }
   }

   /**
    * Apply the given property values, resolving any runtime references
    * to other artifacts in this artifact factory. Must use deep copy, so we
    * don't permanently modify this property.
    * @param artifactName the artifact name passed for better exception information
    * @param mbd the merged artifact definition
    * @param bw the ArtifactWrapper wrapping the target object
    * @param pvs the new property values
    */
   protected void applyPropertyValues(String artifactName, ArtifactDefinition mbd, ArtifactWrapper bw, PropertyValues pvs) {
      if (pvs.isEmpty()) {
         return;
      }

      if (System.getSecurityManager() != null && bw instanceof ArtifactWrapperImpl) {
         ((ArtifactWrapperImpl) bw).setSecurityContext(getAccessControlContext());
      }

      MutablePropertyValues mpvs = null;
      List<PropertyValue> original;

      if (pvs instanceof MutablePropertyValues) {
         mpvs = (MutablePropertyValues) pvs;
         if (mpvs.isConverted()) {
            // Shortcut: use the pre-converted values as-is.
            try {
               bw.setPropertyValues(mpvs);
               return;
            }
            catch (ArtifactsException ex) {
               throw new ArtifactCreationException(
                  mbd.getResourceDescription(), artifactName, "Error setting property values", ex);
            }
         }
         original = mpvs.getPropertyValueList();
      }
      else {
         original = Arrays.asList(pvs.getPropertyValues());
      }

      TypeConverter converter = getCustomTypeConverter();
      if (converter == null) {
         converter = bw;
      }
      ArtifactDefinitionValueResolver valueResolver = new ArtifactDefinitionValueResolver(this, artifactName, mbd, converter);

      // Create a deep copy, resolving any references for values.
      List<PropertyValue> deepCopy = new ArrayList<>(original.size());
      boolean resolveNecessary = false;
      for (PropertyValue pv : original) {
         if (pv.isConverted()) {
            deepCopy.add(pv);
         }
         else {
            String propertyName = pv.getName();
            Object originalValue = pv.getValue();
            if (originalValue == AutowiredPropertyMarker.INSTANCE) {
               Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
               if (writeMethod == null) {
                  throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
               }
               originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
            }
            Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
            Object convertedValue = resolvedValue;
            boolean convertible = bw.isWritableProperty(propertyName) &&
               !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
            if (convertible) {
               convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
            }
            // Possibly store converted value in merged artifact definition,
            // in order to avoid re-conversion for every created artifact instance.
            if (resolvedValue == originalValue) {
               if (convertible) {
                  pv.setConvertedValue(convertedValue);
               }
               deepCopy.add(pv);
            }
            else if (convertible && originalValue instanceof TypedStringValue &&
               !((TypedStringValue) originalValue).isDynamic() &&
               !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
               pv.setConvertedValue(convertedValue);
               deepCopy.add(pv);
            }
            else {
               resolveNecessary = true;
               deepCopy.add(new PropertyValue(pv, convertedValue));
            }
         }
      }
      if (mpvs != null && !resolveNecessary) {
         mpvs.setConverted();
      }

      // Set our (possibly massaged) deep copy.
      try {
         bw.setPropertyValues(new MutablePropertyValues(deepCopy));
      }
      catch (ArtifactsException ex) {
         throw new ArtifactCreationException(
            mbd.getResourceDescription(), artifactName, "Error setting property values", ex);
      }
   }

   /**
    * Convert the given value for the specified target property.
    */
   @Nullable
   private Object convertForProperty(
      @Nullable Object value, String propertyName, ArtifactWrapper bw, TypeConverter converter) {

      if (converter instanceof ArtifactWrapperImpl) {
         return ((ArtifactWrapperImpl) converter).convertForProperty(value, propertyName);
      }
      else {
         PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
         MethodParameter methodParam = ArtifactUtils.getWriteMethodParameter(pd);
         return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
      }
   }

   /**
    * Initialize the given artifact instance, applying factory callbacks
    * as well as init methods and artifact post processors.
    * <p>Called from {@link #createArtifact} for traditionally defined artifacts,
    * and from {@link #initializeArtifact} for existing artifact instances.
    * @param artifactName the artifact name in the factory (for debugging purposes)
    * @param artifact the new artifact instance we may need to initialize
    * @param mbd the artifact definition that the artifact was created with
    * (can also be {@code null}, if given an existing artifact instance)
    * @return the initialized artifact instance (potentially wrapped)
    * @see ArtifactNameAware
    * @see ArtifactClassLoaderAware
    * @see ArtifactFactoryAware
    * @see #applyArtifactPostProcessorsBeforeInitialization
    * @see #invokeInitMethods
    * @see #applyArtifactPostProcessorsAfterInitialization
    */
   protected Object initializeArtifact(final String artifactName, final Object artifact, @Nullable RootArtifactDefinition mbd) {
      if (System.getSecurityManager() != null) {
         AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            invokeAwareMethods(artifactName, artifact);
            return null;
         }, getAccessControlContext());
      }
      else {
         invokeAwareMethods(artifactName, artifact);
      }

      Object wrappedArtifact = artifact;
      if (mbd == null || !mbd.isSynthetic()) {
         wrappedArtifact = applyArtifactPostProcessorsBeforeInitialization(wrappedArtifact, artifactName);
      }

      try {
         invokeInitMethods(artifactName, wrappedArtifact, mbd);
      }
      catch (Throwable ex) {
         throw new ArtifactCreationException(
            (mbd != null ? mbd.getResourceDescription() : null),
            artifactName, "Invocation of init method failed", ex);
      }
      if (mbd == null || !mbd.isSynthetic()) {
         wrappedArtifact = applyArtifactPostProcessorsAfterInitialization(wrappedArtifact, artifactName);
      }

      return wrappedArtifact;
   }

   private void invokeAwareMethods(final String artifactName, final Object artifact) {
      if (artifact instanceof Aware) {
         if (artifact instanceof ArtifactNameAware) {
            ((ArtifactNameAware) artifact).setArtifactName(artifactName);
         }
         if (artifact instanceof ArtifactClassLoaderAware) {
            ClassLoader bcl = getArtifactClassLoader();
            if (bcl != null) {
               ((ArtifactClassLoaderAware) artifact).setArtifactClassLoader(bcl);
            }
         }
         if (artifact instanceof ArtifactFactoryAware) {
            ((ArtifactFactoryAware) artifact).setArtifactFactory(AbstractAutowireCapableArtifactFactory.this);
         }
      }
   }

   /**
    * Give a artifact a chance to react now all its properties are set,
    * and a chance to know about its owning artifact factory (this object).
    * This means checking whether the artifact implements InitializingArtifact or defines
    * a custom init method, and invoking the necessary callback(s) if it does.
    * @param artifactName the artifact name in the factory (for debugging purposes)
    * @param artifact the new artifact instance we may need to initialize
    * @param mbd the merged artifact definition that the artifact was created with
    * (can also be {@code null}, if given an existing artifact instance)
    * @throws Throwable if thrown by init methods or by the invocation process
    * @see #invokeCustomInitMethod
    */
   protected void invokeInitMethods(String artifactName, final Object artifact, @Nullable RootArtifactDefinition mbd)
      throws Throwable {

      boolean isInitializingArtifact = (artifact instanceof InitializingArtifact);
      if (isInitializingArtifact && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
         if (logger.isTraceEnabled()) {
            logger.trace("Invoking afterPropertiesSet() on artifact with name '" + artifactName + "'");
         }
         if (System.getSecurityManager() != null) {
            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                  ((InitializingArtifact) artifact).afterPropertiesSet();
                  return null;
               }, getAccessControlContext());
            }
            catch (PrivilegedActionException pae) {
               throw pae.getException();
            }
         }
         else {
            ((InitializingArtifact) artifact).afterPropertiesSet();
         }
      }

      if (mbd != null && artifact.getClass() != NullArtifact.class) {
         String initMethodName = mbd.getInitMethodName();
         if (StringUtils.hasLength(initMethodName) &&
            !(isInitializingArtifact && "afterPropertiesSet".equals(initMethodName)) &&
            !mbd.isExternallyManagedInitMethod(initMethodName)) {
            invokeCustomInitMethod(artifactName, artifact, mbd);
         }
      }
   }

   /**
    * Invoke the specified custom init method on the given artifact.
    * Called by invokeInitMethods.
    * <p>Can be overridden in subclasses for custom resolution of init
    * methods with arguments.
    * @see #invokeInitMethods
    */
   protected void invokeCustomInitMethod(String artifactName, final Object artifact, RootArtifactDefinition mbd)
      throws Throwable {

      String initMethodName = mbd.getInitMethodName();
      AssertUtils.state(initMethodName != null, "No init method set");
      Method initMethod = (mbd.isNonPublicAccessAllowed() ?
         ArtifactUtils.findMethod(artifact.getClass(), initMethodName) :
         ClassUtils.getMethodIfAvailable(artifact.getClass(), initMethodName));

      if (initMethod == null) {
         if (mbd.isEnforceInitMethod()) {
            throw new ArtifactDefinitionValidationException("Could not find an init method named '" +
               initMethodName + "' on artifact with name '" + artifactName + "'");
         }
         else {
            if (logger.isTraceEnabled()) {
               logger.trace("No default init method named '" + initMethodName +
                  "' found on artifact with name '" + artifactName + "'");
            }
            // Ignore non-existent default lifecycle methods.
            return;
         }
      }

      if (logger.isTraceEnabled()) {
         logger.trace("Invoking init method  '" + initMethodName + "' on artifact with name '" + artifactName + "'");
      }
      Method methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(initMethod);

      if (System.getSecurityManager() != null) {
         AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            ReflectionUtils.makeAccessible(methodToInvoke);
            return null;
         });
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
               methodToInvoke.invoke(artifact), getAccessControlContext());
         }
         catch (PrivilegedActionException pae) {
            InvocationTargetException ex = (InvocationTargetException) pae.getException();
            throw ex.getTargetException();
         }
      }
      else {
         try {
            ReflectionUtils.makeAccessible(methodToInvoke);
            methodToInvoke.invoke(artifact);
         }
         catch (InvocationTargetException ex) {
            throw ex.getTargetException();
         }
      }
   }

   /**
    * Applies the {@code postProcessAfterInitialization} callback of all
    * registered ArtifactPostProcessors, giving them a chance to post-process the
    * object obtained from FactoryArtifacts (for example, to auto-proxy them).
    * @see #applyArtifactPostProcessorsAfterInitialization
    */
   @Override
   protected Object postProcessObjectFromFactoryArtifact(Object object, String artifactName) {
      return applyArtifactPostProcessorsAfterInitialization(object, artifactName);
   }

   /**
    * Overridden to clear FactoryArtifact instance cache as well.
    */
   @Override
   protected void removeSingleton(String artifactName) {
      synchronized (getSingletonMutex()) {
         super.removeSingleton(artifactName);
         this.factoryArtifactInstanceCache.remove(artifactName);
      }
   }

   /**
    * Overridden to clear FactoryArtifact instance cache as well.
    */
   @Override
   protected void clearSingletonCache() {
      synchronized (getSingletonMutex()) {
         super.clearSingletonCache();
         this.factoryArtifactInstanceCache.clear();
      }
   }

   /**
    * Expose the logger to collaborating delegates.
    */
   Log getLogger() {
      return logger;
   }

   /**
    * Special DependencyDescriptor variant for Gratify's good old autowire="byType" mode.
    * Always optional; never considering the parameter name for choosing a primary candidate.
    */
   @SuppressWarnings("serial")
   private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

      public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
         super(methodParameter, false, eager);
      }

      @Override
      public String getDependencyName() {
         return null;
      }
   }

   /**
    * {@link MethodCallback} used to find {@link FactoryArtifact} type information.
    */
   private static class FactoryArtifactMethodTypeFinder implements ReflectionUtils.MethodCallback {

      private final String factoryMethodName;

      private ResolvableType result = ResolvableType.NONE;

      FactoryArtifactMethodTypeFinder(String factoryMethodName) {
         this.factoryMethodName = factoryMethodName;
      }

      @Override
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
         if (isFactoryArtifactMethod(method)) {
            ResolvableType returnType = ResolvableType.forMethodReturnType(method);
            ResolvableType candidate = returnType.as(FactoryArtifact.class).getGeneric();
            if (this.result == ResolvableType.NONE) {
               this.result = candidate;
            }
            else {
               Class<?> resolvedResult = this.result.resolve();
               Class<?> commonAncestor = ClassUtils.determineCommonAncestor(candidate.resolve(), resolvedResult);
               if (!ObjectUtils.nullSafeEquals(resolvedResult, commonAncestor)) {
                  this.result = ResolvableType.forClass(commonAncestor);
               }
            }
         }
      }

      private boolean isFactoryArtifactMethod(Method method) {
         return (method.getName().equals(this.factoryMethodName) &&
            FactoryArtifact.class.isAssignableFrom(method.getReturnType()));
      }

      ResolvableType getResult() {
         Class<?> resolved = this.result.resolve();
         boolean foundResult = resolved != null && resolved != Object.class;
         return (foundResult ? this.result : ResolvableType.NONE);
      }
   }
}
