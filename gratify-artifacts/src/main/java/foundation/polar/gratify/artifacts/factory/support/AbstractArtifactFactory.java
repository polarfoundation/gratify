package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.*;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.*;
import foundation.polar.gratify.core.AttributeAccessor;
import foundation.polar.gratify.core.DecoratingClassLoader;
import foundation.polar.gratify.core.NamedThreadLocal;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.utils.*;
import foundation.polar.gratify.utils.logging.LogMessage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditor;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class for {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}
 * implementations, providing the full capabilities of the
 * {@link foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory} SPI.
 * Does <i>not</i> assume a listable artifact factory: can therefore also be used
 * as base class for artifact factory implementations which obtain artifact definitions
 * from some backend resource (where artifact definition access is an expensive operation).
 *
 * <p>This class provides a singleton cache (through its base class
 * {@link foundation.polar.gratify.artifacts.factory.support.DefaultSingletonArtifactRegistry},
 * singleton/prototype determination, {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact}
 * handling, aliases, artifact definition merging for child artifact definitions,
 * and artifact destruction ({@link foundation.polar.gratify.artifacts.factory.DisposableArtifact}
 * interface, custom destroy methods). Furthermore, it can manage a artifact factory
 * hierarchy (delegating to the parent in case of an unknown artifact), through implementing
 * the {@link foundation.polar.gratify.artifacts.factory.HierarchicalArtifactFactory} interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * {@link #getArtifactDefinition} and {@link #createArtifact}, retrieving a artifact definition
 * for a given artifact name and creating a artifact instance for a given artifact definition,
 * respectively. Default implementations of those operations can be found in
 * {@link DefaultListableArtifactFactory} and {@link AbstractAutowireCapableArtifactFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @see #getArtifactDefinition
 * @see #createArtifact
 * @see AbstractAutowireCapableArtifactFactory#createArtifact
 * @see DefaultListableArtifactFactory#getArtifactDefinition
 */
public abstract class AbstractArtifactFactory
   extends FactoryArtifactRegistrySupport implements ConfigurableArtifactFactory {
   /** Parent artifact factory, for artifact inheritance support. */
   @Nullable
   private ArtifactFactory parentArtifactFactory;

   /** ClassLoader to resolve artifact class names with, if necessary. */
   @Nullable
   private ClassLoader artifactClassLoader = ClassUtils.getDefaultClassLoader();

   /** ClassLoader to temporarily resolve artifact class names with, if necessary. */
   @Nullable
   private ClassLoader tempClassLoader;

   /** Whether to cache artifact metadata or rather reobtain it for every access. */
   private boolean cacheArtifactMetadata = true;

   /** Resolution strategy for expressions in artifact definition values. */
   @Nullable
   private ArtifactExpressionResolver artifactExpressionResolver;

   /** Gratify ConversionService to use instead of PropertyEditors. */
   @Nullable
   private ConversionService conversionService;

   /** Custom PropertyEditorRegistrars to apply to the artifacts of this factory. */
   private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

   /** Custom PropertyEditors to apply to the artifacts of this factory. */
   private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

   /** A custom TypeConverter to use, overriding the default PropertyEditor mechanism. */
   @Nullable
   private TypeConverter typeConverter;

   /** String resolvers to apply e.g. to annotation attribute values. */
   private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

   /** ArtifactPostProcessors to apply in createArtifact. */
   private final List<ArtifactPostProcessor> artifactPostProcessors = new CopyOnWriteArrayList<>();

   /** Indicates whether any InstantiationAwareArtifactPostProcessors have been registered. */
   private volatile boolean hasInstantiationAwareArtifactPostProcessors;

   /** Indicates whether any DestructionAwareArtifactPostProcessors have been registered. */
   private volatile boolean hasDestructionAwareArtifactPostProcessors;

   /** Map from scope identifier String to corresponding Scope. */
   private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

   /** Security context used when running with a SecurityManager. */
   @Nullable
   private SecurityContextProvider securityContextProvider;

   /** Map from artifact name to merged RootArtifactDefinition. */
   private final Map<String, RootArtifactDefinition> mergedArtifactDefinitions = new ConcurrentHashMap<>(256);

   /** Names of artifacts that have already been created at least once. */
   private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

   /** Names of artifacts that are currently in creation. */
   private final ThreadLocal<Object> prototypesCurrentlyInCreation =
      new NamedThreadLocal<>("Prototype artifacts currently in creation");

   /**
    * Create a new AbstractArtifactFactory.
    */
   public AbstractArtifactFactory() {
   }

   /**
    * Create a new AbstractArtifactFactory with the given parent.
    * @param parentArtifactFactory parent artifact factory, or {@code null} if none
    * @see #getArtifact
    */
   public AbstractArtifactFactory(@Nullable ArtifactFactory parentArtifactFactory) {
      this.parentArtifactFactory = parentArtifactFactory;
   }

   //---------------------------------------------------------------------
   // Implementation of ArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public Object getArtifact(String name) throws ArtifactsException {
      return doGetArtifact(name, null, null, false);
   }

   @Override
   public <T> T getArtifact(String name, Class<T> requiredType) throws ArtifactsException {
      return doGetArtifact(name, requiredType, null, false);
   }

   @Override
   public Object getArtifact(String name, Object... args) throws ArtifactsException {
      return doGetArtifact(name, null, args, false);
   }

   /**
    * Return an instance, which may be shared or independent, of the specified artifact.
    * @param name the name of the artifact to retrieve
    * @param requiredType the required type of the artifact to retrieve
    * @param args arguments to use when creating a artifact instance using explicit arguments
    * (only applied when creating a new instance as opposed to retrieving an existing one)
    * @return an instance of the artifact
    * @throws ArtifactsException if the artifact could not be created
    */
   public <T> T getArtifact(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
      throws ArtifactsException {

      return doGetArtifact(name, requiredType, args, false);
   }

   /**
    * Return an instance, which may be shared or independent, of the specified artifact.
    * @param name the name of the artifact to retrieve
    * @param requiredType the required type of the artifact to retrieve
    * @param args arguments to use when creating a artifact instance using explicit arguments
    * (only applied when creating a new instance as opposed to retrieving an existing one)
    * @param typeCheckOnly whether the instance is obtained for a type check,
    * not for actual use
    * @return an instance of the artifact
    * @throws ArtifactsException if the artifact could not be created
    */
   @SuppressWarnings("unchecked")
   protected <T> T doGetArtifact(final String name, @Nullable final Class<T> requiredType,
                             @Nullable final Object[] args, boolean typeCheckOnly) throws ArtifactsException {

      final String artifactName = transformedArtifactName(name);
      Object artifact;

      // Eagerly check singleton cache for manually registered singletons.
      Object sharedInstance = getSingleton(artifactName);
      if (sharedInstance != null && args == null) {
         if (logger.isTraceEnabled()) {
            if (isSingletonCurrentlyInCreation(artifactName)) {
               logger.trace("Returning eagerly cached instance of singleton artifact '" + artifactName +
                  "' that is not fully initialized yet - a consequence of a circular reference");
            }
            else {
               logger.trace("Returning cached instance of singleton artifact '" + artifactName + "'");
            }
         }
         artifact = getObjectForArtifactInstance(sharedInstance, name, artifactName, null);
      }

      else {
         // Fail if we're already creating this artifact instance:
         // We're assumably within a circular reference.
         if (isPrototypeCurrentlyInCreation(artifactName)) {
            throw new ArtifactCurrentlyInCreationException(artifactName);
         }

         // Check if artifact definition exists in this factory.
         ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
         if (parentArtifactFactory != null && !containsArtifactDefinition(artifactName)) {
            // Not found -> check parent.
            String nameToLookup = originalArtifactName(name);
            if (parentArtifactFactory instanceof AbstractArtifactFactory) {
               return ((AbstractArtifactFactory) parentArtifactFactory).doGetArtifact(
                  nameToLookup, requiredType, args, typeCheckOnly);
            }
            else if (args != null) {
               // Delegation to parent with explicit args.
               return (T) parentArtifactFactory.getArtifact(nameToLookup, args);
            }
            else if (requiredType != null) {
               // No args -> delegate to standard getArtifact method.
               return parentArtifactFactory.getArtifact(nameToLookup, requiredType);
            }
            else {
               return (T) parentArtifactFactory.getArtifact(nameToLookup);
            }
         }

         if (!typeCheckOnly) {
            markArtifactAsCreated(artifactName);
         }

         try {
            final RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
            checkMergedArtifactDefinition(mbd, artifactName, args);

            // Guarantee initialization of artifacts that the current artifact depends on.
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
               for (String dep : dependsOn) {
                  if (isDependent(artifactName, dep)) {
                     throw new ArtifactCreationException(mbd.getResourceDescription(), artifactName,
                        "Circular depends-on relationship between '" + artifactName + "' and '" + dep + "'");
                  }
                  registerDependentArtifact(dep, artifactName);
                  try {
                     getArtifact(dep);
                  }
                  catch (NoSuchArtifactDefinitionException ex) {
                     throw new ArtifactCreationException(mbd.getResourceDescription(), artifactName,
                        "'" + artifactName + "' depends on missing artifact '" + dep + "'", ex);
                  }
               }
            }

            // Create artifact instance.
            if (mbd.isSingleton()) {
               sharedInstance = getSingleton(artifactName, () -> {
                  try {
                     return createArtifact(artifactName, mbd, args);
                  }
                  catch (ArtifactsException ex) {
                     // Explicitly remove instance from singleton cache: It might have been put there
                     // eagerly by the creation process, to allow for circular reference resolution.
                     // Also remove any artifacts that received a temporary reference to the artifact.
                     destroySingleton(artifactName);
                     throw ex;
                  }
               });
               artifact = getObjectForArtifactInstance(sharedInstance, name, artifactName, mbd);
            }

            else if (mbd.isPrototype()) {
               // It's a prototype -> create a new instance.
               Object prototypeInstance = null;
               try {
                  beforePrototypeCreation(artifactName);
                  prototypeInstance = createArtifact(artifactName, mbd, args);
               }
               finally {
                  afterPrototypeCreation(artifactName);
               }
               artifact = getObjectForArtifactInstance(prototypeInstance, name, artifactName, mbd);
            }

            else {
               String scopeName = mbd.getScope();
               final Scope scope = this.scopes.get(scopeName);
               if (scope == null) {
                  throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
               }
               try {
                  Object scopedInstance = scope.get(artifactName, () -> {
                     beforePrototypeCreation(artifactName);
                     try {
                        return createArtifact(artifactName, mbd, args);
                     }
                     finally {
                        afterPrototypeCreation(artifactName);
                     }
                  });
                  artifact = getObjectForArtifactInstance(scopedInstance, name, artifactName, mbd);
               }
               catch (IllegalStateException ex) {
                  throw new ArtifactCreationException(artifactName,
                     "Scope '" + scopeName + "' is not active for the current thread; consider " +
                        "defining a scoped proxy for this artifact if you intend to refer to it from a singleton",
                     ex);
               }
            }
         }
         catch (ArtifactsException ex) {
            cleanupAfterArtifactCreationFailure(artifactName);
            throw ex;
         }
      }

      // Check if required type matches the type of the actual artifact instance.
      if (requiredType != null && !requiredType.isInstance(artifact)) {
         try {
            T convertedArtifact = getTypeConverter().convertIfNecessary(artifact, requiredType);
            if (convertedArtifact == null) {
               throw new ArtifactNotOfRequiredTypeException(name, requiredType, artifact.getClass());
            }
            return convertedArtifact;
         }
         catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
               logger.trace("Failed to convert artifact '" + name + "' to required type '" +
                  ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new ArtifactNotOfRequiredTypeException(name, requiredType, artifact.getClass());
         }
      }
      return (T) artifact;
   }

   @Override
   public boolean containsArtifact(String name) {
      String artifactName = transformedArtifactName(name);
      if (containsSingleton(artifactName) || containsArtifactDefinition(artifactName)) {
         return (!ArtifactFactoryUtils.isFactoryDereference(name) || isFactoryArtifact(name));
      }
      // Not found -> check parent.
      ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
      return (parentArtifactFactory != null && parentArtifactFactory.containsArtifact(originalArtifactName(name)));
   }

   @Override
   public boolean isSingleton(String name) throws NoSuchArtifactDefinitionException {
      String artifactName = transformedArtifactName(name);

      Object artifactInstance = getSingleton(artifactName, false);
      if (artifactInstance != null) {
         if (artifactInstance instanceof FactoryArtifact) {
            return (ArtifactFactoryUtils.isFactoryDereference(name) || ((FactoryArtifact<?>) artifactInstance).isSingleton());
         }
         else {
            return !ArtifactFactoryUtils.isFactoryDereference(name);
         }
      }

      // No singleton instance found -> check artifact definition.
      ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
      if (parentArtifactFactory != null && !containsArtifactDefinition(artifactName)) {
         // No artifact definition found in this factory -> delegate to parent.
         return parentArtifactFactory.isSingleton(originalArtifactName(name));
      }

      RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);

      // In case of FactoryArtifact, return singleton status of created object if not a dereference.
      if (mbd.isSingleton()) {
         if (isFactoryArtifact(artifactName, mbd)) {
            if (ArtifactFactoryUtils.isFactoryDereference(name)) {
               return true;
            }
            FactoryArtifact<?> factoryArtifact = (FactoryArtifact<?>) getArtifact(FACTORY_BEAN_PREFIX + artifactName);
            return factoryArtifact.isSingleton();
         }
         else {
            return !ArtifactFactoryUtils.isFactoryDereference(name);
         }
      }
      else {
         return false;
      }
   }

   @Override
   public boolean isPrototype(String name) throws NoSuchArtifactDefinitionException {
      String artifactName = transformedArtifactName(name);

      ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
      if (parentArtifactFactory != null && !containsArtifactDefinition(artifactName)) {
         // No artifact definition found in this factory -> delegate to parent.
         return parentArtifactFactory.isPrototype(originalArtifactName(name));
      }

      RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
      if (mbd.isPrototype()) {
         // In case of FactoryArtifact, return singleton status of created object if not a dereference.
         return (!ArtifactFactoryUtils.isFactoryDereference(name) || isFactoryArtifact(artifactName, mbd));
      }

      // Singleton or scoped - not a prototype.
      // However, FactoryArtifact may still produce a prototype object...
      if (ArtifactFactoryUtils.isFactoryDereference(name)) {
         return false;
      }
      if (isFactoryArtifact(artifactName, mbd)) {
         final FactoryArtifact<?> fb = (FactoryArtifact<?>) getArtifact(FACTORY_BEAN_PREFIX + artifactName);
         if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Boolean>) () ->
                  ((fb instanceof SmartFactoryArtifact && ((SmartFactoryArtifact<?>) fb).isPrototype()) || !fb.isSingleton()),
               getAccessControlContext());
         }
         else {
            return ((fb instanceof SmartFactoryArtifact && ((SmartFactoryArtifact<?>) fb).isPrototype()) ||
               !fb.isSingleton());
         }
      }
      else {
         return false;
      }
   }

   @Override
   public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchArtifactDefinitionException {
      return isTypeMatch(name, typeToMatch, true);
   }

   /**
    * Internal extended variant of {@link #isTypeMatch(String, ResolvableType)}
    * to check whether the artifact with the given name matches the specified type. Allow
    * additional constraints to be applied to ensure that artifacts are not created early.
    * @param name the name of the artifact to query
    * @param typeToMatch the type to match against (as a
    * {@code ResolvableType})
    * @return {@code true} if the artifact type matches, {@code false} if it
    * doesn't match or cannot be determined yet
    * @throws NoSuchArtifactDefinitionException if there is no artifact with the given name
    * @see #getArtifact
    * @see #getType
    */
   protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryArtifactInit)
      throws NoSuchArtifactDefinitionException {

      String artifactName = transformedArtifactName(name);
      boolean isFactoryDereference = ArtifactFactoryUtils.isFactoryDereference(name);

      // Check manually registered singletons.
      Object artifactInstance = getSingleton(artifactName, false);
      if (artifactInstance != null && artifactInstance.getClass() != NullArtifact.class) {
         if (artifactInstance instanceof FactoryArtifact) {
            if (!isFactoryDereference) {
               Class<?> type = getTypeForFactoryArtifact((FactoryArtifact<?>) artifactInstance);
               return (type != null && typeToMatch.isAssignableFrom(type));
            }
            else {
               return typeToMatch.isInstance(artifactInstance);
            }
         }
         else if (!isFactoryDereference) {
            if (typeToMatch.isInstance(artifactInstance)) {
               // Direct match for exposed instance?
               return true;
            }
            else if (typeToMatch.hasGenerics() && containsArtifactDefinition(artifactName)) {
               // Generics potentially only match on the target class, not on the proxy...
               RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
               Class<?> targetType = mbd.getTargetType();
               if (targetType != null && targetType != ClassUtils.getUserClass(artifactInstance)) {
                  // Check raw class match as well, making sure it's exposed on the proxy.
                  Class<?> classToMatch = typeToMatch.resolve();
                  if (classToMatch != null && !classToMatch.isInstance(artifactInstance)) {
                     return false;
                  }
                  if (typeToMatch.isAssignableFrom(targetType)) {
                     return true;
                  }
               }
               ResolvableType resolvableType = mbd.targetType;
               if (resolvableType == null) {
                  resolvableType = mbd.factoryMethodReturnType;
               }
               return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
            }
         }
         return false;
      }
      else if (containsSingleton(artifactName) && !containsArtifactDefinition(artifactName)) {
         // null instance registered
         return false;
      }

      // No singleton instance found -> check artifact definition.
      ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
      if (parentArtifactFactory != null && !containsArtifactDefinition(artifactName)) {
         // No artifact definition found in this factory -> delegate to parent.
         return parentArtifactFactory.isTypeMatch(originalArtifactName(name), typeToMatch);
      }

      // Retrieve corresponding artifact definition.
      RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
      ArtifactDefinitionHolder dbd = mbd.getDecoratedDefinition();

      // Setup the types that we want to match against
      Class<?> classToMatch = typeToMatch.resolve();
      if (classToMatch == null) {
         classToMatch = FactoryArtifact.class;
      }
      Class<?>[] typesToMatch = (FactoryArtifact.class == classToMatch ?
         new Class<?>[] {classToMatch} : new Class<?>[] {FactoryArtifact.class, classToMatch});


      // Attempt to predict the artifact type
      Class<?> predictedType = null;

      // We're looking for a regular reference but we're a factory artifact that has
      // a decorated artifact definition. The target artifact should be the same type
      // as FactoryArtifact would ultimately return.
      if (!isFactoryDereference && dbd != null && isFactoryArtifact(artifactName, mbd)) {
         // We should only attempt if the user explicitly set lazy-init to true
         // and we know the merged artifact definition is for a factory artifact.
         if (!mbd.isLazyInit() || allowFactoryArtifactInit) {
            RootArtifactDefinition tbd = getMergedArtifactDefinition(dbd.getArtifactName(), dbd.getArtifactDefinition(), mbd);
            Class<?> targetType = predictArtifactType(dbd.getArtifactName(), tbd, typesToMatch);
            if (targetType != null && !FactoryArtifact.class.isAssignableFrom(targetType)) {
               predictedType = targetType;
            }
         }
      }

      // If we couldn't use the target type, try regular prediction.
      if (predictedType == null) {
         predictedType = predictArtifactType(artifactName, mbd, typesToMatch);
         if (predictedType == null) {
            return false;
         }
      }

      // Attempt to get the actual ResolvableType for the artifact.
      ResolvableType artifactType = null;

      // If it's a FactoryArtifact, we want to look at what it creates, not the factory class.
      if (FactoryArtifact.class.isAssignableFrom(predictedType)) {
         if (artifactInstance == null && !isFactoryDereference) {
            artifactType = getTypeForFactoryArtifact(artifactName, mbd, allowFactoryArtifactInit);
            predictedType = artifactType.resolve();
            if (predictedType == null) {
               return false;
            }
         }
      }
      else if (isFactoryDereference) {
         // Special case: A SmartInstantiationAwareArtifactPostProcessor returned a non-FactoryArtifact
         // type but we nevertheless are being asked to dereference a FactoryArtifact...
         // Let's check the original artifact class and proceed with it if it is a FactoryArtifact.
         predictedType = predictArtifactType(artifactName, mbd, FactoryArtifact.class);
         if (predictedType == null || !FactoryArtifact.class.isAssignableFrom(predictedType)) {
            return false;
         }
      }

      // We don't have an exact type but if artifact definition target type or the factory
      // method return type matches the predicted type then we can use that.
      if (artifactType == null) {
         ResolvableType definedType = mbd.targetType;
         if (definedType == null) {
            definedType = mbd.factoryMethodReturnType;
         }
         if (definedType != null && definedType.resolve() == predictedType) {
            artifactType = definedType;
         }
      }

      // If we have a artifact type use it so that generics are considered
      if (artifactType != null) {
         return typeToMatch.isAssignableFrom(artifactType);
      }

      // If we don't have a artifact type, fallback to the predicted type
      return typeToMatch.isAssignableFrom(predictedType);
   }

   @Override
   public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchArtifactDefinitionException {
      return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
   }

   @Override
   @Nullable
   public Class<?> getType(String name) throws NoSuchArtifactDefinitionException {
      return getType(name, true);
   }

   @Override
   @Nullable
   public Class<?> getType(String name, boolean allowFactoryArtifactInit) throws NoSuchArtifactDefinitionException {
      String artifactName = transformedArtifactName(name);

      // Check manually registered singletons.
      Object artifactInstance = getSingleton(artifactName, false);
      if (artifactInstance != null && artifactInstance.getClass() != NullArtifact.class) {
         if (artifactInstance instanceof FactoryArtifact && !ArtifactFactoryUtils.isFactoryDereference(name)) {
            return getTypeForFactoryArtifact((FactoryArtifact<?>) artifactInstance);
         }
         else {
            return artifactInstance.getClass();
         }
      }

      // No singleton instance found -> check artifact definition.
      ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
      if (parentArtifactFactory != null && !containsArtifactDefinition(artifactName)) {
         // No artifact definition found in this factory -> delegate to parent.
         return parentArtifactFactory.getType(originalArtifactName(name));
      }

      RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);

      // Check decorated artifact definition, if any: We assume it'll be easier
      // to determine the decorated artifact's type than the proxy's type.
      ArtifactDefinitionHolder dbd = mbd.getDecoratedDefinition();
      if (dbd != null && !ArtifactFactoryUtils.isFactoryDereference(name)) {
         RootArtifactDefinition tbd = getMergedArtifactDefinition(dbd.getArtifactName(), dbd.getArtifactDefinition(), mbd);
         Class<?> targetClass = predictArtifactType(dbd.getArtifactName(), tbd);
         if (targetClass != null && !FactoryArtifact.class.isAssignableFrom(targetClass)) {
            return targetClass;
         }
      }

      Class<?> artifactClass = predictArtifactType(artifactName, mbd);

      // Check artifact class whether we're dealing with a FactoryArtifact.
      if (artifactClass != null && FactoryArtifact.class.isAssignableFrom(artifactClass)) {
         if (!ArtifactFactoryUtils.isFactoryDereference(name)) {
            // If it's a FactoryArtifact, we want to look at what it creates, not at the factory class.
            return getTypeForFactoryArtifact(artifactName, mbd, allowFactoryArtifactInit).resolve();
         }
         else {
            return artifactClass;
         }
      }
      else {
         return (!ArtifactFactoryUtils.isFactoryDereference(name) ? artifactClass : null);
      }
   }

   @Override
   public String[] getAliases(String name) {
      String artifactName = transformedArtifactName(name);
      List<String> aliases = new ArrayList<>();
      boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
      String fullArtifactName = artifactName;
      if (factoryPrefix) {
         fullArtifactName = FACTORY_BEAN_PREFIX + artifactName;
      }
      if (!fullArtifactName.equals(name)) {
         aliases.add(fullArtifactName);
      }
      String[] retrievedAliases = super.getAliases(artifactName);
      String prefix = factoryPrefix ? FACTORY_BEAN_PREFIX : "";
      for (String retrievedAlias : retrievedAliases) {
         String alias = prefix + retrievedAlias;
         if (!alias.equals(name)) {
            aliases.add(alias);
         }
      }
      if (!containsSingleton(artifactName) && !containsArtifactDefinition(artifactName)) {
         ArtifactFactory parentArtifactFactory = getParentArtifactFactory();
         if (parentArtifactFactory != null) {
            aliases.addAll(Arrays.asList(parentArtifactFactory.getAliases(fullArtifactName)));
         }
      }
      return StringUtils.toStringArray(aliases);
   }
   
   //---------------------------------------------------------------------
   // Implementation of HierarchicalArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   @Nullable
   public ArtifactFactory getParentArtifactFactory() {
      return this.parentArtifactFactory;
   }

   @Override
   public boolean containsLocalArtifact(String name) {
      String artifactName = transformedArtifactName(name);
      return ((containsSingleton(artifactName) || containsArtifactDefinition(artifactName)) &&
         (!ArtifactFactoryUtils.isFactoryDereference(name) || isFactoryArtifact(artifactName)));
   }

   //---------------------------------------------------------------------
   // Implementation of ConfigurableArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public void setParentArtifactFactory(@Nullable ArtifactFactory parentArtifactFactory) {
      if (this.parentArtifactFactory != null && this.parentArtifactFactory != parentArtifactFactory) {
         throw new IllegalStateException("Already associated with parent ArtifactFactory: " + this.parentArtifactFactory);
      }
      this.parentArtifactFactory = parentArtifactFactory;
   }

   @Override
   public void setArtifactClassLoader(@Nullable ClassLoader artifactClassLoader) {
      this.artifactClassLoader = (artifactClassLoader != null ? artifactClassLoader : ClassUtils.getDefaultClassLoader());
   }

   @Override
   @Nullable
   public ClassLoader getArtifactClassLoader() {
      return this.artifactClassLoader;
   }

   @Override
   public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
      this.tempClassLoader = tempClassLoader;
   }

   @Override
   @Nullable
   public ClassLoader getTempClassLoader() {
      return this.tempClassLoader;
   }

   @Override
   public void setCacheArtifactMetadata(boolean cacheArtifactMetadata) {
      this.cacheArtifactMetadata = cacheArtifactMetadata;
   }

   @Override
   public boolean isCacheArtifactMetadata() {
      return this.cacheArtifactMetadata;
   }

   @Override
   public void setArtifactExpressionResolver(@Nullable ArtifactExpressionResolver resolver) {
      this.artifactExpressionResolver = resolver;
   }

   @Override
   @Nullable
   public ArtifactExpressionResolver getArtifactExpressionResolver() {
      return this.artifactExpressionResolver;
   }

   @Override
   public void setConversionService(@Nullable ConversionService conversionService) {
      this.conversionService = conversionService;
   }

   @Override
   @Nullable
   public ConversionService getConversionService() {
      return this.conversionService;
   }

   @Override
   public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
      AssertUtils.notNull(registrar, "PropertyEditorRegistrar must not be null");
      this.propertyEditorRegistrars.add(registrar);
   }

   /**
    * Return the set of PropertyEditorRegistrars.
    */
   public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
      return this.propertyEditorRegistrars;
   }

   @Override
   public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
      AssertUtils.notNull(requiredType, "Required type must not be null");
      AssertUtils.notNull(propertyEditorClass, "PropertyEditor class must not be null");
      this.customEditors.put(requiredType, propertyEditorClass);
   }

   @Override
   public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
      registerCustomEditors(registry);
   }

   /**
    * Return the map of custom editors, with Classes as keys and PropertyEditor classes as values.
    */
   public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
      return this.customEditors;
   }

   @Override
   public void setTypeConverter(TypeConverter typeConverter) {
      this.typeConverter = typeConverter;
   }

   /**
    * Return the custom TypeConverter to use, if any.
    * @return the custom TypeConverter, or {@code null} if none specified
    */
   @Nullable
   protected TypeConverter getCustomTypeConverter() {
      return this.typeConverter;
   }

   @Override
   public TypeConverter getTypeConverter() {
      TypeConverter customConverter = getCustomTypeConverter();
      if (customConverter != null) {
         return customConverter;
      }
      else {
         // Build default TypeConverter, registering custom editors.
         SimpleTypeConverter typeConverter = new SimpleTypeConverter();
         typeConverter.setConversionService(getConversionService());
         registerCustomEditors(typeConverter);
         return typeConverter;
      }
   }

   @Override
   public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
      AssertUtils.notNull(valueResolver, "StringValueResolver must not be null");
      this.embeddedValueResolvers.add(valueResolver);
   }

   @Override
   public boolean hasEmbeddedValueResolver() {
      return !this.embeddedValueResolvers.isEmpty();
   }

   @Override
   @Nullable
   public String resolveEmbeddedValue(@Nullable String value) {
      if (value == null) {
         return null;
      }
      String result = value;
      for (StringValueResolver resolver : this.embeddedValueResolvers) {
         result = resolver.resolveStringValue(result);
         if (result == null) {
            return null;
         }
      }
      return result;
   }

   @Override
   public void addArtifactPostProcessor(ArtifactPostProcessor artifactPostProcessor) {
      AssertUtils.notNull(artifactPostProcessor, "ArtifactPostProcessor must not be null");
      // Remove from old position, if any
      this.artifactPostProcessors.remove(artifactPostProcessor);
      // Track whether it is instantiation/destruction aware
      if (artifactPostProcessor instanceof InstantiationAwareArtifactPostProcessor) {
         this.hasInstantiationAwareArtifactPostProcessors = true;
      }
      if (artifactPostProcessor instanceof DestructionAwareArtifactPostProcessor) {
         this.hasDestructionAwareArtifactPostProcessors = true;
      }
      // Add to end of list
      this.artifactPostProcessors.add(artifactPostProcessor);
   }

   @Override
   public int getArtifactPostProcessorCount() {
      return this.artifactPostProcessors.size();
   }

   /**
    * Return the list of ArtifactPostProcessors that will get applied
    * to artifacts created with this factory.
    */
   public List<ArtifactPostProcessor> getArtifactPostProcessors() {
      return this.artifactPostProcessors;
   }

   /**
    * Return whether this factory holds a InstantiationAwareArtifactPostProcessor
    * that will get applied to singleton artifacts on shutdown.
    * @see #addArtifactPostProcessor
    * @see foundation.polar.gratify.artifacts.factory.config.InstantiationAwareArtifactPostProcessor
    */
   protected boolean hasInstantiationAwareArtifactPostProcessors() {
      return this.hasInstantiationAwareArtifactPostProcessors;
   }

   /**
    * Return whether this factory holds a DestructionAwareArtifactPostProcessor
    * that will get applied to singleton artifacts on shutdown.
    * @see #addArtifactPostProcessor
    * @see foundation.polar.gratify.artifacts.factory.config.DestructionAwareArtifactPostProcessor
    */
   protected boolean hasDestructionAwareArtifactPostProcessors() {
      return this.hasDestructionAwareArtifactPostProcessors;
   }

   @Override
   public void registerScope(String scopeName, Scope scope) {
      AssertUtils.notNull(scopeName, "Scope identifier must not be null");
      AssertUtils.notNull(scope, "Scope must not be null");
      if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
         throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
      }
      Scope previous = this.scopes.put(scopeName, scope);
      if (previous != null && previous != scope) {
         if (logger.isDebugEnabled()) {
            logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
         }
      }
      else {
         if (logger.isTraceEnabled()) {
            logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
         }
      }
   }

   @Override
   public String[] getRegisteredScopeNames() {
      return StringUtils.toStringArray(this.scopes.keySet());
   }

   @Override
   @Nullable
   public Scope getRegisteredScope(String scopeName) {
      AssertUtils.notNull(scopeName, "Scope identifier must not be null");
      return this.scopes.get(scopeName);
   }

   /**
    * Set the security context provider for this artifact factory. If a security manager
    * is set, interaction with the user code will be executed using the privileged
    * of the provided security context.
    */
   public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
      this.securityContextProvider = securityProvider;
   }

   /**
    * Delegate the creation of the access control context to the
    * {@link #setSecurityContextProvider SecurityContextProvider}.
    */
   @Override
   public AccessControlContext getAccessControlContext() {
      return (this.securityContextProvider != null ?
         this.securityContextProvider.getAccessControlContext() :
         AccessController.getContext());
   }

   @Override
   public void copyConfigurationFrom(ConfigurableArtifactFactory otherFactory) {
      AssertUtils.notNull(otherFactory, "ArtifactFactory must not be null");
      setArtifactClassLoader(otherFactory.getArtifactClassLoader());
      setCacheArtifactMetadata(otherFactory.isCacheArtifactMetadata());
      setArtifactExpressionResolver(otherFactory.getArtifactExpressionResolver());
      setConversionService(otherFactory.getConversionService());
      if (otherFactory instanceof AbstractArtifactFactory) {
         AbstractArtifactFactory otherAbstractFactory = (AbstractArtifactFactory) otherFactory;
         this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
         this.customEditors.putAll(otherAbstractFactory.customEditors);
         this.typeConverter = otherAbstractFactory.typeConverter;
         this.artifactPostProcessors.addAll(otherAbstractFactory.artifactPostProcessors);
         this.hasInstantiationAwareArtifactPostProcessors = this.hasInstantiationAwareArtifactPostProcessors ||
            otherAbstractFactory.hasInstantiationAwareArtifactPostProcessors;
         this.hasDestructionAwareArtifactPostProcessors = this.hasDestructionAwareArtifactPostProcessors ||
            otherAbstractFactory.hasDestructionAwareArtifactPostProcessors;
         this.scopes.putAll(otherAbstractFactory.scopes);
         this.securityContextProvider = otherAbstractFactory.securityContextProvider;
      }
      else {
         setTypeConverter(otherFactory.getTypeConverter());
         String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
         for (String scopeName : otherScopeNames) {
            this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
         }
      }
   }

   /**
    * Return a 'merged' ArtifactDefinition for the given artifact name,
    * merging a child artifact definition with its parent if necessary.
    * <p>This {@code getMergedArtifactDefinition} considers artifact definition
    * in ancestors as well.
    * @param name the name of the artifact to retrieve the merged definition for
    * (may be an alias)
    * @return a (potentially merged) RootArtifactDefinition for the given artifact
    * @throws NoSuchArtifactDefinitionException if there is no artifact with the given name
    * @throws ArtifactDefinitionStoreException in case of an invalid artifact definition
    */
   @Override
   public ArtifactDefinition getMergedArtifactDefinition(String name) throws ArtifactsException {
      String artifactName = transformedArtifactName(name);
      // Efficiently check whether artifact definition exists in this factory.
      if (!containsArtifactDefinition(artifactName) && getParentArtifactFactory() instanceof ConfigurableArtifactFactory) {
         return ((ConfigurableArtifactFactory) getParentArtifactFactory()).getMergedArtifactDefinition(artifactName);
      }
      // Resolve merged artifact definition locally.
      return getMergedLocalArtifactDefinition(artifactName);
   }

   @Override
   public boolean isFactoryArtifact(String name) throws NoSuchArtifactDefinitionException {
      String artifactName = transformedArtifactName(name);
      Object artifactInstance = getSingleton(artifactName, false);
      if (artifactInstance != null) {
         return (artifactInstance instanceof FactoryArtifact);
      }
      // No singleton instance found -> check artifact definition.
      if (!containsArtifactDefinition(artifactName) && getParentArtifactFactory() instanceof ConfigurableArtifactFactory) {
         // No artifact definition found in this factory -> delegate to parent.
         return ((ConfigurableArtifactFactory) getParentArtifactFactory()).isFactoryArtifact(name);
      }
      return isFactoryArtifact(artifactName, getMergedLocalArtifactDefinition(artifactName));
   }

   @Override
   public boolean isActuallyInCreation(String artifactName) {
      return (isSingletonCurrentlyInCreation(artifactName) || isPrototypeCurrentlyInCreation(artifactName));
   }

   /**
    * Return whether the specified prototype artifact is currently in creation
    * (within the current thread).
    * @param artifactName the name of the artifact
    */
   protected boolean isPrototypeCurrentlyInCreation(String artifactName) {
      Object curVal = this.prototypesCurrentlyInCreation.get();
      return (curVal != null &&
         (curVal.equals(artifactName) || (curVal instanceof Set && ((Set<?>) curVal).contains(artifactName))));
   }

   /**
    * Callback before prototype creation.
    * <p>The default implementation register the prototype as currently in creation.
    * @param artifactName the name of the prototype about to be created
    * @see #isPrototypeCurrentlyInCreation
    */
   @SuppressWarnings("unchecked")
   protected void beforePrototypeCreation(String artifactName) {
      Object curVal = this.prototypesCurrentlyInCreation.get();
      if (curVal == null) {
         this.prototypesCurrentlyInCreation.set(artifactName);
      }
      else if (curVal instanceof String) {
         Set<String> artifactNameSet = new HashSet<>(2);
         artifactNameSet.add((String) curVal);
         artifactNameSet.add(artifactName);
         this.prototypesCurrentlyInCreation.set(artifactNameSet);
      }
      else {
         Set<String> artifactNameSet = (Set<String>) curVal;
         artifactNameSet.add(artifactName);
      }
   }

   /**
    * Callback after prototype creation.
    * <p>The default implementation marks the prototype as not in creation anymore.
    * @param artifactName the name of the prototype that has been created
    * @see #isPrototypeCurrentlyInCreation
    */
   @SuppressWarnings("unchecked")
   protected void afterPrototypeCreation(String artifactName) {
      Object curVal = this.prototypesCurrentlyInCreation.get();
      if (curVal instanceof String) {
         this.prototypesCurrentlyInCreation.remove();
      }
      else if (curVal instanceof Set) {
         Set<String> artifactNameSet = (Set<String>) curVal;
         artifactNameSet.remove(artifactName);
         if (artifactNameSet.isEmpty()) {
            this.prototypesCurrentlyInCreation.remove();
         }
      }
   }

   @Override
   public void destroyArtifact(String artifactName, Object artifactInstance) {
      destroyArtifact(artifactName, artifactInstance, getMergedLocalArtifactDefinition(artifactName));
   }

   /**
    * Destroy the given artifact instance (usually a prototype instance
    * obtained from this factory) according to the given artifact definition.
    * @param artifactName the name of the artifact definition
    * @param artifact the artifact instance to destroy
    * @param mbd the merged artifact definition
    */
   protected void destroyArtifact(String artifactName, Object artifact, RootArtifactDefinition mbd) {
      new DisposableArtifactAdapter(artifact, artifactName, mbd, getArtifactPostProcessors(), getAccessControlContext()).destroy();
   }

   @Override
   public void destroyScopedArtifact(String artifactName) {
      RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
      if (mbd.isSingleton() || mbd.isPrototype()) {
         throw new IllegalArgumentException(
            "Artifact name '" + artifactName + "' does not correspond to an object in a mutable scope");
      }
      String scopeName = mbd.getScope();
      Scope scope = this.scopes.get(scopeName);
      if (scope == null) {
         throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
      }
      Object artifact = scope.remove(artifactName);
      if (artifact != null) {
         destroyArtifact(artifactName, artifact, mbd);
      }
   }


   //---------------------------------------------------------------------
   // Implementation methods
   //---------------------------------------------------------------------

   /**
    * Return the artifact name, stripping out the factory dereference prefix if necessary,
    * and resolving aliases to canonical names.
    * @param name the user-specified name
    * @return the transformed artifact name
    */
   protected String transformedArtifactName(String name) {
      return canonicalName(ArtifactFactoryUtils.transformedArtifactName(name));
   }

   /**
    * Determine the original artifact name, resolving locally defined aliases to canonical names.
    * @param name the user-specified name
    * @return the original artifact name
    */
   protected String originalArtifactName(String name) {
      String artifactName = transformedArtifactName(name);
      if (name.startsWith(FACTORY_BEAN_PREFIX)) {
         artifactName = FACTORY_BEAN_PREFIX + artifactName;
      }
      return artifactName;
   }

   /**
    * Initialize the given ArtifactWrapper with the custom editors registered
    * with this factory. To be called for ArtifactWrappers that will create
    * and populate artifact instances.
    * <p>The default implementation delegates to {@link #registerCustomEditors}.
    * Can be overridden in subclasses.
    * @param bw the ArtifactWrapper to initialize
    */
   protected void initArtifactWrapper(ArtifactWrapper bw) {
      bw.setConversionService(getConversionService());
      registerCustomEditors(bw);
   }

   /**
    * Initialize the given PropertyEditorRegistry with the custom editors
    * that have been registered with this ArtifactFactory.
    * <p>To be called for ArtifactWrappers that will create and populate artifact
    * instances, and for SimpleTypeConverter used for constructor argument
    * and factory method type conversion.
    * @param registry the PropertyEditorRegistry to initialize
    */
   protected void registerCustomEditors(PropertyEditorRegistry registry) {
      PropertyEditorRegistrySupport registrySupport =
         (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
      if (registrySupport != null) {
         registrySupport.useConfigValueEditors();
      }
      if (!this.propertyEditorRegistrars.isEmpty()) {
         for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
            try {
               registrar.registerCustomEditors(registry);
            }
            catch (ArtifactCreationException ex) {
               Throwable rootCause = ex.getMostSpecificCause();
               if (rootCause instanceof ArtifactCurrentlyInCreationException) {
                  ArtifactCreationException bce = (ArtifactCreationException) rootCause;
                  String bceArtifactName = bce.getArtifactName();
                  if (bceArtifactName != null && isCurrentlyInCreation(bceArtifactName)) {
                     if (logger.isDebugEnabled()) {
                        logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
                           "] failed because it tried to obtain currently created artifact '" +
                           ex.getArtifactName() + "': " + ex.getMessage());
                     }
                     onSuppressedException(ex);
                     continue;
                  }
               }
               throw ex;
            }
         }
      }
      if (!this.customEditors.isEmpty()) {
         this.customEditors.forEach((requiredType, editorClass) ->
            registry.registerCustomEditor(requiredType, ArtifactUtils.instantiateClass(editorClass)));
      }
   }


   /**
    * Return a merged RootArtifactDefinition, traversing the parent artifact definition
    * if the specified artifact corresponds to a child artifact definition.
    * @param artifactName the name of the artifact to retrieve the merged definition for
    * @return a (potentially merged) RootArtifactDefinition for the given artifact
    * @throws NoSuchArtifactDefinitionException if there is no artifact with the given name
    * @throws ArtifactDefinitionStoreException in case of an invalid artifact definition
    */
   protected RootArtifactDefinition getMergedLocalArtifactDefinition(String artifactName) throws ArtifactsException {
      // Quick check on the concurrent map first, with minimal locking.
      RootArtifactDefinition mbd = this.mergedArtifactDefinitions.get(artifactName);
      if (mbd != null && !mbd.stale) {
         return mbd;
      }
      return getMergedArtifactDefinition(artifactName, getArtifactDefinition(artifactName));
   }

   /**
    * Return a RootArtifactDefinition for the given top-level artifact, by merging with
    * the parent if the given artifact's definition is a child artifact definition.
    * @param artifactName the name of the artifact definition
    * @param bd the original artifact definition (Root/ChildArtifactDefinition)
    * @return a (potentially merged) RootArtifactDefinition for the given artifact
    * @throws ArtifactDefinitionStoreException in case of an invalid artifact definition
    */
   protected RootArtifactDefinition getMergedArtifactDefinition(String artifactName, ArtifactDefinition bd)
      throws ArtifactDefinitionStoreException {

      return getMergedArtifactDefinition(artifactName, bd, null);
   }

   /**
    * Return a RootArtifactDefinition for the given artifact, by merging with the
    * parent if the given artifact's definition is a child artifact definition.
    * @param artifactName the name of the artifact definition
    * @param bd the original artifact definition (Root/ChildArtifactDefinition)
    * @param containingBd the containing artifact definition in case of inner artifact,
    * or {@code null} in case of a top-level artifact
    * @return a (potentially merged) RootArtifactDefinition for the given artifact
    * @throws ArtifactDefinitionStoreException in case of an invalid artifact definition
    */
   protected RootArtifactDefinition getMergedArtifactDefinition(
      String artifactName, ArtifactDefinition bd, @Nullable ArtifactDefinition containingBd)
      throws ArtifactDefinitionStoreException {

      synchronized (this.mergedArtifactDefinitions) {
         RootArtifactDefinition mbd = null;
         RootArtifactDefinition previous = null;

         // Check with full lock now in order to enforce the same merged instance.
         if (containingBd == null) {
            mbd = this.mergedArtifactDefinitions.get(artifactName);
         }

         if (mbd == null || mbd.stale) {
            previous = mbd;
            if (bd.getParentName() == null) {
               // Use copy of given root artifact definition.
               if (bd instanceof RootArtifactDefinition) {
                  mbd = ((RootArtifactDefinition) bd).cloneArtifactDefinition();
               }
               else {
                  mbd = new RootArtifactDefinition(bd);
               }
            }
            else {
               // Child artifact definition: needs to be merged with parent.
               ArtifactDefinition pbd;
               try {
                  String parentArtifactName = transformedArtifactName(bd.getParentName());
                  if (!artifactName.equals(parentArtifactName)) {
                     pbd = getMergedArtifactDefinition(parentArtifactName);
                  }
                  else {
                     ArtifactFactory parent = getParentArtifactFactory();
                     if (parent instanceof ConfigurableArtifactFactory) {
                        pbd = ((ConfigurableArtifactFactory) parent).getMergedArtifactDefinition(parentArtifactName);
                     }
                     else {
                        throw new NoSuchArtifactDefinitionException(parentArtifactName,
                           "Parent name '" + parentArtifactName + "' is equal to artifact name '" + artifactName +
                              "': cannot be resolved without an AbstractArtifactFactory parent");
                     }
                  }
               }
               catch (NoSuchArtifactDefinitionException ex) {
                  throw new ArtifactDefinitionStoreException(bd.getResourceDescription(), artifactName,
                     "Could not resolve parent artifact definition '" + bd.getParentName() + "'", ex);
               }
               // Deep copy with overridden values.
               mbd = new RootArtifactDefinition(pbd);
               mbd.overrideFrom(bd);
            }

            // Set default singleton scope, if not configured before.
            if (!StringUtils.hasLength(mbd.getScope())) {
               mbd.setScope(SCOPE_SINGLETON);
            }

            // A artifact contained in a non-singleton artifact cannot be a singleton itself.
            // Let's correct this on the fly here, since this might be the result of
            // parent-child merging for the outer artifact, in which case the original inner artifact
            // definition will not have inherited the merged outer artifact's singleton status.
            if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
               mbd.setScope(containingBd.getScope());
            }

            // Cache the merged artifact definition for the time being
            // (it might still get re-merged later on in order to pick up metadata changes)
            if (containingBd == null && isCacheArtifactMetadata()) {
               this.mergedArtifactDefinitions.put(artifactName, mbd);
            }
         }
         if (previous != null) {
            copyRelevantMergedArtifactDefinitionCaches(previous, mbd);
         }
         return mbd;
      }
   }

   private void copyRelevantMergedArtifactDefinitionCaches(RootArtifactDefinition previous, RootArtifactDefinition mbd) {
      if (ObjectUtils.nullSafeEquals(mbd.getArtifactClassName(), previous.getArtifactClassName()) &&
         ObjectUtils.nullSafeEquals(mbd.getFactoryArtifactName(), previous.getFactoryArtifactName()) &&
         ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
         ResolvableType targetType = mbd.targetType;
         ResolvableType previousTargetType = previous.targetType;
         if (targetType == null || targetType.equals(previousTargetType)) {
            mbd.targetType = previousTargetType;
            mbd.isFactoryArtifact = previous.isFactoryArtifact;
            mbd.resolvedTargetType = previous.resolvedTargetType;
            mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
            mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
         }
      }
   }

   /**
    * Check the given merged artifact definition,
    * potentially throwing validation exceptions.
    * @param mbd the merged artifact definition to check
    * @param artifactName the name of the artifact
    * @param args the arguments for artifact creation, if any
    * @throws ArtifactDefinitionStoreException in case of validation failure
    */
   protected void checkMergedArtifactDefinition(RootArtifactDefinition mbd, String artifactName, @Nullable Object[] args)
      throws ArtifactDefinitionStoreException {

      if (mbd.isAbstract()) {
         throw new ArtifactIsAbstractException(artifactName);
      }
   }

   /**
    * Remove the merged artifact definition for the specified artifact,
    * recreating it on next access.
    * @param artifactName the artifact name to clear the merged definition for
    */
   protected void clearMergedArtifactDefinition(String artifactName) {
      RootArtifactDefinition bd = this.mergedArtifactDefinitions.get(artifactName);
      if (bd != null) {
         bd.stale = true;
      }
   }

   /**
    * Clear the merged artifact definition cache, removing entries for artifacts
    * which are not considered eligible for full metadata caching yet.
    * <p>Typically triggered after changes to the original artifact definitions,
    * e.g. after applying a {@code ArtifactFactoryPostProcessor}. Note that metadata
    * for artifacts which have already been created at this point will be kept around.
    */
   public void clearMetadataCache() {
      this.mergedArtifactDefinitions.forEach((artifactName, bd) -> {
         if (!isArtifactEligibleForMetadataCaching(artifactName)) {
            bd.stale = true;
         }
      });
   }

   /**
    * Resolve the artifact class for the specified artifact definition,
    * resolving a artifact class name into a Class reference (if necessary)
    * and storing the resolved Class in the artifact definition for further use.
    * @param mbd the merged artifact definition to determine the class for
    * @param artifactName the name of the artifact (for error handling purposes)
    * @param typesToMatch the types to match in case of internal type matching purposes
    * (also signals that the returned {@code Class} will never be exposed to application code)
    * @return the resolved artifact class (or {@code null} if none)
    * @throws CannotLoadArtifactClassException if we failed to load the class
    */
   @Nullable
   protected Class<?> resolveArtifactClass(final RootArtifactDefinition mbd, String artifactName, final Class<?>... typesToMatch)
      throws CannotLoadArtifactClassException {

      try {
         if (mbd.hasArtifactClass()) {
            return mbd.getArtifactClass();
         }
         if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>) () ->
               doResolveArtifactClass(mbd, typesToMatch), getAccessControlContext());
         }
         else {
            return doResolveArtifactClass(mbd, typesToMatch);
         }
      }
      catch (PrivilegedActionException pae) {
         ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
         throw new CannotLoadArtifactClassException(mbd.getResourceDescription(), artifactName, mbd.getArtifactClassName(), ex);
      }
      catch (ClassNotFoundException ex) {
         throw new CannotLoadArtifactClassException(mbd.getResourceDescription(), artifactName, mbd.getArtifactClassName(), ex);
      }
      catch (LinkageError err) {
         throw new CannotLoadArtifactClassException(mbd.getResourceDescription(), artifactName, mbd.getArtifactClassName(), err);
      }
   }

   @Nullable
   private Class<?> doResolveArtifactClass(RootArtifactDefinition mbd, Class<?>... typesToMatch)
      throws ClassNotFoundException {

      ClassLoader artifactClassLoader = getArtifactClassLoader();
      ClassLoader dynamicLoader = artifactClassLoader;
      boolean freshResolve = false;

      if (!ObjectUtils.isEmpty(typesToMatch)) {
         // When just doing type checks (i.e. not creating an actual instance yet),
         // use the specified temporary class loader (e.g. in a weaving scenario).
         ClassLoader tempClassLoader = getTempClassLoader();
         if (tempClassLoader != null) {
            dynamicLoader = tempClassLoader;
            freshResolve = true;
            if (tempClassLoader instanceof DecoratingClassLoader) {
               DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
               for (Class<?> typeToMatch : typesToMatch) {
                  dcl.excludeClass(typeToMatch.getName());
               }
            }
         }
      }

      String className = mbd.getArtifactClassName();
      if (className != null) {
         Object evaluated = evaluateArtifactDefinitionString(className, mbd);
         if (!className.equals(evaluated)) {
            // A dynamically resolved expression, supported as of 4.2...
            if (evaluated instanceof Class) {
               return (Class<?>) evaluated;
            }
            else if (evaluated instanceof String) {
               className = (String) evaluated;
               freshResolve = true;
            }
            else {
               throw new IllegalStateException("Invalid class name expression result: " + evaluated);
            }
         }
         if (freshResolve) {
            // When resolving against a temporary class loader, exit early in order
            // to avoid storing the resolved Class in the artifact definition.
            if (dynamicLoader != null) {
               try {
                  return dynamicLoader.loadClass(className);
               }
               catch (ClassNotFoundException ex) {
                  if (logger.isTraceEnabled()) {
                     logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
                  }
               }
            }
            return ClassUtils.forName(className, dynamicLoader);
         }
      }

      // Resolve regularly, caching the result in the ArtifactDefinition...
      return mbd.resolveArtifactClass(artifactClassLoader);
   }

   /**
    * Evaluate the given String as contained in a artifact definition,
    * potentially resolving it as an expression.
    * @param value the value to check
    * @param artifactDefinition the artifact definition that the value comes from
    * @return the resolved value
    * @see #setArtifactExpressionResolver
    */
   @Nullable
   protected Object evaluateArtifactDefinitionString(@Nullable String value, @Nullable ArtifactDefinition artifactDefinition) {
      if (this.artifactExpressionResolver == null) {
         return value;
      }

      Scope scope = null;
      if (artifactDefinition != null) {
         String scopeName = artifactDefinition.getScope();
         if (scopeName != null) {
            scope = getRegisteredScope(scopeName);
         }
      }
      return this.artifactExpressionResolver.evaluate(value, new ArtifactExpressionContext(this, scope));
   }

   /**
    * Predict the eventual artifact type (of the processed artifact instance) for the
    * specified artifact. Called by {@link #getType} and {@link #isTypeMatch}.
    * Does not need to handle FactoryArtifacts specifically, since it is only
    * supposed to operate on the raw artifact type.
    * <p>This implementation is simplistic in that it is not able to
    * handle factory methods and InstantiationAwareArtifactPostProcessors.
    * It only predicts the artifact type correctly for a standard artifact.
    * To be overridden in subclasses, applying more sophisticated type detection.
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition to determine the type for
    * @param typesToMatch the types to match in case of internal type matching purposes
    * (also signals that the returned {@code Class} will never be exposed to application code)
    * @return the type of the artifact, or {@code null} if not predictable
    */
   @Nullable
   protected Class<?> predictArtifactType(String artifactName, RootArtifactDefinition mbd, Class<?>... typesToMatch) {
      Class<?> targetType = mbd.getTargetType();
      if (targetType != null) {
         return targetType;
      }
      if (mbd.getFactoryMethodName() != null) {
         return null;
      }
      return resolveArtifactClass(mbd, artifactName, typesToMatch);
   }

   /**
    * Check whether the given artifact is defined as a {@link FactoryArtifact}.
    * @param artifactName the name of the artifact
    * @param mbd the corresponding artifact definition
    */
   protected boolean isFactoryArtifact(String artifactName, RootArtifactDefinition mbd) {
      Boolean result = mbd.isFactoryArtifact;
      if (result == null) {
         Class<?> artifactType = predictArtifactType(artifactName, mbd, FactoryArtifact.class);
         result = (artifactType != null && FactoryArtifact.class.isAssignableFrom(artifactType));
         mbd.isFactoryArtifact = result;
      }
      return result;
   }

   /**
    * Determine the artifact type for the given FactoryArtifact definition, as far as possible.
    * Only called if there is no singleton instance registered for the target artifact
    * already. Implementations are only allowed to instantiate the factory artifact if
    * {@code allowInit} is {@code true}, otherwise they should try to determine the
    * result through other means.
    * <p>If no {@link FactoryArtifact#OBJECT_TYPE_ATTRIBUTE} if set on the artifact definition
    * and {@code allowInit} is {@code true}, the default implementation will create
    * the FactoryArtifact via {@code getArtifact} to call its {@code getObjectType} method.
    * Subclasses are encouraged to optimize this, typically by inspecting the generic
    * signature of the factory artifact class or the factory method that creates it. If
    * subclasses do instantiate the FactoryArtifact, they should consider trying the
    * {@code getObjectType} method without fully populating the artifact. If this fails, a
    * full FactoryArtifact creation as performed by this implementation should be used as
    * fallback.
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition for the artifact
    * @param allowInit if initialization of the FactoryArtifact is permitted
    * @return the type for the artifact if determinable, otherwise {@code ResolvableType.NONE}
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact#getObjectType()
    * @see #getArtifact(String)
    */
   protected ResolvableType getTypeForFactoryArtifact(String artifactName, RootArtifactDefinition mbd, boolean allowInit) {
      ResolvableType result = getTypeForFactoryArtifactFromAttributes(mbd);
      if (result != ResolvableType.NONE) {
         return result;
      }

      if (allowInit && mbd.isSingleton()) {
         try {
            FactoryArtifact<?> factoryArtifact = doGetArtifact(FACTORY_BEAN_PREFIX + artifactName, FactoryArtifact.class, null, true);
            Class<?> objectType = getTypeForFactoryArtifact(factoryArtifact);
            return (objectType != null) ? ResolvableType.forClass(objectType) : ResolvableType.NONE;
         }
         catch (ArtifactCreationException ex) {
            if (ex.contains(ArtifactCurrentlyInCreationException.class)) {
               logger.trace(LogMessage.format("Artifact currently in creation on FactoryArtifact type check: %s", ex));
            }
            else if (mbd.isLazyInit()) {
               logger.trace(LogMessage.format("Artifact creation exception on lazy FactoryArtifact type check: %s", ex));
            }
            else {
               logger.debug(LogMessage.format("Artifact creation exception on non-lazy FactoryArtifact type check: %s", ex));
            }
            onSuppressedException(ex);
         }
      }
      return ResolvableType.NONE;
   }

   /**
    * Determine the artifact type for a FactoryArtifact by inspecting its attributes for a
    * {@link FactoryArtifact#OBJECT_TYPE_ATTRIBUTE} value.
    * @param attributes the attributes to inspect
    * @return a {@link ResolvableType} extracted from the attributes or
    * {@code ResolvableType.NONE}
    */
   ResolvableType getTypeForFactoryArtifactFromAttributes(AttributeAccessor attributes) {
      Object attribute = attributes.getAttribute(FactoryArtifact.OBJECT_TYPE_ATTRIBUTE);
      if (attribute instanceof ResolvableType) {
         return (ResolvableType) attribute;
      }
      if (attribute instanceof Class) {
         return ResolvableType.forClass((Class<?>) attribute);
      }
      return ResolvableType.NONE;
   }

   /**
    * Determine the artifact type for the given FactoryArtifact definition, as far as possible.
    * Only called if there is no singleton instance registered for the target artifact already.
    * <p>The default implementation creates the FactoryArtifact via {@code getArtifact}
    * to call its {@code getObjectType} method. Subclasses are encouraged to optimize
    * this, typically by just instantiating the FactoryArtifact but not populating it yet,
    * trying whether its {@code getObjectType} method already returns a type.
    * If no type found, a full FactoryArtifact creation as performed by this implementation
    * should be used as fallback.
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition for the artifact
    * @return the type for the artifact if determinable, or {@code null} otherwise
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact#getObjectType()
    * @see #getArtifact(String)
    * @deprecated since 5.2 in favor of {@link #getTypeForFactoryArtifact(String, RootArtifactDefinition, boolean)}
    */
   @Nullable
   @Deprecated
   protected Class<?> getTypeForFactoryArtifact(String artifactName, RootArtifactDefinition mbd) {
      return getTypeForFactoryArtifact(artifactName, mbd, true).resolve();
   }

   /**
    * Mark the specified artifact as already created (or about to be created).
    * <p>This allows the artifact factory to optimize its caching for repeated
    * creation of the specified artifact.
    * @param artifactName the name of the artifact
    */
   protected void markArtifactAsCreated(String artifactName) {
      if (!this.alreadyCreated.contains(artifactName)) {
         synchronized (this.mergedArtifactDefinitions) {
            if (!this.alreadyCreated.contains(artifactName)) {
               // Let the artifact definition get re-merged now that we're actually creating
               // the artifact... just in case some of its metadata changed in the meantime.
               clearMergedArtifactDefinition(artifactName);
               this.alreadyCreated.add(artifactName);
            }
         }
      }
   }

   /**
    * Perform appropriate cleanup of cached metadata after artifact creation failed.
    * @param artifactName the name of the artifact
    */
   protected void cleanupAfterArtifactCreationFailure(String artifactName) {
      synchronized (this.mergedArtifactDefinitions) {
         this.alreadyCreated.remove(artifactName);
      }
   }

   /**
    * Determine whether the specified artifact is eligible for having
    * its artifact definition metadata cached.
    * @param artifactName the name of the artifact
    * @return {@code true} if the artifact's metadata may be cached
    * at this point already
    */
   protected boolean isArtifactEligibleForMetadataCaching(String artifactName) {
      return this.alreadyCreated.contains(artifactName);
   }

   /**
    * Remove the singleton instance (if any) for the given artifact name,
    * but only if it hasn't been used for other purposes than type checking.
    * @param artifactName the name of the artifact
    * @return {@code true} if actually removed, {@code false} otherwise
    */
   protected boolean removeSingletonIfCreatedForTypeCheckOnly(String artifactName) {
      if (!this.alreadyCreated.contains(artifactName)) {
         removeSingleton(artifactName);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Check whether this factory's artifact creation phase already started,
    * i.e. whether any artifact has been marked as created in the meantime.
    * @see #markArtifactAsCreated
    */
   protected boolean hasArtifactCreationStarted() {
      return !this.alreadyCreated.isEmpty();
   }

   /**
    * Get the object for the given artifact instance, either the artifact
    * instance itself or its created object in case of a FactoryArtifact.
    * @param artifactInstance the shared artifact instance
    * @param name name that may include factory dereference prefix
    * @param artifactName the canonical artifact name
    * @param mbd the merged artifact definition
    * @return the object to expose for the artifact
    */
   protected Object getObjectForArtifactInstance(
      Object artifactInstance, String name, String artifactName, @Nullable RootArtifactDefinition mbd) {

      // Don't let calling code try to dereference the factory if the artifact isn't a factory.
      if (ArtifactFactoryUtils.isFactoryDereference(name)) {
         if (artifactInstance instanceof NullArtifact) {
            return artifactInstance;
         }
         if (!(artifactInstance instanceof FactoryArtifact)) {
            throw new ArtifactIsNotAFactoryException(artifactName, artifactInstance.getClass());
         }
         if (mbd != null) {
            mbd.isFactoryArtifact = true;
         }
         return artifactInstance;
      }

      // Now we have the artifact instance, which may be a normal artifact or a FactoryArtifact.
      // If it's a FactoryArtifact, we use it to create a artifact instance, unless the
      // caller actually wants a reference to the factory.
      if (!(artifactInstance instanceof FactoryArtifact)) {
         return artifactInstance;
      }

      Object object = null;
      if (mbd != null) {
         mbd.isFactoryArtifact = true;
      }
      else {
         object = getCachedObjectForFactoryArtifact(artifactName);
      }
      if (object == null) {
         // Return artifact instance from factory.
         FactoryArtifact<?> factory = (FactoryArtifact<?>) artifactInstance;
         // Caches object obtained from FactoryArtifact if it is a singleton.
         if (mbd == null && containsArtifactDefinition(artifactName)) {
            mbd = getMergedLocalArtifactDefinition(artifactName);
         }
         boolean synthetic = (mbd != null && mbd.isSynthetic());
         object = getObjectFromFactoryArtifact(factory, artifactName, !synthetic);
      }
      return object;
   }

   /**
    * Determine whether the given artifact name is already in use within this factory,
    * i.e. whether there is a local artifact or alias registered under this name or
    * an inner artifact created with this name.
    * @param artifactName the name to check
    */
   public boolean isArtifactNameInUse(String artifactName) {
      return isAlias(artifactName) || containsLocalArtifact(artifactName) || hasDependentArtifact(artifactName);
   }

   /**
    * Determine whether the given artifact requires destruction on shutdown.
    * <p>The default implementation checks the DisposableArtifact interface as well as
    * a specified destroy method and registered DestructionAwareArtifactPostProcessors.
    * @param artifact the artifact instance to check
    * @param mbd the corresponding artifact definition
    * @see foundation.polar.gratify.artifacts.factory.DisposableArtifact
    * @see AbstractArtifactDefinition#getDestroyMethodName()
    * @see foundation.polar.gratify.artifacts.factory.config.DestructionAwareArtifactPostProcessor
    */
   protected boolean requiresDestruction(Object artifact, RootArtifactDefinition mbd) {
      return (artifact.getClass() != NullArtifact.class &&
         (DisposableArtifactAdapter.hasDestroyMethod(artifact, mbd) || (hasDestructionAwareArtifactPostProcessors() &&
            DisposableArtifactAdapter.hasApplicableProcessors(artifact, getArtifactPostProcessors()))));
   }

   /**
    * Add the given artifact to the list of disposable artifacts in this factory,
    * registering its DisposableArtifact interface and/or the given destroy method
    * to be called on factory shutdown (if applicable). Only applies to singletons.
    * @param artifactName the name of the artifact
    * @param artifact the artifact instance
    * @param mbd the artifact definition for the artifact
    * @see RootArtifactDefinition#isSingleton
    * @see RootArtifactDefinition#getDependsOn
    * @see #registerDisposableArtifact
    * @see #registerDependentArtifact
    */
   protected void registerDisposableArtifactIfNecessary(String artifactName, Object artifact, RootArtifactDefinition mbd) {
      AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
      if (!mbd.isPrototype() && requiresDestruction(artifact, mbd)) {
         if (mbd.isSingleton()) {
            // Register a DisposableArtifact implementation that performs all destruction
            // work for the given artifact: DestructionAwareArtifactPostProcessors,
            // DisposableArtifact interface, custom destroy method.
            registerDisposableArtifact(artifactName,
               new DisposableArtifactAdapter(artifact, artifactName, mbd, getArtifactPostProcessors(), acc));
         }
         else {
            // A artifact with a custom scope...
            Scope scope = this.scopes.get(mbd.getScope());
            if (scope == null) {
               throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
            }
            scope.registerDestructionCallback(artifactName,
               new DisposableArtifactAdapter(artifact, artifactName, mbd, getArtifactPostProcessors(), acc));
         }
      }
   }


   //---------------------------------------------------------------------
   // Abstract methods to be implemented by subclasses
   //---------------------------------------------------------------------

   /**
    * Check if this artifact factory contains a artifact definition with the given name.
    * Does not consider any hierarchy this factory may participate in.
    * Invoked by {@code containsArtifact} when no cached singleton instance is found.
    * <p>Depending on the nature of the concrete artifact factory implementation,
    * this operation might be expensive (for example, because of directory lookups
    * in external registries). However, for listable artifact factories, this usually
    * just amounts to a local hash lookup: The operation is therefore part of the
    * public interface there. The same implementation can serve for both this
    * template method and the public interface method in that case.
    * @param artifactName the name of the artifact to look for
    * @return if this artifact factory contains a artifact definition with the given name
    * @see #containsArtifact
    * @see foundation.polar.gratify.artifacts.factory.ListableArtifactFactory#containsArtifactDefinition
    */
   protected abstract boolean containsArtifactDefinition(String artifactName);

   /**
    * Return the artifact definition for the given artifact name.
    * Subclasses should normally implement caching, as this method is invoked
    * by this class every time artifact definition metadata is needed.
    * <p>Depending on the nature of the concrete artifact factory implementation,
    * this operation might be expensive (for example, because of directory lookups
    * in external registries). However, for listable artifact factories, this usually
    * just amounts to a local hash lookup: The operation is therefore part of the
    * public interface there. The same implementation can serve for both this
    * template method and the public interface method in that case.
    * @param artifactName the name of the artifact to find a definition for
    * @return the ArtifactDefinition for this prototype name (never {@code null})
    * @throws foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException
    * if the artifact definition cannot be resolved
    * @throws ArtifactsException in case of errors
    * @see RootArtifactDefinition
    * @see ChildArtifactDefinition
    * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory#getArtifactDefinition
    */
   protected abstract ArtifactDefinition getArtifactDefinition(String artifactName) throws ArtifactsException;

   /**
    * Create a artifact instance for the given merged artifact definition (and arguments).
    * The artifact definition will already have been merged with the parent definition
    * in case of a child definition.
    * <p>All artifact retrieval methods delegate to this method for actual artifact creation.
    * @param artifactName the name of the artifact
    * @param mbd the merged artifact definition for the artifact
    * @param args explicit arguments to use for constructor or factory method invocation
    * @return a new instance of the artifact
    * @throws ArtifactCreationException if the artifact could not be created
    */
   protected abstract Object createArtifact(String artifactName, RootArtifactDefinition mbd, @Nullable Object[] args)
      throws ArtifactCreationException;
}
