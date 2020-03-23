package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactCreationException;
import foundation.polar.gratify.artifacts.factory.ArtifactCurrentlyInCreationException;
import foundation.polar.gratify.artifacts.factory.FactoryArtifact;
import foundation.polar.gratify.artifacts.factory.FactoryArtifactNotInitializedException;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support base class for singleton registries which need to handle
 * {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} instances,
 * integrated with {@link DefaultSingletonArtifactRegistry}'s singleton management.
 *
 * <p>Serves as base class for {@link AbstractArtifactFactory}.
 *
 * @author Juergen Hoeller
 */
public abstract class FactoryArtifactRegistrySupport extends DefaultSingletonArtifactRegistry {
   /** Cache of singleton objects created by FactoryArtifacts: FactoryArtifact name to object. */
   private final Map<String, Object> factoryArtifactObjectCache = new ConcurrentHashMap<>(16);

   /**
    * Determine the type for the given FactoryArtifact.
    * @param factoryArtifact the FactoryArtifact instance to check
    * @return the FactoryArtifact's object type,
    * or {@code null} if the type cannot be determined yet
    */
   @Nullable
   protected Class<?> getTypeForFactoryArtifact(final FactoryArtifact<?> factoryArtifact) {
      try {
         if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Class<?>>)
               factoryArtifact::getObjectType, getAccessControlContext());
         }
         else {
            return factoryArtifact.getObjectType();
         }
      }
      catch (Throwable ex) {
         // Thrown from the FactoryArtifact's getObjectType implementation.
         logger.info("FactoryArtifact threw exception from getObjectType, despite the contract saying " +
            "that it should return null if the type of its object cannot be determined yet", ex);
         return null;
      }
   }

   /**
    * Obtain an object to expose from the given FactoryArtifact, if available
    * in cached form. Quick check for minimal synchronization.
    * @param artifactName the name of the bean
    * @return the object obtained from the FactoryArtifact,
    * or {@code null} if not available
    */
   @Nullable
   protected Object getCachedObjectForFactoryArtifact(String artifactName) {
      return this.factoryArtifactObjectCache.get(artifactName);
   }

   /**
    * Obtain an object to expose from the given FactoryArtifact.
    * @param factory the FactoryArtifact instance
    * @param artifactName the name of the bean
    * @param shouldPostProcess whether the bean is subject to post-processing
    * @return the object obtained from the FactoryArtifact
    * @throws ArtifactCreationException if FactoryArtifact object creation failed
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact#getObject()
    */
   protected Object getObjectFromFactoryArtifact(FactoryArtifact<?> factory, String artifactName, boolean shouldPostProcess) {
      if (factory.isSingleton() && containsSingleton(artifactName)) {
         synchronized (getSingletonMutex()) {
            Object object = this.factoryArtifactObjectCache.get(artifactName);
            if (object == null) {
               object = doGetObjectFromFactoryArtifact(factory, artifactName);
               // Only post-process and store if not put there already during getObject() call above
               // (e.g. because of circular reference processing triggered by custom getArtifact calls)
               Object alreadyThere = this.factoryArtifactObjectCache.get(artifactName);
               if (alreadyThere != null) {
                  object = alreadyThere;
               }
               else {
                  if (shouldPostProcess) {
                     if (isSingletonCurrentlyInCreation(artifactName)) {
                        // Temporarily return non-post-processed object, not storing it yet..
                        return object;
                     }
                     beforeSingletonCreation(artifactName);
                     try {
                        object = postProcessObjectFromFactoryArtifact(object, artifactName);
                     }
                     catch (Throwable ex) {
                        throw new ArtifactCreationException(artifactName,
                           "Post-processing of FactoryArtifact's singleton object failed", ex);
                     }
                     finally {
                        afterSingletonCreation(artifactName);
                     }
                  }
                  if (containsSingleton(artifactName)) {
                     this.factoryArtifactObjectCache.put(artifactName, object);
                  }
               }
            }
            return object;
         }
      }
      else {
         Object object = doGetObjectFromFactoryArtifact(factory, artifactName);
         if (shouldPostProcess) {
            try {
               object = postProcessObjectFromFactoryArtifact(object, artifactName);
            }
            catch (Throwable ex) {
               throw new ArtifactCreationException(artifactName, "Post-processing of FactoryArtifact's object failed", ex);
            }
         }
         return object;
      }
   }

   /**
    * Obtain an object to expose from the given FactoryArtifact.
    * @param factory the FactoryArtifact instance
    * @param artifactName the name of the bean
    * @return the object obtained from the FactoryArtifact
    * @throws ArtifactCreationException if FactoryArtifact object creation failed
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact#getObject()
    */
   private Object doGetObjectFromFactoryArtifact(final FactoryArtifact<?> factory, final String artifactName)
      throws ArtifactCreationException {
      Object object;
      try {
         if (System.getSecurityManager() != null) {
            AccessControlContext acc = getAccessControlContext();
            try {
               object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
            }
            catch (PrivilegedActionException pae) {
               throw pae.getException();
            }
         }
         else {
            object = factory.getObject();
         }
      }
      catch (FactoryArtifactNotInitializedException ex) {
         throw new ArtifactCurrentlyInCreationException(artifactName, ex.toString());
      }
      catch (Throwable ex) {
         throw new ArtifactCreationException(artifactName, "FactoryArtifact threw exception on object creation", ex);
      }

      // Do not accept a null value for a FactoryArtifact that's not fully
      // initialized yet: Many FactoryArtifacts just return null then.
      if (object == null) {
         if (isSingletonCurrentlyInCreation(artifactName)) {
            throw new ArtifactCurrentlyInCreationException(
               artifactName, "FactoryArtifact which is currently in creation returned null from getObject");
         }
         object = new NullArtifact();
      }
      return object;
   }

   /**
    * Post-process the given object that has been obtained from the FactoryArtifact.
    * The resulting object will get exposed for bean references.
    * <p>The default implementation simply returns the given object as-is.
    * Subclasses may override this, for example, to apply post-processors.
    * @param object the object obtained from the FactoryArtifact.
    * @param artifactName the name of the bean
    * @return the object to expose
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException if any post-processing failed
    */
   protected Object postProcessObjectFromFactoryArtifact(Object object, String artifactName) throws ArtifactsException {
      return object;
   }

   /**
    * Get a FactoryArtifact for the given bean if possible.
    * @param artifactName the name of the bean
    * @param beanInstance the corresponding bean instance
    * @return the bean instance as FactoryArtifact
    * @throws ArtifactsException if the given bean cannot be exposed as a FactoryArtifact
    */
   protected FactoryArtifact<?> getFactoryArtifact(String artifactName, Object beanInstance) throws ArtifactsException {
      if (!(beanInstance instanceof FactoryArtifact)) {
         throw new ArtifactCreationException(artifactName,
            "Artifact instance of type [" + beanInstance.getClass() + "] is not a FactoryArtifact");
      }
      return (FactoryArtifact<?>) beanInstance;
   }

   /**
    * Overridden to clear the FactoryArtifact object cache as well.
    */
   @Override
   protected void removeSingleton(String artifactName) {
      synchronized (getSingletonMutex()) {
         super.removeSingleton(artifactName);
         this.factoryArtifactObjectCache.remove(artifactName);
      }
   }

   /**
    * Overridden to clear the FactoryArtifact object cache as well.
    */
   @Override
   protected void clearSingletonCache() {
      synchronized (getSingletonMutex()) {
         super.clearSingletonCache();
         this.factoryArtifactObjectCache.clear();
      }
   }

   /**
    * Return the security context for this bean factory. If a security manager
    * is set, interaction with the user code will be executed using the privileged
    * of the security context returned by this method.
    * @see AccessController#getContext()
    */
   protected AccessControlContext getAccessControlContext() {
      return AccessController.getContext();
   }
}
