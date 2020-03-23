package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.SingletonArtifactRegistry;
import foundation.polar.gratify.core.SimpleAliasRegistry;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link foundation.polar.gratify.artifacts.factory.config.SingletonArtifactRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link foundation.polar.gratify.artifacts.factory.DisposableArtifact} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory}
 * interface extends the {@link SingletonArtifactRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractArtifactFactory} and {@link DefaultListableArtifactFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 *
 * @see #registerSingleton
 * @see #registerDisposableArtifact
 * @see foundation.polar.gratify.artifacts.factory.DisposableArtifact
 * @see foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory
 */
public class DefaultSingletonArtifactRegistry extends SimpleAliasRegistry implements SingletonArtifactRegistry {
   /** Cache of singleton objects: bean name to bean instance. */
   private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

   /** Cache of singleton factories: bean name to ObjectFactory. */
   private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

   /** Cache of early singleton objects: bean name to bean instance. */
   private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);

   /** Set of registered singletons, containing the bean names in registration order. */
   private final Set<String> registeredSingletons = new LinkedHashSet<>(256);

   /** Names of beans that are currently in creation. */
   private final Set<String> singletonsCurrentlyInCreation =
      Collections.newSetFromMap(new ConcurrentHashMap<>(16));

   /** Names of beans currently excluded from in creation checks. */
   private final Set<String> inCreationCheckExclusions =
      Collections.newSetFromMap(new ConcurrentHashMap<>(16));

   /** List of suppressed Exceptions, available for associating related causes. */
   @Nullable
   private Set<Exception> suppressedExceptions;

   /** Flag that indicates whether we're currently within destroySingletons. */
   private boolean singletonsCurrentlyInDestruction = false;

   /** Disposable bean instances: bean name to disposable instance. */
   private final Map<String, Object> disposableArtifacts = new LinkedHashMap<>();

   /** Map between containing bean names: bean name to Set of bean names that the bean contains. */
   private final Map<String, Set<String>> containedArtifactMap = new ConcurrentHashMap<>(16);

   /** Map between dependent bean names: bean name to Set of dependent bean names. */
   private final Map<String, Set<String>> dependentArtifactMap = new ConcurrentHashMap<>(64);

   /** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
   private final Map<String, Set<String>> dependenciesForArtifactMap = new ConcurrentHashMap<>(64);


   @Override
   public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
      AssertUtils.notNull(beanName, "Artifact name must not be null");
      AssertUtils.notNull(singletonObject, "Singleton object must not be null");
      synchronized (this.singletonObjects) {
         Object oldObject = this.singletonObjects.get(beanName);
         if (oldObject != null) {
            throw new IllegalStateException("Could not register object [" + singletonObject +
               "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
         }
         addSingleton(beanName, singletonObject);
      }
   }

   /**
    * Add the given singleton object to the singleton cache of this factory.
    * <p>To be called for eager registration of singletons.
    * @param beanName the name of the bean
    * @param singletonObject the singleton object
    */
   protected void addSingleton(String beanName, Object singletonObject) {
      synchronized (this.singletonObjects) {
         this.singletonObjects.put(beanName, singletonObject);
         this.singletonFactories.remove(beanName);
         this.earlySingletonObjects.remove(beanName);
         this.registeredSingletons.add(beanName);
      }
   }

   /**
    * Add the given singleton factory for building the specified singleton
    * if necessary.
    * <p>To be called for eager registration of singletons, e.g. to be able to
    * resolve circular references.
    * @param beanName the name of the bean
    * @param singletonFactory the factory for the singleton object
    */
   protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
      AssertUtils.notNull(singletonFactory, "Singleton factory must not be null");
      synchronized (this.singletonObjects) {
         if (!this.singletonObjects.containsKey(beanName)) {
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.add(beanName);
         }
      }
   }

   @Override
   @Nullable
   public Object getSingleton(String beanName) {
      return getSingleton(beanName, true);
   }

   /**
    * Return the (raw) singleton object registered under the given name.
    * <p>Checks already instantiated singletons and also allows for an early
    * reference to a currently created singleton (resolving a circular reference).
    * @param beanName the name of the bean to look for
    * @param allowEarlyReference whether early references should be created or not
    * @return the registered singleton object, or {@code null} if none found
    */
   @Nullable
   protected Object getSingleton(String beanName, boolean allowEarlyReference) {
      Object singletonObject = this.singletonObjects.get(beanName);
      if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
         synchronized (this.singletonObjects) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
               ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
               if (singletonFactory != null) {
                  singletonObject = singletonFactory.getObject();
                  this.earlySingletonObjects.put(beanName, singletonObject);
                  this.singletonFactories.remove(beanName);
               }
            }
         }
      }
      return singletonObject;
   }

   /**
    * Return the (raw) singleton object registered under the given name,
    * creating and registering a new one if none registered yet.
    * @param beanName the name of the bean
    * @param singletonFactory the ObjectFactory to lazily create the singleton
    * with, if necessary
    * @return the registered singleton object
    */
   public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
      AssertUtils.notNull(beanName, "Artifact name must not be null");
      synchronized (this.singletonObjects) {
         Object singletonObject = this.singletonObjects.get(beanName);
         if (singletonObject == null) {
            if (this.singletonsCurrentlyInDestruction) {
               throw new ArtifactCreationNotAllowedException(beanName,
                  "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                     "(Do not request a bean from a ArtifactFactory in a destroy method implementation!)");
            }
            if (logger.isDebugEnabled()) {
               logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
            }
            beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
            if (recordSuppressedExceptions) {
               this.suppressedExceptions = new LinkedHashSet<>();
            }
            try {
               singletonObject = singletonFactory.getObject();
               newSingleton = true;
            }
            catch (IllegalStateException ex) {
               // Has the singleton object implicitly appeared in the meantime ->
               // if yes, proceed with it since the exception indicates that state.
               singletonObject = this.singletonObjects.get(beanName);
               if (singletonObject == null) {
                  throw ex;
               }
            }
            catch (ArtifactCreationException ex) {
               if (recordSuppressedExceptions) {
                  for (Exception suppressedException : this.suppressedExceptions) {
                     ex.addRelatedCause(suppressedException);
                  }
               }
               throw ex;
            }
            finally {
               if (recordSuppressedExceptions) {
                  this.suppressedExceptions = null;
               }
               afterSingletonCreation(beanName);
            }
            if (newSingleton) {
               addSingleton(beanName, singletonObject);
            }
         }
         return singletonObject;
      }
   }

   /**
    * Register an Exception that happened to get suppressed during the creation of a
    * singleton bean instance, e.g. a temporary circular reference resolution problem.
    * @param ex the Exception to register
    */
   protected void onSuppressedException(Exception ex) {
      synchronized (this.singletonObjects) {
         if (this.suppressedExceptions != null) {
            this.suppressedExceptions.add(ex);
         }
      }
   }

   /**
    * Remove the bean with the given name from the singleton cache of this factory,
    * to be able to clean up eager registration of a singleton if creation failed.
    * @param beanName the name of the bean
    * @see #getSingletonMutex()
    */
   protected void removeSingleton(String beanName) {
      synchronized (this.singletonObjects) {
         this.singletonObjects.remove(beanName);
         this.singletonFactories.remove(beanName);
         this.earlySingletonObjects.remove(beanName);
         this.registeredSingletons.remove(beanName);
      }
   }

   @Override
   public boolean containsSingleton(String beanName) {
      return this.singletonObjects.containsKey(beanName);
   }

   @Override
   public String[] getSingletonNames() {
      synchronized (this.singletonObjects) {
         return StringUtils.toStringArray(this.registeredSingletons);
      }
   }

   @Override
   public int getSingletonCount() {
      synchronized (this.singletonObjects) {
         return this.registeredSingletons.size();
      }
   }


   public void setCurrentlyInCreation(String beanName, boolean inCreation) {
      AssertUtils.notNull(beanName, "Artifact name must not be null");
      if (!inCreation) {
         this.inCreationCheckExclusions.add(beanName);
      }
      else {
         this.inCreationCheckExclusions.remove(beanName);
      }
   }

   public boolean isCurrentlyInCreation(String beanName) {
      AssertUtils.notNull(beanName, "Artifact name must not be null");
      return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
   }

   protected boolean isActuallyInCreation(String beanName) {
      return isSingletonCurrentlyInCreation(beanName);
   }

   /**
    * Return whether the specified singleton bean is currently in creation
    * (within the entire factory).
    * @param beanName the name of the bean
    */
   public boolean isSingletonCurrentlyInCreation(String beanName) {
      return this.singletonsCurrentlyInCreation.contains(beanName);
   }

   /**
    * Callback before singleton creation.
    * <p>The default implementation register the singleton as currently in creation.
    * @param beanName the name of the singleton about to be created
    * @see #isSingletonCurrentlyInCreation
    */
   protected void beforeSingletonCreation(String beanName) {
      if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
         throw new ArtifactCurrentlyInCreationException(beanName);
      }
   }

   /**
    * Callback after singleton creation.
    * <p>The default implementation marks the singleton as not in creation anymore.
    * @param beanName the name of the singleton that has been created
    * @see #isSingletonCurrentlyInCreation
    */
   protected void afterSingletonCreation(String beanName) {
      if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
         throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
      }
   }

   /**
    * Add the given bean to the list of disposable beans in this registry.
    * <p>Disposable beans usually correspond to registered singletons,
    * matching the bean name but potentially being a different instance
    * (for example, a DisposableArtifact adapter for a singleton that does not
    * naturally implement Gratify's DisposableArtifact interface).
    * @param beanName the name of the bean
    * @param bean the bean instance
    */
   public void registerDisposableArtifact(String beanName, DisposableArtifact bean) {
      synchronized (this.disposableArtifacts) {
         this.disposableArtifacts.put(beanName, bean);
      }
   }

   /**
    * Register a containment relationship between two beans,
    * e.g. between an inner bean and its containing outer bean.
    * <p>Also registers the containing bean as dependent on the contained bean
    * in terms of destruction order.
    * @param containedArtifactName the name of the contained (inner) bean
    * @param containingArtifactName the name of the containing (outer) bean
    * @see #registerDependentArtifact
    */
   public void registerContainedArtifact(String containedArtifactName, String containingArtifactName) {
      synchronized (this.containedArtifactMap) {
         Set<String> containedArtifacts =
            this.containedArtifactMap.computeIfAbsent(containingArtifactName, k -> new LinkedHashSet<>(8));
         if (!containedArtifacts.add(containedArtifactName)) {
            return;
         }
      }
      registerDependentArtifact(containedArtifactName, containingArtifactName);
   }

   /**
    * Register a dependent bean for the given bean,
    * to be destroyed before the given bean is destroyed.
    * @param beanName the name of the bean
    * @param dependentArtifactName the name of the dependent bean
    */
   public void registerDependentArtifact(String beanName, String dependentArtifactName) {
      String canonicalName = canonicalName(beanName);

      synchronized (this.dependentArtifactMap) {
         Set<String> dependentArtifacts =
            this.dependentArtifactMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
         if (!dependentArtifacts.add(dependentArtifactName)) {
            return;
         }
      }

      synchronized (this.dependenciesForArtifactMap) {
         Set<String> dependenciesForArtifact =
            this.dependenciesForArtifactMap.computeIfAbsent(dependentArtifactName, k -> new LinkedHashSet<>(8));
         dependenciesForArtifact.add(canonicalName);
      }
   }

   /**
    * Determine whether the specified dependent bean has been registered as
    * dependent on the given bean or on any of its transitive dependencies.
    * @param beanName the name of the bean to check
    * @param dependentArtifactName the name of the dependent bean
    */
   protected boolean isDependent(String beanName, String dependentArtifactName) {
      synchronized (this.dependentArtifactMap) {
         return isDependent(beanName, dependentArtifactName, null);
      }
   }

   private boolean isDependent(String beanName, String dependentArtifactName, @Nullable Set<String> alreadySeen) {
      if (alreadySeen != null && alreadySeen.contains(beanName)) {
         return false;
      }
      String canonicalName = canonicalName(beanName);
      Set<String> dependentArtifacts = this.dependentArtifactMap.get(canonicalName);
      if (dependentArtifacts == null) {
         return false;
      }
      if (dependentArtifacts.contains(dependentArtifactName)) {
         return true;
      }
      for (String transitiveDependency : dependentArtifacts) {
         if (alreadySeen == null) {
            alreadySeen = new HashSet<>();
         }
         alreadySeen.add(beanName);
         if (isDependent(transitiveDependency, dependentArtifactName, alreadySeen)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine whether a dependent bean has been registered for the given name.
    * @param beanName the name of the bean to check
    */
   protected boolean hasDependentArtifact(String beanName) {
      return this.dependentArtifactMap.containsKey(beanName);
   }

   /**
    * Return the names of all beans which depend on the specified bean, if any.
    * @param beanName the name of the bean
    * @return the array of dependent bean names, or an empty array if none
    */
   public String[] getDependentArtifacts(String beanName) {
      Set<String> dependentArtifacts = this.dependentArtifactMap.get(beanName);
      if (dependentArtifacts == null) {
         return new String[0];
      }
      synchronized (this.dependentArtifactMap) {
         return StringUtils.toStringArray(dependentArtifacts);
      }
   }

   /**
    * Return the names of all beans that the specified bean depends on, if any.
    * @param beanName the name of the bean
    * @return the array of names of beans which the bean depends on,
    * or an empty array if none
    */
   public String[] getDependenciesForArtifact(String beanName) {
      Set<String> dependenciesForArtifact = this.dependenciesForArtifactMap.get(beanName);
      if (dependenciesForArtifact == null) {
         return new String[0];
      }
      synchronized (this.dependenciesForArtifactMap) {
         return StringUtils.toStringArray(dependenciesForArtifact);
      }
   }

   public void destroySingletons() {
      if (logger.isTraceEnabled()) {
         logger.trace("Destroying singletons in " + this);
      }
      synchronized (this.singletonObjects) {
         this.singletonsCurrentlyInDestruction = true;
      }

      String[] disposableArtifactNames;
      synchronized (this.disposableArtifacts) {
         disposableArtifactNames = StringUtils.toStringArray(this.disposableArtifacts.keySet());
      }
      for (int i = disposableArtifactNames.length - 1; i >= 0; i--) {
         destroySingleton(disposableArtifactNames[i]);
      }

      this.containedArtifactMap.clear();
      this.dependentArtifactMap.clear();
      this.dependenciesForArtifactMap.clear();

      clearSingletonCache();
   }

   /**
    * Clear all cached singleton instances in this registry.
    */
   protected void clearSingletonCache() {
      synchronized (this.singletonObjects) {
         this.singletonObjects.clear();
         this.singletonFactories.clear();
         this.earlySingletonObjects.clear();
         this.registeredSingletons.clear();
         this.singletonsCurrentlyInDestruction = false;
      }
   }

   /**
    * Destroy the given bean. Delegates to {@code destroyArtifact}
    * if a corresponding disposable bean instance is found.
    * @param beanName the name of the bean
    * @see #destroyArtifact
    */
   public void destroySingleton(String beanName) {
      // Remove a registered singleton of the given name, if any.
      removeSingleton(beanName);

      // Destroy the corresponding DisposableArtifact instance.
      DisposableArtifact disposableArtifact;
      synchronized (this.disposableArtifacts) {
         disposableArtifact = (DisposableArtifact) this.disposableArtifacts.remove(beanName);
      }
      destroyArtifact(beanName, disposableArtifact);
   }

   /**
    * Destroy the given bean. Must destroy beans that depend on the given
    * bean before the bean itself. Should not throw any exceptions.
    * @param beanName the name of the bean
    * @param bean the bean instance to destroy
    */
   protected void destroyArtifact(String beanName, @Nullable DisposableArtifact bean) {
      // Trigger destruction of dependent beans first...
      Set<String> dependencies;
      synchronized (this.dependentArtifactMap) {
         // Within full synchronization in order to guarantee a disconnected Set
         dependencies = this.dependentArtifactMap.remove(beanName);
      }
      if (dependencies != null) {
         if (logger.isTraceEnabled()) {
            logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
         }
         for (String dependentArtifactName : dependencies) {
            destroySingleton(dependentArtifactName);
         }
      }

      // Actually destroy the bean now...
      if (bean != null) {
         try {
            bean.destroy();
         }
         catch (Throwable ex) {
            if (logger.isWarnEnabled()) {
               logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
            }
         }
      }

      // Trigger destruction of contained beans...
      Set<String> containedArtifacts;
      synchronized (this.containedArtifactMap) {
         // Within full synchronization in order to guarantee a disconnected Set
         containedArtifacts = this.containedArtifactMap.remove(beanName);
      }
      if (containedArtifacts != null) {
         for (String containedArtifactName : containedArtifacts) {
            destroySingleton(containedArtifactName);
         }
      }

      // Remove destroyed bean from other beans' dependencies.
      synchronized (this.dependentArtifactMap) {
         for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentArtifactMap.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Set<String>> entry = it.next();
            Set<String> dependenciesToClean = entry.getValue();
            dependenciesToClean.remove(beanName);
            if (dependenciesToClean.isEmpty()) {
               it.remove();
            }
         }
      }

      // Remove destroyed bean's prepared dependency information.
      this.dependenciesForArtifactMap.remove(beanName);
   }

   /**
    * Exposes the singleton mutex to subclasses and external collaborators.
    * <p>Subclasses should synchronize on the given Object if they perform
    * any sort of extended singleton creation phase. In particular, subclasses
    * should <i>not</i> have their own mutexes involved in singleton creation,
    * to avoid the potential for deadlocks in lazy-init situations.
    */
   @Override
   public final Object getSingletonMutex() {
      return this.singletonObjects;
   }
}
