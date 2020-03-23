package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.annotation.MergedAnnotation;
import foundation.polar.gratify.annotation.MergedAnnotations;
import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.*;
import foundation.polar.gratify.core.OrderComparator;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.inject.Provider;
import foundation.polar.gratify.utils.*;
import foundation.polar.gratify.utils.logging.LogMessage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Gratify's default implementation of the {@link ConfigurableListableArtifactFactory}
 * and {@link ArtifactDefinitionRegistry} interfaces: a full-fledged artifact factory
 * based on artifact definition metadata, extensible through post-processors.
 *
 * <p>Typical usage is registering all artifact definitions first (possibly read
 * from a artifact definition file), before accessing artifacts. Artifact lookup by name
 * is therefore an inexpensive operation in a local artifact definition table,
 * operating on pre-resolved artifact definition metadata objects.
 *
 * <p>Note that readers for specific artifact definition formats are typically
 * implemented separately rather than as artifact factory subclasses:
 * see for example {@link PropertiesArtifactDefinitionReader} and
 * {@link foundation.polar.gratify.artifacts.factory.xml.XmlArtifactDefinitionReader}.
 *
 * <p>For an alternative implementation of the
 * {@link foundation.polar.gratify.artifacts.factory.ListableArtifactFactory} interface,
 * have a look at {@link StaticListableArtifactFactory}, which manages existing
 * artifact instances rather than creating new ones based on artifact definitions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 *
 * @see #registerArtifactDefinition
 * @see #addArtifactPostProcessor
 * @see #getArtifact
 * @see #resolveDependency
 */
@SuppressWarnings("serial")
public class DefaultListableArtifactFactory extends AbstractAutowireCapableArtifactFactory
   implements ConfigurableListableArtifactFactory, ArtifactDefinitionRegistry, Serializable {

   @Nullable
   private static Class<?> javaxInjectProviderClass;

   static {
      try {
         javaxInjectProviderClass =
            ClassUtils.forName("javax.inject.Provider", DefaultListableArtifactFactory.class.getClassLoader());
      }
      catch (ClassNotFoundException ex) {
         // JSR-330 API not available - Provider interface simply not supported then.
         javaxInjectProviderClass = null;
      }
   }


   /** Map from serialized id to factory instance. */
   private static final Map<String, Reference<DefaultListableArtifactFactory>> serializableFactories =
      new ConcurrentHashMap<>(8);

   /** Optional id for this factory, for serialization purposes. */
   @Nullable
   private String serializationId;

   /** Whether to allow re-registration of a different definition with the same name. */
   private boolean allowArtifactDefinitionOverriding = true;

   /** Whether to allow eager class loading even for lazy-init artifacts. */
   private boolean allowEagerClassLoading = true;

   /** Optional OrderComparator for dependency Lists and arrays. */
   @Nullable
   private Comparator<Object> dependencyComparator;

   /** Resolver to use for checking if a artifact definition is an autowire candidate. */
   private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

   /** Map from dependency type to corresponding autowired value. */
   private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

   /** Map of artifact definition objects, keyed by artifact name. */
   private final Map<String, ArtifactDefinition> artifactDefinitionMap = new ConcurrentHashMap<>(256);

   /** Map of singleton and non-singleton artifact names, keyed by dependency type. */
   private final Map<Class<?>, String[]> allArtifactNamesByType = new ConcurrentHashMap<>(64);

   /** Map of singleton-only artifact names, keyed by dependency type. */
   private final Map<Class<?>, String[]> singletonArtifactNamesByType = new ConcurrentHashMap<>(64);

   /** List of artifact definition names, in registration order. */
   private volatile List<String> artifactDefinitionNames = new ArrayList<>(256);

   /** List of names of manually registered singletons, in registration order. */
   private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

   /** Cached array of artifact definition names in case of frozen configuration. */
   @Nullable
   private volatile String[] frozenArtifactDefinitionNames;

   /** Whether artifact definition metadata may be cached for all artifacts. */
   private volatile boolean configurationFrozen = false;


   /**
    * Create a new DefaultListableArtifactFactory.
    */
   public DefaultListableArtifactFactory() {
      super();
   }

   /**
    * Create a new DefaultListableArtifactFactory with the given parent.
    * @param parentArtifactFactory the parent ArtifactFactory
    */
   public DefaultListableArtifactFactory(@Nullable ArtifactFactory parentArtifactFactory) {
      super(parentArtifactFactory);
   }

   /**
    * Specify an id for serialization purposes, allowing this ArtifactFactory to be
    * deserialized from this id back into the ArtifactFactory object, if needed.
    */
   public void setSerializationId(@Nullable String serializationId) {
      if (serializationId != null) {
         serializableFactories.put(serializationId, new WeakReference<>(this));
      }
      else if (this.serializationId != null) {
         serializableFactories.remove(this.serializationId);
      }
      this.serializationId = serializationId;
   }

   /**
    * Return an id for serialization purposes, if specified, allowing this ArtifactFactory
    * to be deserialized from this id back into the ArtifactFactory object, if needed.
    */
   @Nullable
   public String getSerializationId() {
      return this.serializationId;
   }

   /**
    * Set whether it should be allowed to override artifact definitions by registering
    * a different definition with the same name, automatically replacing the former.
    * If not, an exception will be thrown. This also applies to overriding aliases.
    * <p>Default is "true".
    * @see #registerArtifactDefinition
    */
   public void setAllowArtifactDefinitionOverriding(boolean allowArtifactDefinitionOverriding) {
      this.allowArtifactDefinitionOverriding = allowArtifactDefinitionOverriding;
   }

   /**
    * Return whether it should be allowed to override artifact definitions by registering
    * a different definition with the same name, automatically replacing the former.
    */
   public boolean isAllowArtifactDefinitionOverriding() {
      return this.allowArtifactDefinitionOverriding;
   }

   /**
    * Set whether the factory is allowed to eagerly load artifact classes
    * even for artifact definitions that are marked as "lazy-init".
    * <p>Default is "true". Turn this flag off to suppress class loading
    * for lazy-init artifacts unless such a artifact is explicitly requested.
    * In particular, by-type lookups will then simply ignore artifact definitions
    * without resolved class name, instead of loading the artifact classes on
    * demand just to perform a type check.
    * @see AbstractArtifactDefinition#setLazyInit
    */
   public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
      this.allowEagerClassLoading = allowEagerClassLoading;
   }

   /**
    * Return whether the factory is allowed to eagerly load artifact classes
    * even for artifact definitions that are marked as "lazy-init".
    */
   public boolean isAllowEagerClassLoading() {
      return this.allowEagerClassLoading;
   }

   /**
    * Set a {@link java.util.Comparator} for dependency Lists and arrays.
    * @see foundation.polar.gratify.core.OrderComparator
    * @see foundation.polar.gratify.core.annotation.AnnotationAwareOrderComparator
    */
   public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
      this.dependencyComparator = dependencyComparator;
   }

   /**
    * Return the dependency comparator for this ArtifactFactory (may be {@code null}.
    */
   @Nullable
   public Comparator<Object> getDependencyComparator() {
      return this.dependencyComparator;
   }

   /**
    * Set a custom autowire candidate resolver for this ArtifactFactory to use
    * when deciding whether a artifact definition should be considered as a
    * candidate for autowiring.
    */
   public void setAutowireCandidateResolver(final AutowireCandidateResolver autowireCandidateResolver) {
      AssertUtils.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
      if (autowireCandidateResolver instanceof ArtifactFactoryAware) {
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               ((ArtifactFactoryAware) autowireCandidateResolver).setArtifactFactory(DefaultListableArtifactFactory.this);
               return null;
            }, getAccessControlContext());
         }
         else {
            ((ArtifactFactoryAware) autowireCandidateResolver).setArtifactFactory(this);
         }
      }
      this.autowireCandidateResolver = autowireCandidateResolver;
   }

   /**
    * Return the autowire candidate resolver for this ArtifactFactory (never {@code null}).
    */
   public AutowireCandidateResolver getAutowireCandidateResolver() {
      return this.autowireCandidateResolver;
   }

   @Override
   public void copyConfigurationFrom(ConfigurableArtifactFactory otherFactory) {
      super.copyConfigurationFrom(otherFactory);
      if (otherFactory instanceof DefaultListableArtifactFactory) {
         DefaultListableArtifactFactory otherListableFactory = (DefaultListableArtifactFactory) otherFactory;
         this.allowArtifactDefinitionOverriding = otherListableFactory.allowArtifactDefinitionOverriding;
         this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
         this.dependencyComparator = otherListableFactory.dependencyComparator;
         // A clone of the AutowireCandidateResolver since it is potentially ArtifactFactoryAware...
         setAutowireCandidateResolver(
            ArtifactUtils.instantiateClass(otherListableFactory.getAutowireCandidateResolver().getClass()));
         // Make resolvable dependencies (e.g. ResourceLoader) available here as well...
         this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
      }
   }

   //---------------------------------------------------------------------
   // Implementation of remaining ArtifactFactory methods
   //---------------------------------------------------------------------

   @Override
   public <T> T getArtifact(Class<T> requiredType) throws ArtifactsException {
      return getArtifact(requiredType, (Object[]) null);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T getArtifact(Class<T> requiredType, @Nullable Object... args) throws ArtifactsException {
      AssertUtils.notNull(requiredType, "Required type must not be null");
      Object resolved = resolveArtifact(ResolvableType.forRawClass(requiredType), args, false);
      if (resolved == null) {
         throw new NoSuchArtifactDefinitionException(requiredType);
      }
      return (T) resolved;
   }

   @Override
   public <T> ObjectProvider<T> getArtifactProvider(Class<T> requiredType) throws ArtifactsException {
      AssertUtils.notNull(requiredType, "Required type must not be null");
      return getArtifactProvider(ResolvableType.forRawClass(requiredType));
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> ObjectProvider<T> getArtifactProvider(ResolvableType requiredType) {
      return new ArtifactObjectProvider<T>() {
         @Override
         public T getObject() throws ArtifactsException {
            T resolved = resolveArtifact(requiredType, null, false);
            if (resolved == null) {
               throw new NoSuchArtifactDefinitionException(requiredType);
            }
            return resolved;
         }
         @Override
         public T getObject(Object... args) throws ArtifactsException {
            T resolved = resolveArtifact(requiredType, args, false);
            if (resolved == null) {
               throw new NoSuchArtifactDefinitionException(requiredType);
            }
            return resolved;
         }
         @Override
         @Nullable
         public T getIfAvailable() throws ArtifactsException {
            return resolveArtifact(requiredType, null, false);
         }
         @Override
         @Nullable
         public T getIfUnique() throws ArtifactsException {
            return resolveArtifact(requiredType, null, true);
         }
         @Override
         public Stream<T> stream() {
            return Arrays.stream(getArtifactNamesForTypedStream(requiredType))
               .map(name -> (T) getArtifact(name))
               .filter(artifact -> !(artifact instanceof NullArtifact));
         }
         @Override
         public Stream<T> orderedStream() {
            String[] artifactNames = getArtifactNamesForTypedStream(requiredType);
            Map<String, T> matchingArtifacts = new LinkedHashMap<>(artifactNames.length);
            for (String artifactName : artifactNames) {
               Object artifactInstance = getArtifact(artifactName);
               if (!(artifactInstance instanceof NullArtifact)) {
                  matchingArtifacts.put(artifactName, (T) artifactInstance);
               }
            }
            Stream<T> stream = matchingArtifacts.values().stream();
            return stream.sorted(adaptOrderComparator(matchingArtifacts));
         }
      };
   }

   @Nullable
   private <T> T resolveArtifact(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
      NamedArtifactHolder<T> namedArtifact = resolveNamedArtifact(requiredType, args, nonUniqueAsNull);
      if (namedArtifact != null) {
         return namedArtifact.getArtifactInstance();
      }
      ArtifactFactory parent = getParentArtifactFactory();
      if (parent instanceof DefaultListableArtifactFactory) {
         return ((DefaultListableArtifactFactory) parent).resolveArtifact(requiredType, args, nonUniqueAsNull);
      }
      else if (parent != null) {
         ObjectProvider<T> parentProvider = parent.getArtifactProvider(requiredType);
         if (args != null) {
            return parentProvider.getObject(args);
         }
         else {
            return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
         }
      }
      return null;
   }

   private String[] getArtifactNamesForTypedStream(ResolvableType requiredType) {
      return ArtifactFactoryUtils.artifactNamesForTypeIncludingAncestors(this, requiredType);
   }

   //---------------------------------------------------------------------
   // Implementation of ListableArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public boolean containsArtifactDefinition(String artifactName) {
      AssertUtils.notNull(artifactName, "Artifact name must not be null");
      return this.artifactDefinitionMap.containsKey(artifactName);
   }

   @Override
   public int getArtifactDefinitionCount() {
      return this.artifactDefinitionMap.size();
   }

   @Override
   public String[] getArtifactDefinitionNames() {
      String[] frozenNames = this.frozenArtifactDefinitionNames;
      if (frozenNames != null) {
         return frozenNames.clone();
      }
      else {
         return StringUtils.toStringArray(this.artifactDefinitionNames);
      }
   }

   @Override
   public String[] getArtifactNamesForType(ResolvableType type) {
      return getArtifactNamesForType(type, true, true);
   }

   @Override
   public String[] getArtifactNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
      Class<?> resolved = type.resolve();
      if (resolved != null && !type.hasGenerics()) {
         return getArtifactNamesForType(resolved, includeNonSingletons, allowEagerInit);
      }
      else {
         return doGetArtifactNamesForType(type, includeNonSingletons, allowEagerInit);
      }
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable Class<?> type) {
      return getArtifactNamesForType(type, true, true);
   }

   @Override
   public String[] getArtifactNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
      if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
         return doGetArtifactNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
      }
      Map<Class<?>, String[]> cache =
         (includeNonSingletons ? this.allArtifactNamesByType : this.singletonArtifactNamesByType);
      String[] resolvedArtifactNames = cache.get(type);
      if (resolvedArtifactNames != null) {
         return resolvedArtifactNames;
      }
      resolvedArtifactNames = doGetArtifactNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
      if (ClassUtils.isCacheSafe(type, getArtifactClassLoader())) {
         cache.put(type, resolvedArtifactNames);
      }
      return resolvedArtifactNames;
   }

   private String[] doGetArtifactNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
      List<String> result = new ArrayList<>();

      // Check all artifact definitions.
      for (String artifactName : this.artifactDefinitionNames) {
         // Only consider artifact as eligible if the artifact name
         // is not defined as alias for some other artifact.
         if (!isAlias(artifactName)) {
            try {
               RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
               // Only check artifact definition if it is complete.
               if (!mbd.isAbstract() && (allowEagerInit ||
                  (mbd.hasArtifactClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
                     !requiresEagerInitForType(mbd.getFactoryArtifactName()))) {
                  boolean isFactoryArtifact = isFactoryArtifact(artifactName, mbd);
                  ArtifactDefinitionHolder dbd = mbd.getDecoratedDefinition();
                  boolean matchFound = false;
                  boolean allowFactoryArtifactInit = allowEagerInit || containsSingleton(artifactName);
                  boolean isNonLazyDecorated = dbd != null && !mbd.isLazyInit();
                  if (!isFactoryArtifact) {
                     if (includeNonSingletons || isSingleton(artifactName, mbd, dbd)) {
                        matchFound = isTypeMatch(artifactName, type, allowFactoryArtifactInit);
                     }
                  }
                  else  {
                     if (includeNonSingletons || isNonLazyDecorated ||
                        (allowFactoryArtifactInit && isSingleton(artifactName, mbd, dbd))) {
                        matchFound = isTypeMatch(artifactName, type, allowFactoryArtifactInit);
                     }
                     if (!matchFound) {
                        // In case of FactoryArtifact, try to match FactoryArtifact instance itself next.
                        artifactName = FACTORY_BEAN_PREFIX + artifactName;
                        matchFound = isTypeMatch(artifactName, type, allowFactoryArtifactInit);
                     }
                  }
                  if (matchFound) {
                     result.add(artifactName);
                  }
               }
            }
            catch (CannotLoadArtifactClassException | ArtifactDefinitionStoreException ex) {
               if (allowEagerInit) {
                  throw ex;
               }
               // Probably a placeholder: let's ignore it for type matching purposes.
               LogMessage message = (ex instanceof CannotLoadArtifactClassException) ?
                  LogMessage.format("Ignoring artifact class loading failure for artifact '%s'", artifactName) :
                  LogMessage.format("Ignoring unresolvable metadata in artifact definition '%s'", artifactName);
               logger.trace(message, ex);
               onSuppressedException(ex);
            }
         }
      }


      // Check manually registered singletons too.
      for (String artifactName : this.manualSingletonNames) {
         try {
            // In case of FactoryArtifact, match object created by FactoryArtifact.
            if (isFactoryArtifact(artifactName)) {
               if ((includeNonSingletons || isSingleton(artifactName)) && isTypeMatch(artifactName, type)) {
                  result.add(artifactName);
                  // Match found for this artifact: do not match FactoryArtifact itself anymore.
                  continue;
               }
               // In case of FactoryArtifact, try to match FactoryArtifact itself next.
               artifactName = FACTORY_BEAN_PREFIX + artifactName;
            }
            // Match raw artifact instance (might be raw FactoryArtifact).
            if (isTypeMatch(artifactName, type)) {
               result.add(artifactName);
            }
         }
         catch (NoSuchArtifactDefinitionException ex) {
            // Shouldn't happen - probably a result of circular reference resolution...
            logger.trace(LogMessage.format("Failed to check manually registered singleton with name '%s'", artifactName), ex);
         }
      }

      return StringUtils.toStringArray(result);
   }

   private boolean isSingleton(String artifactName, RootArtifactDefinition mbd, @Nullable ArtifactDefinitionHolder dbd) {
      return (dbd != null ? mbd.isSingleton() : isSingleton(artifactName));
   }

   /**
    * Check whether the specified artifact would need to be eagerly initialized
    * in order to determine its type.
    * @param factoryArtifactName a factory-artifact reference that the artifact definition
    * defines a factory method for
    * @return whether eager initialization is necessary
    */
   private boolean requiresEagerInitForType(@Nullable String factoryArtifactName) {
      return (factoryArtifactName != null && isFactoryArtifact(factoryArtifactName) && !containsSingleton(factoryArtifactName));
   }

   @Override
   public <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type) throws ArtifactsException {
      return getArtifactsOfType(type, true, true);
   }

   @Override
   @SuppressWarnings("unchecked")
   public <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException {

      String[] artifactNames = getArtifactNamesForType(type, includeNonSingletons, allowEagerInit);
      Map<String, T> result = new LinkedHashMap<>(artifactNames.length);
      for (String artifactName : artifactNames) {
         try {
            Object artifactInstance = getArtifact(artifactName);
            if (!(artifactInstance instanceof NullArtifact)) {
               result.put(artifactName, (T) artifactInstance);
            }
         }
         catch (ArtifactCreationException ex) {
            Throwable rootCause = ex.getMostSpecificCause();
            if (rootCause instanceof ArtifactCurrentlyInCreationException) {
               ArtifactCreationException bce = (ArtifactCreationException) rootCause;
               String exArtifactName = bce.getArtifactName();
               if (exArtifactName != null && isCurrentlyInCreation(exArtifactName)) {
                  if (logger.isTraceEnabled()) {
                     logger.trace("Ignoring match to currently created artifact '" + exArtifactName + "': " +
                        ex.getMessage());
                  }
                  onSuppressedException(ex);
                  // Ignore: indicates a circular reference when autowiring constructors.
                  // We want to find matches other than the currently created artifact itself.
                  continue;
               }
            }
            throw ex;
         }
      }
      return result;
   }

   @Override
   public String[] getArtifactNamesForAnnotation(Class<? extends Annotation> annotationType) {
      List<String> result = new ArrayList<>();
      for (String artifactName : this.artifactDefinitionNames) {
         ArtifactDefinition artifactDefinition = getArtifactDefinition(artifactName);
         if (!artifactDefinition.isAbstract() && findAnnotationOnArtifact(artifactName, annotationType) != null) {
            result.add(artifactName);
         }
      }
      for (String artifactName : this.manualSingletonNames) {
         if (!result.contains(artifactName) && findAnnotationOnArtifact(artifactName, annotationType) != null) {
            result.add(artifactName);
         }
      }
      return StringUtils.toStringArray(result);
   }

   @Override
   public Map<String, Object> getArtifactsWithAnnotation(Class<? extends Annotation> annotationType) {
      String[] artifactNames = getArtifactNamesForAnnotation(annotationType);
      Map<String, Object> result = new LinkedHashMap<>(artifactNames.length);
      for (String artifactName : artifactNames) {
         Object artifactInstance = getArtifact(artifactName);
         if (!(artifactInstance instanceof NullArtifact)) {
            result.put(artifactName, artifactInstance);
         }
      }
      return result;
   }

   @Override
   @Nullable
   public <A extends Annotation> A findAnnotationOnArtifact(String artifactName, Class<A> annotationType)
      throws NoSuchArtifactDefinitionException {

      return findMergedAnnotationOnArtifact(artifactName, annotationType)
         .synthesize(MergedAnnotation::isPresent).orElse(null);
   }

   private <A extends Annotation> MergedAnnotation<A> findMergedAnnotationOnArtifact(
      String artifactName, Class<A> annotationType) {

      Class<?> artifactType = getType(artifactName);
      if (artifactType != null) {
         MergedAnnotation<A> annotation =
            MergedAnnotations.from(artifactType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(annotationType);
         if (annotation.isPresent()) {
            return annotation;
         }
      }
      if (containsArtifactDefinition(artifactName)) {
         RootArtifactDefinition bd = getMergedLocalArtifactDefinition(artifactName);
         // Check raw artifact class, e.g. in case of a proxy.
         if (bd.hasArtifactClass()) {
            Class<?> artifactClass = bd.getArtifactClass();
            if (artifactClass != artifactType) {
               MergedAnnotation<A> annotation =
                  MergedAnnotations.from(artifactClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(annotationType);
               if (annotation.isPresent()) {
                  return annotation;
               }
            }
         }
         // Check annotations declared on factory method, if any.
         Method factoryMethod = bd.getResolvedFactoryMethod();
         if (factoryMethod != null) {
            MergedAnnotation<A> annotation =
               MergedAnnotations.from(factoryMethod, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(annotationType);
            if (annotation.isPresent()) {
               return annotation;
            }
         }
      }
      return MergedAnnotation.missing();
   }

   //---------------------------------------------------------------------
   // Implementation of ConfigurableListableArtifactFactory interface
   //---------------------------------------------------------------------

   @Override
   public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
      AssertUtils.notNull(dependencyType, "Dependency type must not be null");
      if (autowiredValue != null) {
         if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
            throw new IllegalArgumentException("Value [" + autowiredValue +
               "] does not implement specified dependency type [" + dependencyType.getName() + "]");
         }
         this.resolvableDependencies.put(dependencyType, autowiredValue);
      }
   }

   @Override
   public boolean isAutowireCandidate(String artifactName, DependencyDescriptor descriptor)
      throws NoSuchArtifactDefinitionException {

      return isAutowireCandidate(artifactName, descriptor, getAutowireCandidateResolver());
   }

   /**
    * Determine whether the specified artifact definition qualifies as an autowire candidate,
    * to be injected into other artifacts which declare a dependency of matching type.
    * @param artifactName the name of the artifact definition to check
    * @param descriptor the descriptor of the dependency to resolve
    * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
    * @return whether the artifact should be considered as autowire candidate
    */
   protected boolean isAutowireCandidate(String artifactName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
      throws NoSuchArtifactDefinitionException {

      String artifactDefinitionName = ArtifactFactoryUtils.transformedArtifactName(artifactName);
      if (containsArtifactDefinition(artifactDefinitionName)) {
         return isAutowireCandidate(artifactName, getMergedLocalArtifactDefinition(artifactDefinitionName), descriptor, resolver);
      }
      else if (containsSingleton(artifactName)) {
         return isAutowireCandidate(artifactName, new RootArtifactDefinition(getType(artifactName)), descriptor, resolver);
      }

      ArtifactFactory parent = getParentArtifactFactory();
      if (parent instanceof DefaultListableArtifactFactory) {
         // No artifact definition found in this factory -> delegate to parent.
         return ((DefaultListableArtifactFactory) parent).isAutowireCandidate(artifactName, descriptor, resolver);
      }
      else if (parent instanceof ConfigurableListableArtifactFactory) {
         // If no DefaultListableArtifactFactory, can't pass the resolver along.
         return ((ConfigurableListableArtifactFactory) parent).isAutowireCandidate(artifactName, descriptor);
      }
      else {
         return true;
      }
   }

   /**
    * Determine whether the specified artifact definition qualifies as an autowire candidate,
    * to be injected into other artifacts which declare a dependency of matching type.
    * @param artifactName the name of the artifact definition to check
    * @param mbd the merged artifact definition to check
    * @param descriptor the descriptor of the dependency to resolve
    * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
    * @return whether the artifact should be considered as autowire candidate
    */
   protected boolean isAutowireCandidate(String artifactName, RootArtifactDefinition mbd,
                                         DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

      String artifactDefinitionName = ArtifactFactoryUtils.transformedArtifactName(artifactName);
      resolveArtifactClass(mbd, artifactDefinitionName);
      if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
         new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
      }
      return resolver.isAutowireCandidate(
         new ArtifactDefinitionHolder(mbd, artifactName, getAliases(artifactDefinitionName)), descriptor);
   }

   @Override
   public ArtifactDefinition getArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException {
      ArtifactDefinition bd = this.artifactDefinitionMap.get(artifactName);
      if (bd == null) {
         if (logger.isTraceEnabled()) {
            logger.trace("No artifact named '" + artifactName + "' found in " + this);
         }
         throw new NoSuchArtifactDefinitionException(artifactName);
      }
      return bd;
   }

   @Override
   public Iterator<String> getArtifactNamesIterator() {
      CompositeIterator<String> iterator = new CompositeIterator<>();
      iterator.add(this.artifactDefinitionNames.iterator());
      iterator.add(this.manualSingletonNames.iterator());
      return iterator;
   }

   @Override
   public void clearMetadataCache() {
      super.clearMetadataCache();
      clearByTypeCache();
   }

   @Override
   public void freezeConfiguration() {
      this.configurationFrozen = true;
      this.frozenArtifactDefinitionNames = StringUtils.toStringArray(this.artifactDefinitionNames);
   }

   @Override
   public boolean isConfigurationFrozen() {
      return this.configurationFrozen;
   }

   /**
    * Considers all artifacts as eligible for metadata caching
    * if the factory's configuration has been marked as frozen.
    * @see #freezeConfiguration()
    */
   @Override
   protected boolean isArtifactEligibleForMetadataCaching(String artifactName) {
      return (this.configurationFrozen || super.isArtifactEligibleForMetadataCaching(artifactName));
   }

   @Override
   public void preInstantiateSingletons() throws ArtifactsException {
      if (logger.isTraceEnabled()) {
         logger.trace("Pre-instantiating singletons in " + this);
      }

      // Iterate over a copy to allow for init methods which in turn register new artifact definitions.
      // While this may not be part of the regular factory bootstrap, it does otherwise work fine.
      List<String> artifactNames = new ArrayList<>(this.artifactDefinitionNames);

      // Trigger initialization of all non-lazy singleton artifacts...
      for (String artifactName : artifactNames) {
         RootArtifactDefinition bd = getMergedLocalArtifactDefinition(artifactName);
         if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryArtifact(artifactName)) {
               Object artifact = getArtifact(FACTORY_BEAN_PREFIX + artifactName);
               if (artifact instanceof FactoryArtifact) {
                  final FactoryArtifact<?> factory = (FactoryArtifact<?>) artifact;
                  boolean isEagerInit;
                  if (System.getSecurityManager() != null && factory instanceof SmartFactoryArtifact) {
                     isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>)
                           ((SmartFactoryArtifact<?>) factory)::isEagerInit,
                        getAccessControlContext());
                  }
                  else {
                     isEagerInit = (factory instanceof SmartFactoryArtifact &&
                        ((SmartFactoryArtifact<?>) factory).isEagerInit());
                  }
                  if (isEagerInit) {
                     getArtifact(artifactName);
                  }
               }
            }
            else {
               getArtifact(artifactName);
            }
         }
      }

      // Trigger post-initialization callback for all applicable artifacts...
      for (String artifactName : artifactNames) {
         Object singletonInstance = getSingleton(artifactName);
         if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
            if (System.getSecurityManager() != null) {
               AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                  smartSingleton.afterSingletonsInstantiated();
                  return null;
               }, getAccessControlContext());
            }
            else {
               smartSingleton.afterSingletonsInstantiated();
            }
         }
      }
   }


   //---------------------------------------------------------------------
   // Implementation of ArtifactDefinitionRegistry interface
   //---------------------------------------------------------------------

   @Override
   public void registerArtifactDefinition(String artifactName, ArtifactDefinition artifactDefinition)
      throws ArtifactDefinitionStoreException {

      AssertUtils.hasText(artifactName, "Artifact name must not be empty");
      AssertUtils.notNull(artifactDefinition, "ArtifactDefinition must not be null");

      if (artifactDefinition instanceof AbstractArtifactDefinition) {
         try {
            ((AbstractArtifactDefinition) artifactDefinition).validate();
         }
         catch (ArtifactDefinitionValidationException ex) {
            throw new ArtifactDefinitionStoreException(artifactDefinition.getResourceDescription(), artifactName,
               "Validation of artifact definition failed", ex);
         }
      }

      ArtifactDefinition existingDefinition = this.artifactDefinitionMap.get(artifactName);
      if (existingDefinition != null) {
         if (!isAllowArtifactDefinitionOverriding()) {
            throw new ArtifactDefinitionOverrideException(artifactName, artifactDefinition, existingDefinition);
         }
         else if (existingDefinition.getRole() < artifactDefinition.getRole()) {
            // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
            if (logger.isInfoEnabled()) {
               logger.info("Overriding user-defined artifact definition for artifact '" + artifactName +
                  "' with a framework-generated artifact definition: replacing [" +
                  existingDefinition + "] with [" + artifactDefinition + "]");
            }
         }
         else if (!artifactDefinition.equals(existingDefinition)) {
            if (logger.isDebugEnabled()) {
               logger.debug("Overriding artifact definition for artifact '" + artifactName +
                  "' with a different definition: replacing [" + existingDefinition +
                  "] with [" + artifactDefinition + "]");
            }
         }
         else {
            if (logger.isTraceEnabled()) {
               logger.trace("Overriding artifact definition for artifact '" + artifactName +
                  "' with an equivalent definition: replacing [" + existingDefinition +
                  "] with [" + artifactDefinition + "]");
            }
         }
         this.artifactDefinitionMap.put(artifactName, artifactDefinition);
      }
      else {
         if (hasArtifactCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.artifactDefinitionMap) {
               this.artifactDefinitionMap.put(artifactName, artifactDefinition);
               List<String> updatedDefinitions = new ArrayList<>(this.artifactDefinitionNames.size() + 1);
               updatedDefinitions.addAll(this.artifactDefinitionNames);
               updatedDefinitions.add(artifactName);
               this.artifactDefinitionNames = updatedDefinitions;
               removeManualSingletonName(artifactName);
            }
         }
         else {
            // Still in startup registration phase
            this.artifactDefinitionMap.put(artifactName, artifactDefinition);
            this.artifactDefinitionNames.add(artifactName);
            removeManualSingletonName(artifactName);
         }
         this.frozenArtifactDefinitionNames = null;
      }

      if (existingDefinition != null || containsSingleton(artifactName)) {
         resetArtifactDefinition(artifactName);
      }
   }

   @Override
   public void removeArtifactDefinition(String artifactName) throws NoSuchArtifactDefinitionException {
      AssertUtils.hasText(artifactName, "'artifactName' must not be empty");

      ArtifactDefinition bd = this.artifactDefinitionMap.remove(artifactName);
      if (bd == null) {
         if (logger.isTraceEnabled()) {
            logger.trace("No artifact named '" + artifactName + "' found in " + this);
         }
         throw new NoSuchArtifactDefinitionException(artifactName);
      }

      if (hasArtifactCreationStarted()) {
         // Cannot modify startup-time collection elements anymore (for stable iteration)
         synchronized (this.artifactDefinitionMap) {
            List<String> updatedDefinitions = new ArrayList<>(this.artifactDefinitionNames);
            updatedDefinitions.remove(artifactName);
            this.artifactDefinitionNames = updatedDefinitions;
         }
      }
      else {
         // Still in startup registration phase
         this.artifactDefinitionNames.remove(artifactName);
      }
      this.frozenArtifactDefinitionNames = null;

      resetArtifactDefinition(artifactName);
   }

   /**
    * Reset all artifact definition caches for the given artifact,
    * including the caches of artifacts that are derived from it.
    * <p>Called after an existing artifact definition has been replaced or removed,
    * triggering {@link #clearMergedArtifactDefinition}, {@link #destroySingleton}
    * and {@link MergedArtifactDefinitionPostProcessor#resetArtifactDefinition} on the
    * given artifact and on all artifact definitions that have the given artifact as parent.
    * @param artifactName the name of the artifact to reset
    * @see #registerArtifactDefinition
    * @see #removeArtifactDefinition
    */
   protected void resetArtifactDefinition(String artifactName) {
      // Remove the merged artifact definition for the given artifact, if already created.
      clearMergedArtifactDefinition(artifactName);

      // Remove corresponding artifact from singleton cache, if any. Shouldn't usually
      // be necessary, rather just meant for overriding a context's default artifacts
      // (e.g. the default StaticMessageSource in a StaticApplicationContext).
      destroySingleton(artifactName);

      // Notify all post-processors that the specified artifact definition has been reset.
      for (ArtifactPostProcessor processor : getArtifactPostProcessors()) {
         if (processor instanceof MergedArtifactDefinitionPostProcessor) {
            ((MergedArtifactDefinitionPostProcessor) processor).resetArtifactDefinition(artifactName);
         }
      }

      // Reset all artifact definitions that have the given artifact as parent (recursively).
      for (String bdName : this.artifactDefinitionNames) {
         if (!artifactName.equals(bdName)) {
            ArtifactDefinition bd = this.artifactDefinitionMap.get(bdName);
            // Ensure bd is non-null due to potential concurrent modification
            // of the artifactDefinitionMap.
            if (bd != null && artifactName.equals(bd.getParentName())) {
               resetArtifactDefinition(bdName);
            }
         }
      }
   }

   /**
    * Only allows alias overriding if artifact definition overriding is allowed.
    */
   @Override
   protected boolean allowAliasOverriding() {
      return isAllowArtifactDefinitionOverriding();
   }

   @Override
   public void registerSingleton(String artifactName, Object singletonObject) throws IllegalStateException {
      super.registerSingleton(artifactName, singletonObject);
      updateManualSingletonNames(set -> set.add(artifactName), set -> !this.artifactDefinitionMap.containsKey(artifactName));
      clearByTypeCache();
   }

   @Override
   public void destroySingletons() {
      super.destroySingletons();
      updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
      clearByTypeCache();
   }

   @Override
   public void destroySingleton(String artifactName) {
      super.destroySingleton(artifactName);
      removeManualSingletonName(artifactName);
      clearByTypeCache();
   }

   private void removeManualSingletonName(String artifactName) {
      updateManualSingletonNames(set -> set.remove(artifactName), set -> set.contains(artifactName));
   }

   /**
    * Update the factory's internal set of manual singleton names.
    * @param action the modification action
    * @param condition a precondition for the modification action
    * (if this condition does not apply, the action can be skipped)
    */
   private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
      if (hasArtifactCreationStarted()) {
         // Cannot modify startup-time collection elements anymore (for stable iteration)
         synchronized (this.artifactDefinitionMap) {
            if (condition.test(this.manualSingletonNames)) {
               Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
               action.accept(updatedSingletons);
               this.manualSingletonNames = updatedSingletons;
            }
         }
      }
      else {
         // Still in startup registration phase
         if (condition.test(this.manualSingletonNames)) {
            action.accept(this.manualSingletonNames);
         }
      }
   }

   /**
    * Remove any assumptions about by-type mappings.
    */
   private void clearByTypeCache() {
      this.allArtifactNamesByType.clear();
      this.singletonArtifactNamesByType.clear();
   }


   //---------------------------------------------------------------------
   // Dependency resolution functionality
   //---------------------------------------------------------------------

   @Override
   public <T> NamedArtifactHolder<T> resolveNamedArtifact(Class<T> requiredType) throws ArtifactsException {
      AssertUtils.notNull(requiredType, "Required type must not be null");
      NamedArtifactHolder<T> namedArtifact = resolveNamedArtifact(ResolvableType.forRawClass(requiredType), null, false);
      if (namedArtifact != null) {
         return namedArtifact;
      }
      ArtifactFactory parent = getParentArtifactFactory();
      if (parent instanceof AutowireCapableArtifactFactory) {
         return ((AutowireCapableArtifactFactory) parent).resolveNamedArtifact(requiredType);
      }
      throw new NoSuchArtifactDefinitionException(requiredType);
   }

   @SuppressWarnings("unchecked")
   @Nullable
   private <T> NamedArtifactHolder<T> resolveNamedArtifact(
      ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws ArtifactsException {

      AssertUtils.notNull(requiredType, "Required type must not be null");
      String[] candidateNames = getArtifactNamesForType(requiredType);

      if (candidateNames.length > 1) {
         List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
         for (String artifactName : candidateNames) {
            if (!containsArtifactDefinition(artifactName) || getArtifactDefinition(artifactName).isAutowireCandidate()) {
               autowireCandidates.add(artifactName);
            }
         }
         if (!autowireCandidates.isEmpty()) {
            candidateNames = StringUtils.toStringArray(autowireCandidates);
         }
      }

      if (candidateNames.length == 1) {
         String artifactName = candidateNames[0];
         return new NamedArtifactHolder<>(artifactName, (T) getArtifact(artifactName, requiredType.toClass(), args));
      }
      else if (candidateNames.length > 1) {
         Map<String, Object> candidates = new LinkedHashMap<>(candidateNames.length);
         for (String artifactName : candidateNames) {
            if (containsSingleton(artifactName) && args == null) {
               Object artifactInstance = getArtifact(artifactName);
               candidates.put(artifactName, (artifactInstance instanceof NullArtifact ? null : artifactInstance));
            }
            else {
               candidates.put(artifactName, getType(artifactName));
            }
         }
         String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
         if (candidateName == null) {
            candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
         }
         if (candidateName != null) {
            Object artifactInstance = candidates.get(candidateName);
            if (artifactInstance == null || artifactInstance instanceof Class) {
               artifactInstance = getArtifact(candidateName, requiredType.toClass(), args);
            }
            return new NamedArtifactHolder<>(candidateName, (T) artifactInstance);
         }
         if (!nonUniqueAsNull) {
            throw new NoUniqueArtifactDefinitionException(requiredType, candidates.keySet());
         }
      }

      return null;
   }

   @Override
   @Nullable
   public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingArtifactName,
                                   @Nullable Set<String> autowiredArtifactNames, @Nullable TypeConverter typeConverter) throws ArtifactsException {

      descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
      if (Optional.class == descriptor.getDependencyType()) {
         return createOptionalDependency(descriptor, requestingArtifactName);
      }
      else if (ObjectFactory.class == descriptor.getDependencyType() ||
         ObjectProvider.class == descriptor.getDependencyType()) {
         return new DependencyObjectProvider(descriptor, requestingArtifactName);
      }
      else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
         return new Jsr330Factory().createDependencyProvider(descriptor, requestingArtifactName);
      }
      else {
         Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
            descriptor, requestingArtifactName);
         if (result == null) {
            result = doResolveDependency(descriptor, requestingArtifactName, autowiredArtifactNames, typeConverter);
         }
         return result;
      }
   }

   @Nullable
   public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String artifactName,
                                     @Nullable Set<String> autowiredArtifactNames, @Nullable TypeConverter typeConverter) throws ArtifactsException {

      InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
      try {
         Object shortcut = descriptor.resolveShortcut(this);
         if (shortcut != null) {
            return shortcut;
         }

         Class<?> type = descriptor.getDependencyType();
         Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
         if (value != null) {
            if (value instanceof String) {
               String strVal = resolveEmbeddedValue((String) value);
               ArtifactDefinition bd = (artifactName != null && containsArtifact(artifactName) ?
                  getMergedArtifactDefinition(artifactName) : null);
               value = evaluateArtifactDefinitionString(strVal, bd);
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            try {
               return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
            }
            catch (UnsupportedOperationException ex) {
               // A custom TypeConverter which does not support TypeDescriptor resolution...
               return (descriptor.getField() != null ?
                  converter.convertIfNecessary(value, type, descriptor.getField()) :
                  converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
            }
         }

         Object multipleArtifacts = resolveMultipleArtifacts(descriptor, artifactName, autowiredArtifactNames, typeConverter);
         if (multipleArtifacts != null) {
            return multipleArtifacts;
         }

         Map<String, Object> matchingArtifacts = findAutowireCandidates(artifactName, type, descriptor);
         if (matchingArtifacts.isEmpty()) {
            if (isRequired(descriptor)) {
               raiseNoMatchingArtifactFound(type, descriptor.getResolvableType(), descriptor);
            }
            return null;
         }

         String autowiredArtifactName;
         Object instanceCandidate;

         if (matchingArtifacts.size() > 1) {
            autowiredArtifactName = determineAutowireCandidate(matchingArtifacts, descriptor);
            if (autowiredArtifactName == null) {
               if (isRequired(descriptor) || !indicatesMultipleArtifacts(type)) {
                  return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingArtifacts);
               }
               else {
                  // In case of an optional Collection/Map, silently ignore a non-unique case:
                  // possibly it was meant to be an empty collection of multiple regular artifacts
                  // (before 4.3 in particular when we didn't even look for collection artifacts).
                  return null;
               }
            }
            instanceCandidate = matchingArtifacts.get(autowiredArtifactName);
         }
         else {
            // We have exactly one match.
            Map.Entry<String, Object> entry = matchingArtifacts.entrySet().iterator().next();
            autowiredArtifactName = entry.getKey();
            instanceCandidate = entry.getValue();
         }

         if (autowiredArtifactNames != null) {
            autowiredArtifactNames.add(autowiredArtifactName);
         }
         if (instanceCandidate instanceof Class) {
            instanceCandidate = descriptor.resolveCandidate(autowiredArtifactName, type, this);
         }
         Object result = instanceCandidate;
         if (result instanceof NullArtifact) {
            if (isRequired(descriptor)) {
               raiseNoMatchingArtifactFound(type, descriptor.getResolvableType(), descriptor);
            }
            result = null;
         }
         if (!ClassUtils.isAssignableValue(type, result)) {
            throw new ArtifactNotOfRequiredTypeException(autowiredArtifactName, type, instanceCandidate.getClass());
         }
         return result;
      }
      finally {
         ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
      }
   }

   @Nullable
   private Object resolveMultipleArtifacts(DependencyDescriptor descriptor, @Nullable String artifactName,
                                       @Nullable Set<String> autowiredArtifactNames, @Nullable TypeConverter typeConverter) {

      final Class<?> type = descriptor.getDependencyType();

      if (descriptor instanceof StreamDependencyDescriptor) {
         Map<String, Object> matchingArtifacts = findAutowireCandidates(artifactName, type, descriptor);
         if (autowiredArtifactNames != null) {
            autowiredArtifactNames.addAll(matchingArtifacts.keySet());
         }
         Stream<Object> stream = matchingArtifacts.keySet().stream()
            .map(name -> descriptor.resolveCandidate(name, type, this))
            .filter(artifact -> !(artifact instanceof NullArtifact));
         if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
            stream = stream.sorted(adaptOrderComparator(matchingArtifacts));
         }
         return stream;
      }
      else if (type.isArray()) {
         Class<?> componentType = type.getComponentType();
         ResolvableType resolvableType = descriptor.getResolvableType();
         Class<?> resolvedArrayType = resolvableType.resolve(type);
         if (resolvedArrayType != type) {
            componentType = resolvableType.getComponentType().resolve();
         }
         if (componentType == null) {
            return null;
         }
         Map<String, Object> matchingArtifacts = findAutowireCandidates(artifactName, componentType,
            new MultiElementDescriptor(descriptor));
         if (matchingArtifacts.isEmpty()) {
            return null;
         }
         if (autowiredArtifactNames != null) {
            autowiredArtifactNames.addAll(matchingArtifacts.keySet());
         }
         TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
         Object result = converter.convertIfNecessary(matchingArtifacts.values(), resolvedArrayType);
         if (result instanceof Object[]) {
            Comparator<Object> comparator = adaptDependencyComparator(matchingArtifacts);
            if (comparator != null) {
               Arrays.sort((Object[]) result, comparator);
            }
         }
         return result;
      }
      else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
         Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
         if (elementType == null) {
            return null;
         }
         Map<String, Object> matchingArtifacts = findAutowireCandidates(artifactName, elementType,
            new MultiElementDescriptor(descriptor));
         if (matchingArtifacts.isEmpty()) {
            return null;
         }
         if (autowiredArtifactNames != null) {
            autowiredArtifactNames.addAll(matchingArtifacts.keySet());
         }
         TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
         Object result = converter.convertIfNecessary(matchingArtifacts.values(), type);
         if (result instanceof List) {
            Comparator<Object> comparator = adaptDependencyComparator(matchingArtifacts);
            if (comparator != null) {
               ((List<?>) result).sort(comparator);
            }
         }
         return result;
      }
      else if (Map.class == type) {
         ResolvableType mapType = descriptor.getResolvableType().asMap();
         Class<?> keyType = mapType.resolveGeneric(0);
         if (String.class != keyType) {
            return null;
         }
         Class<?> valueType = mapType.resolveGeneric(1);
         if (valueType == null) {
            return null;
         }
         Map<String, Object> matchingArtifacts = findAutowireCandidates(artifactName, valueType,
            new MultiElementDescriptor(descriptor));
         if (matchingArtifacts.isEmpty()) {
            return null;
         }
         if (autowiredArtifactNames != null) {
            autowiredArtifactNames.addAll(matchingArtifacts.keySet());
         }
         return matchingArtifacts;
      }
      else {
         return null;
      }
   }

   private boolean isRequired(DependencyDescriptor descriptor) {
      return getAutowireCandidateResolver().isRequired(descriptor);
   }

   private boolean indicatesMultipleArtifacts(Class<?> type) {
      return (type.isArray() || (type.isInterface() &&
         (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
   }

   @Nullable
   private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingArtifacts) {
      Comparator<Object> comparator = getDependencyComparator();
      if (comparator instanceof OrderComparator) {
         return ((OrderComparator) comparator).withSourceProvider(
            createFactoryAwareOrderSourceProvider(matchingArtifacts));
      }
      else {
         return comparator;
      }
   }

   private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingArtifacts) {
      Comparator<Object> dependencyComparator = getDependencyComparator();
      OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
         (OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
      return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingArtifacts));
   }

   private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> artifacts) {
      IdentityHashMap<Object, String> instancesToArtifactNames = new IdentityHashMap<>();
      artifacts.forEach((artifactName, instance) -> instancesToArtifactNames.put(instance, artifactName));
      return new FactoryAwareOrderSourceProvider(instancesToArtifactNames);
   }

   /**
    * Find artifact instances that match the required type.
    * Called during autowiring for the specified artifact.
    * @param artifactName the name of the artifact that is about to be wired
    * @param requiredType the actual type of artifact to look for
    * (may be an array component type or collection element type)
    * @param descriptor the descriptor of the dependency to resolve
    * @return a Map of candidate names and candidate instances that match
    * the required type (never {@code null})
    * @throws ArtifactsException in case of errors
    * @see #autowireByType
    * @see #autowireConstructor
    */
   protected Map<String, Object> findAutowireCandidates(
      @Nullable String artifactName, Class<?> requiredType, DependencyDescriptor descriptor) {

      String[] candidateNames = ArtifactFactoryUtils.artifactNamesForTypeIncludingAncestors(
         this, requiredType, true, descriptor.isEager());
      Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
      for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
         Class<?> autowiringType = classObjectEntry.getKey();
         if (autowiringType.isAssignableFrom(requiredType)) {
            Object autowiringValue = classObjectEntry.getValue();
            autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
            if (requiredType.isInstance(autowiringValue)) {
               result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
               break;
            }
         }
      }
      for (String candidate : candidateNames) {
         if (!isSelfReference(artifactName, candidate) && isAutowireCandidate(candidate, descriptor)) {
            addCandidateEntry(result, candidate, descriptor, requiredType);
         }
      }
      if (result.isEmpty()) {
         boolean multiple = indicatesMultipleArtifacts(requiredType);
         // Consider fallback matches if the first pass failed to find anything...
         DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
         for (String candidate : candidateNames) {
            if (!isSelfReference(artifactName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
               (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
               addCandidateEntry(result, candidate, descriptor, requiredType);
            }
         }
         if (result.isEmpty() && !multiple) {
            // Consider self references as a final pass...
            // but in the case of a dependency collection, not the very same artifact itself.
            for (String candidate : candidateNames) {
               if (isSelfReference(artifactName, candidate) &&
                  (!(descriptor instanceof MultiElementDescriptor) || !artifactName.equals(candidate)) &&
                  isAutowireCandidate(candidate, fallbackDescriptor)) {
                  addCandidateEntry(result, candidate, descriptor, requiredType);
               }
            }
         }
      }
      return result;
   }

   /**
    * Add an entry to the candidate map: a artifact instance if available or just the resolved
    * type, preventing early artifact initialization ahead of primary candidate selection.
    */
   private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
                                  DependencyDescriptor descriptor, Class<?> requiredType) {

      if (descriptor instanceof MultiElementDescriptor) {
         Object artifactInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
         if (!(artifactInstance instanceof NullArtifact)) {
            candidates.put(candidateName, artifactInstance);
         }
      }
      else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
         ((StreamDependencyDescriptor) descriptor).isOrdered())) {
         Object artifactInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
         candidates.put(candidateName, (artifactInstance instanceof NullArtifact ? null : artifactInstance));
      }
      else {
         candidates.put(candidateName, getType(candidateName));
      }
   }

   /**
    * Determine the autowire candidate in the given set of artifacts.
    * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
    * @param candidates a Map of candidate names and candidate instances
    * that match the required type, as returned by {@link #findAutowireCandidates}
    * @param descriptor the target dependency to match against
    * @return the name of the autowire candidate, or {@code null} if none found
    */
   @Nullable
   protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
      Class<?> requiredType = descriptor.getDependencyType();
      String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
      if (primaryCandidate != null) {
         return primaryCandidate;
      }
      String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
      if (priorityCandidate != null) {
         return priorityCandidate;
      }
      // Fallback
      for (Map.Entry<String, Object> entry : candidates.entrySet()) {
         String candidateName = entry.getKey();
         Object artifactInstance = entry.getValue();
         if ((artifactInstance != null && this.resolvableDependencies.containsValue(artifactInstance)) ||
            matchesArtifactName(candidateName, descriptor.getDependencyName())) {
            return candidateName;
         }
      }
      return null;
   }

   /**
    * Determine the primary candidate in the given set of artifacts.
    * @param candidates a Map of candidate names and candidate instances
    * (or candidate classes if not created yet) that match the required type
    * @param requiredType the target dependency type to match against
    * @return the name of the primary candidate, or {@code null} if none found
    * @see #isPrimary(String, Object)
    */
   @Nullable
   protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
      String primaryArtifactName = null;
      for (Map.Entry<String, Object> entry : candidates.entrySet()) {
         String candidateArtifactName = entry.getKey();
         Object artifactInstance = entry.getValue();
         if (isPrimary(candidateArtifactName, artifactInstance)) {
            if (primaryArtifactName != null) {
               boolean candidateLocal = containsArtifactDefinition(candidateArtifactName);
               boolean primaryLocal = containsArtifactDefinition(primaryArtifactName);
               if (candidateLocal && primaryLocal) {
                  throw new NoUniqueArtifactDefinitionException(requiredType, candidates.size(),
                     "more than one 'primary' artifact found among candidates: " + candidates.keySet());
               }
               else if (candidateLocal) {
                  primaryArtifactName = candidateArtifactName;
               }
            }
            else {
               primaryArtifactName = candidateArtifactName;
            }
         }
      }
      return primaryArtifactName;
   }

   /**
    * Determine the candidate with the highest priority in the given set of artifacts.
    * <p>Based on {@code @javax.annotation.Priority}. As defined by the related
    * {@link foundation.polar.gratify.core.Ordered} interface, the lowest value has
    * the highest priority.
    * @param candidates a Map of candidate names and candidate instances
    * (or candidate classes if not created yet) that match the required type
    * @param requiredType the target dependency type to match against
    * @return the name of the candidate with the highest priority,
    * or {@code null} if none found
    * @see #getPriority(Object)
    */
   @Nullable
   protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
      String highestPriorityArtifactName = null;
      Integer highestPriority = null;
      for (Map.Entry<String, Object> entry : candidates.entrySet()) {
         String candidateArtifactName = entry.getKey();
         Object artifactInstance = entry.getValue();
         if (artifactInstance != null) {
            Integer candidatePriority = getPriority(artifactInstance);
            if (candidatePriority != null) {
               if (highestPriorityArtifactName != null) {
                  if (candidatePriority.equals(highestPriority)) {
                     throw new NoUniqueArtifactDefinitionException(requiredType, candidates.size(),
                        "Multiple artifacts found with the same priority ('" + highestPriority +
                           "') among candidates: " + candidates.keySet());
                  }
                  else if (candidatePriority < highestPriority) {
                     highestPriorityArtifactName = candidateArtifactName;
                     highestPriority = candidatePriority;
                  }
               }
               else {
                  highestPriorityArtifactName = candidateArtifactName;
                  highestPriority = candidatePriority;
               }
            }
         }
      }
      return highestPriorityArtifactName;
   }

   /**
    * Return whether the artifact definition for the given artifact name has been
    * marked as a primary artifact.
    * @param artifactName the name of the artifact
    * @param artifactInstance the corresponding artifact instance (can be null)
    * @return whether the given artifact qualifies as primary
    */
   protected boolean isPrimary(String artifactName, Object artifactInstance) {
      String transformedArtifactName = transformedArtifactName(artifactName);
      if (containsArtifactDefinition(transformedArtifactName)) {
         return getMergedLocalArtifactDefinition(transformedArtifactName).isPrimary();
      }
      ArtifactFactory parent = getParentArtifactFactory();
      return (parent instanceof DefaultListableArtifactFactory &&
         ((DefaultListableArtifactFactory) parent).isPrimary(transformedArtifactName, artifactInstance));
   }

   /**
    * Return the priority assigned for the given artifact instance by
    * the {@code javax.annotation.Priority} annotation.
    * <p>The default implementation delegates to the specified
    * {@link #setDependencyComparator dependency comparator}, checking its
    * {@link OrderComparator#getPriority method} if it is an extension of
    * Gratify's common {@link OrderComparator} - typically, an
    * {@link foundation.polar.gratify.core.annotation.AnnotationAwareOrderComparator}.
    * If no such comparator is present, this implementation returns {@code null}.
    * @param artifactInstance the artifact instance to check (can be {@code null})
    * @return the priority assigned to that artifact or {@code null} if none is set
    */
   @Nullable
   protected Integer getPriority(Object artifactInstance) {
      Comparator<Object> comparator = getDependencyComparator();
      if (comparator instanceof OrderComparator) {
         return ((OrderComparator) comparator).getPriority(artifactInstance);
      }
      return null;
   }

   /**
    * Determine whether the given candidate name matches the artifact name or the aliases
    * stored in this artifact definition.
    */
   protected boolean matchesArtifactName(String artifactName, @Nullable String candidateName) {
      return (candidateName != null &&
         (candidateName.equals(artifactName) || ObjectUtils.containsElement(getAliases(artifactName), candidateName)));
   }

   /**
    * Determine whether the given artifactName/candidateName pair indicates a self reference,
    * i.e. whether the candidate points back to the original artifact or to a factory method
    * on the original artifact.
    */
   private boolean isSelfReference(@Nullable String artifactName, @Nullable String candidateName) {
      return (artifactName != null && candidateName != null &&
         (artifactName.equals(candidateName) || (containsArtifactDefinition(candidateName) &&
            artifactName.equals(getMergedLocalArtifactDefinition(candidateName).getFactoryArtifactName()))));
   }

   /**
    * Raise a NoSuchArtifactDefinitionException or ArtifactNotOfRequiredTypeException
    * for an unresolvable dependency.
    */
   private void raiseNoMatchingArtifactFound(
      Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws ArtifactsException {

      checkArtifactNotOfRequiredType(type, descriptor);

      throw new NoSuchArtifactDefinitionException(resolvableType,
         "expected at least 1 artifact which qualifies as autowire candidate. " +
            "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
   }

   /**
    * Raise a ArtifactNotOfRequiredTypeException for an unresolvable dependency, if applicable,
    * i.e. if the target type of the artifact would match but an exposed proxy doesn't.
    */
   private void checkArtifactNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
      for (String artifactName : this.artifactDefinitionNames) {
         RootArtifactDefinition mbd = getMergedLocalArtifactDefinition(artifactName);
         Class<?> targetType = mbd.getTargetType();
         if (targetType != null && type.isAssignableFrom(targetType) &&
            isAutowireCandidate(artifactName, mbd, descriptor, getAutowireCandidateResolver())) {
            // Probably a proxy interfering with target type match -> throw meaningful exception.
            Object artifactInstance = getSingleton(artifactName, false);
            Class<?> artifactType = (artifactInstance != null && artifactInstance.getClass() != NullArtifact.class ?
               artifactInstance.getClass() : predictArtifactType(artifactName, mbd));
            if (artifactType != null && !type.isAssignableFrom(artifactType)) {
               throw new ArtifactNotOfRequiredTypeException(artifactName, type, artifactType);
            }
         }
      }

      ArtifactFactory parent = getParentArtifactFactory();
      if (parent instanceof DefaultListableArtifactFactory) {
         ((DefaultListableArtifactFactory) parent).checkArtifactNotOfRequiredType(type, descriptor);
      }
   }

   /**
    * Create an {@link Optional} wrapper for the specified dependency.
    */
   private Optional<?> createOptionalDependency(
      DependencyDescriptor descriptor, @Nullable String artifactName, final Object... args) {

      DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
         @Override
         public boolean isRequired() {
            return false;
         }
         @Override
         public Object resolveCandidate(String artifactName, Class<?> requiredType, ArtifactFactory artifactFactory) {
            return (!ObjectUtils.isEmpty(args) ? artifactFactory.getArtifact(artifactName, args) :
               super.resolveCandidate(artifactName, requiredType, artifactFactory));
         }
      };
      Object result = doResolveDependency(descriptorToUse, artifactName, null, null);
      return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
   }


   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
      sb.append(": defining artifacts [");
      sb.append(StringUtils.collectionToCommaDelimitedString(this.artifactDefinitionNames));
      sb.append("]; ");
      ArtifactFactory parent = getParentArtifactFactory();
      if (parent == null) {
         sb.append("root of factory hierarchy");
      }
      else {
         sb.append("parent: ").append(ObjectUtils.identityToString(parent));
      }
      return sb.toString();
   }


   //---------------------------------------------------------------------
   // Serialization support
   //---------------------------------------------------------------------

   private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      throw new NotSerializableException("DefaultListableArtifactFactory itself is not deserializable - " +
         "just a SerializedArtifactFactoryReference is");
   }

   protected Object writeReplace() throws ObjectStreamException {
      if (this.serializationId != null) {
         return new SerializedArtifactFactoryReference(this.serializationId);
      }
      else {
         throw new NotSerializableException("DefaultListableArtifactFactory has no serialization id");
      }
   }


   /**
    * Minimal id reference to the factory.
    * Resolved to the actual factory instance on deserialization.
    */
   private static class SerializedArtifactFactoryReference implements Serializable {

      private final String id;

      public SerializedArtifactFactoryReference(String id) {
         this.id = id;
      }

      private Object readResolve() {
         Reference<?> ref = serializableFactories.get(this.id);
         if (ref != null) {
            Object result = ref.get();
            if (result != null) {
               return result;
            }
         }
         // Lenient fallback: dummy factory in case of original factory not found...
         DefaultListableArtifactFactory dummyFactory = new DefaultListableArtifactFactory();
         dummyFactory.serializationId = this.id;
         return dummyFactory;
      }
   }


   /**
    * A dependency descriptor marker for nested elements.
    */
   private static class NestedDependencyDescriptor extends DependencyDescriptor {

      public NestedDependencyDescriptor(DependencyDescriptor original) {
         super(original);
         increaseNestingLevel();
      }
   }


   /**
    * A dependency descriptor for a multi-element declaration with nested elements.
    */
   private static class MultiElementDescriptor extends NestedDependencyDescriptor {

      public MultiElementDescriptor(DependencyDescriptor original) {
         super(original);
      }
   }


   /**
    * A dependency descriptor marker for stream access to multiple elements.
    */
   private static class StreamDependencyDescriptor extends DependencyDescriptor {

      private final boolean ordered;

      public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
         super(original);
         this.ordered = ordered;
      }

      public boolean isOrdered() {
         return this.ordered;
      }
   }


   private interface ArtifactObjectProvider<T> extends ObjectProvider<T>, Serializable {
   }


   /**
    * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
    */
   private class DependencyObjectProvider implements ArtifactObjectProvider<Object> {

      private final DependencyDescriptor descriptor;

      private final boolean optional;

      @Nullable
      private final String artifactName;

      public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String artifactName) {
         this.descriptor = new NestedDependencyDescriptor(descriptor);
         this.optional = (this.descriptor.getDependencyType() == Optional.class);
         this.artifactName = artifactName;
      }

      @Override
      public Object getObject() throws ArtifactsException {
         if (this.optional) {
            return createOptionalDependency(this.descriptor, this.artifactName);
         }
         else {
            Object result = doResolveDependency(this.descriptor, this.artifactName, null, null);
            if (result == null) {
               throw new NoSuchArtifactDefinitionException(this.descriptor.getResolvableType());
            }
            return result;
         }
      }

      @Override
      public Object getObject(final Object... args) throws ArtifactsException {
         if (this.optional) {
            return createOptionalDependency(this.descriptor, this.artifactName, args);
         }
         else {
            DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
               @Override
               public Object resolveCandidate(String artifactName, Class<?> requiredType, ArtifactFactory artifactFactory) {
                  return artifactFactory.getArtifact(artifactName, args);
               }
            };
            Object result = doResolveDependency(descriptorToUse, this.artifactName, null, null);
            if (result == null) {
               throw new NoSuchArtifactDefinitionException(this.descriptor.getResolvableType());
            }
            return result;
         }
      }

      @Override
      @Nullable
      public Object getIfAvailable() throws ArtifactsException {
         if (this.optional) {
            return createOptionalDependency(this.descriptor, this.artifactName);
         }
         else {
            DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
               @Override
               public boolean isRequired() {
                  return false;
               }
            };
            return doResolveDependency(descriptorToUse, this.artifactName, null, null);
         }
      }

      @Override
      @Nullable
      public Object getIfUnique() throws ArtifactsException {
         DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
            @Override
            public boolean isRequired() {
               return false;
            }
            @Override
            @Nullable
            public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingArtifacts) {
               return null;
            }
         };
         if (this.optional) {
            return createOptionalDependency(descriptorToUse, this.artifactName);
         }
         else {
            return doResolveDependency(descriptorToUse, this.artifactName, null, null);
         }
      }

      @Nullable
      protected Object getValue() throws ArtifactsException {
         if (this.optional) {
            return createOptionalDependency(this.descriptor, this.artifactName);
         }
         else {
            return doResolveDependency(this.descriptor, this.artifactName, null, null);
         }
      }

      @Override
      public Stream<Object> stream() {
         return resolveStream(false);
      }

      @Override
      public Stream<Object> orderedStream() {
         return resolveStream(true);
      }

      @SuppressWarnings("unchecked")
      private Stream<Object> resolveStream(boolean ordered) {
         DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
         Object result = doResolveDependency(descriptorToUse, this.artifactName, null, null);
         return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
      }
   }


   /**
    * Separate inner class for avoiding a hard dependency on the {@code javax.inject} API.
    * Actual {@code javax.inject.Provider} implementation is nested here in order to make it
    * invisible for Graal's introspection of DefaultListableArtifactFactory's nested classes.
    */
   private class Jsr330Factory implements Serializable {

      public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String artifactName) {
         return new Jsr330Provider(descriptor, artifactName);
      }

      private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

         public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String artifactName) {
            super(descriptor, artifactName);
         }

         @Override
         @Nullable
         public Object get() throws ArtifactsException {
            return getValue();
         }
      }
   }

   /**
    * An {@link foundation.polar.gratify.core.OrderComparator.OrderSourceProvider} implementation
    * that is aware of the artifact metadata of the instances to sort.
    * <p>Lookup for the method factory of an instance to sort, if any, and let the
    * comparator retrieve the {@link foundation.polar.gratify.core.annotation.Order}
    * value defined on it. This essentially allows for the following construct:
    */
   private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

      private final Map<Object, String> instancesToArtifactNames;

      public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToArtifactNames) {
         this.instancesToArtifactNames = instancesToArtifactNames;
      }

      @Override
      @Nullable
      public Object getOrderSource(Object obj) {
         String artifactName = this.instancesToArtifactNames.get(obj);
         if (artifactName == null || !containsArtifactDefinition(artifactName)) {
            return null;
         }
         RootArtifactDefinition artifactDefinition = getMergedLocalArtifactDefinition(artifactName);
         List<Object> sources = new ArrayList<>(2);
         Method factoryMethod = artifactDefinition.getResolvedFactoryMethod();
         if (factoryMethod != null) {
            sources.add(factoryMethod);
         }
         Class<?> targetType = artifactDefinition.getTargetType();
         if (targetType != null && targetType != obj.getClass()) {
            sources.add(targetType);
         }
         return sources.toArray();
      }
   }
}
