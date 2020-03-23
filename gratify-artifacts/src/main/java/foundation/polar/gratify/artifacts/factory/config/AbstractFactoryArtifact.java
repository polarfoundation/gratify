package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.SimpleTypeConverter;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.ReflectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Simple template superclass for {@link FactoryArtifact} implementations that
 * creates a singleton or a prototype object, depending on a flag.
 *
 * <p>If the "singleton" flag is {@code true} (the default),
 * this class will create the object that it creates exactly once
 * on initialization and subsequently return said singleton instance
 * on all calls to the {@link #getObject()} method.
 *
 * <p>Else, this class will create a new instance every time the
 * {@link #getObject()} method is invoked. Subclasses are responsible
 * for implementing the abstract {@link #createInstance()} template
 * method to actually create the object(s) to expose.
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @param <T> the artifact type
 * @see #setSingleton
 * @see #createInstance()
 */
public abstract class AbstractFactoryArtifact<T>
   implements FactoryArtifact<T>, ArtifactClassLoaderAware, ArtifactFactoryAware, InitializingArtifact, DisposableArtifact {

   /** Logger available to subclasses. */
   protected final Log logger = LogFactory.getLog(getClass());

   private boolean singleton = true;

   @Nullable
   private ClassLoader artifactClassLoader = ClassUtils.getDefaultClassLoader();

   @Nullable
   private ArtifactFactory artifactFactory;

   private boolean initialized = false;

   @Nullable
   private T singletonInstance;

   @Nullable
   private T earlySingletonInstance;

   /**
    * Set if a singleton should be created, or a new object on each request
    * otherwise. Default is {@code true} (a singleton).
    */
   public void setSingleton(boolean singleton) {
      this.singleton = singleton;
   }

   @Override
   public boolean isSingleton() {
      return this.singleton;
   }

   @Override
   public void setArtifactClassLoader(ClassLoader classLoader) {
      this.artifactClassLoader = classLoader;
   }

   @Override
   public void setArtifactFactory(@Nullable ArtifactFactory artifactFactory) {
      this.artifactFactory = artifactFactory;
   }

   /**
    * Return the ArtifactFactory that this artifact runs in.
    */
   @Nullable
   protected ArtifactFactory getArtifactFactory() {
      return this.artifactFactory;
   }

   /**
    * Obtain a artifact type converter from the ArtifactFactory that this artifact
    * runs in. This is typically a fresh instance for each call,
    * since TypeConverters are usually <i>not</i> thread-safe.
    * <p>Falls back to a SimpleTypeConverter when not running in a ArtifactFactory.
    * @see ConfigurableArtifactFactory#getTypeConverter()
    * @see foundation.polar.gratify.artifacts.SimpleTypeConverter
    */
   protected TypeConverter getArtifactTypeConverter() {
      ArtifactFactory artifactFactory = getArtifactFactory();
      if (artifactFactory instanceof ConfigurableArtifactFactory) {
         return ((ConfigurableArtifactFactory) artifactFactory).getTypeConverter();
      }
      else {
         return new SimpleTypeConverter();
      }
   }

   /**
    * Eagerly create the singleton instance, if necessary.
    */
   @Override
   public void afterPropertiesSet() throws Exception {
      if (isSingleton()) {
         this.initialized = true;
         this.singletonInstance = createInstance();
         this.earlySingletonInstance = null;
      }
   }

   /**
    * Expose the singleton instance or create a new prototype instance.
    * @see #createInstance()
    * @see #getEarlySingletonInterfaces()
    */
   @Override
   public final T getObject() throws Exception {
      if (isSingleton()) {
         return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
      }
      else {
         return createInstance();
      }
   }

   /**
    * Determine an 'early singleton' instance, exposed in case of a
    * circular reference. Not called in a non-circular scenario.
    */
   @SuppressWarnings("unchecked")
   private T getEarlySingletonInstance() throws Exception {
      Class<?>[] ifcs = getEarlySingletonInterfaces();
      if (ifcs == null) {
         throw new FactoryArtifactNotInitializedException(
            getClass().getName() + " does not support circular references");
      }
      if (this.earlySingletonInstance == null) {
         this.earlySingletonInstance = (T) Proxy.newProxyInstance(
            this.artifactClassLoader, ifcs, new EarlySingletonInvocationHandler());
      }
      return this.earlySingletonInstance;
   }

   /**
    * Expose the singleton instance (for access through the 'early singleton' proxy).
    * @return the singleton instance that this FactoryArtifact holds
    * @throws IllegalStateException if the singleton instance is not initialized
    */
   @Nullable
   private T getSingletonInstance() throws IllegalStateException {
      AssertUtils.state(this.initialized, "Singleton instance not initialized yet");
      return this.singletonInstance;
   }

   /**
    * Destroy the singleton instance, if any.
    * @see #destroyInstance(Object)
    */
   @Override
   public void destroy() throws Exception {
      if (isSingleton()) {
         destroyInstance(this.singletonInstance);
      }
   }

   /**
    * This abstract method declaration mirrors the method in the FactoryArtifact
    * interface, for a consistent offering of abstract template methods.
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifact#getObjectType()
    */
   @Override
   @Nullable
   public abstract Class<?> getObjectType();

   /**
    * Template method that subclasses must override to construct
    * the object returned by this factory.
    * <p>Invoked on initialization of this FactoryArtifact in case of
    * a singleton; else, on each {@link #getObject()} call.
    * @return the object returned by this factory
    * @throws Exception if an exception occurred during object creation
    * @see #getObject()
    */
   protected abstract T createInstance() throws Exception;

   /**
    * Return an array of interfaces that a singleton object exposed by this
    * FactoryArtifact is supposed to implement, for use with an 'early singleton
    * proxy' that will be exposed in case of a circular reference.
    * <p>The default implementation returns this FactoryArtifact's object type,
    * provided that it is an interface, or {@code null} otherwise. The latter
    * indicates that early singleton access is not supported by this FactoryArtifact.
    * This will lead to a FactoryArtifactNotInitializedException getting thrown.
    * @return the interfaces to use for 'early singletons',
    * or {@code null} to indicate a FactoryArtifactNotInitializedException
    * @see foundation.polar.gratify.artifacts.factory.FactoryArtifactNotInitializedException
    */
   @Nullable
   protected Class<?>[] getEarlySingletonInterfaces() {
      Class<?> type = getObjectType();
      return (type != null && type.isInterface() ? new Class<?>[] {type} : null);
   }

   /**
    * Callback for destroying a singleton instance. Subclasses may
    * override this to destroy the previously created instance.
    * <p>The default implementation is empty.
    * @param instance the singleton instance, as returned by
    * {@link #createInstance()}
    * @throws Exception in case of shutdown errors
    * @see #createInstance()
    */
   protected void destroyInstance(@Nullable T instance) throws Exception {
   }

   /**
    * Reflective InvocationHandler for lazy access to the actual singleton object.
    */
   private class EarlySingletonInvocationHandler implements InvocationHandler {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         if (ReflectionUtils.isEqualsMethod(method)) {
            // Only consider equal when proxies are identical.
            return (proxy == args[0]);
         }
         else if (ReflectionUtils.isHashCodeMethod(method)) {
            // Use hashCode of reference proxy.
            return System.identityHashCode(proxy);
         }
         else if (!initialized && ReflectionUtils.isToStringMethod(method)) {
            return "Early singleton proxy for interfaces " +
               ObjectUtils.nullSafeToString(getEarlySingletonInterfaces());
         }
         try {
            return method.invoke(getSingletonInstance(), args);
         }
         catch (InvocationTargetException ex) {
            throw ex.getTargetException();
         }
      }
   }

}
