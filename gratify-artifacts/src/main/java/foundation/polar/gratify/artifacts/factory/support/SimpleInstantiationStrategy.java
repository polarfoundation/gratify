package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactInstantiationException;
import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory;
import foundation.polar.gratify.utils.ReflectionUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * Simple object instantiation strategy for use in a ArtifactFactory.
 *
 * <p>Does not support Method Injection, although it provides hooks for subclasses
 * to override to add Method Injection support, for example by overriding methods.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class SimpleInstantiationStrategy implements InstantiationStrategy {

   private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

   /**
    * Return the factory method currently being invoked or {@code null} if none.
    * <p>Allows factory method implementations to determine whether the current
    * caller is the container itself as opposed to user code.
    */
   @Nullable
   public static Method getCurrentlyInvokedFactoryMethod() {
      return currentlyInvokedFactoryMethod.get();
   }

   @Override
   public Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner) {
      // Don't override the class with CGLIB if no overrides.
      if (!bd.hasMethodOverrides()) {
         Constructor<?> constructorToUse;
         synchronized (bd.constructorArgumentLock) {
            constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
            if (constructorToUse == null) {
               final Class<?> clazz = bd.getArtifactClass();
               if (clazz.isInterface()) {
                  throw new ArtifactInstantiationException(clazz, "Specified class is an interface");
               }
               try {
                  if (System.getSecurityManager() != null) {
                     constructorToUse = AccessController.doPrivileged(
                        (PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
                  }
                  else {
                     constructorToUse = clazz.getDeclaredConstructor();
                  }
                  bd.resolvedConstructorOrFactoryMethod = constructorToUse;
               }
               catch (Throwable ex) {
                  throw new ArtifactInstantiationException(clazz, "No default constructor found", ex);
               }
            }
         }
         return ArtifactUtils.instantiateClass(constructorToUse);
      }
      else {
         // Must generate CGLIB subclass.
         return instantiateWithMethodInjection(bd, beanName, owner);
      }
   }

   /**
    * Subclasses can override this method, which is implemented to throw
    * UnsupportedOperationException, if they can instantiate an object with
    * the Method Injection specified in the given RootArtifactDefinition.
    * Instantiation should use a no-arg constructor.
    */
   protected Object instantiateWithMethodInjection(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner) {
      throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
   }

   @Override
   public Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner,
                             final Constructor<?> ctor, Object... args) {

      if (!bd.hasMethodOverrides()) {
         if (System.getSecurityManager() != null) {
            // use own privileged to change accessibility (when security is on)
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               ReflectionUtils.makeAccessible(ctor);
               return null;
            });
         }
         return ArtifactUtils.instantiateClass(ctor, args);
      }
      else {
         return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
      }
   }

   /**
    * Subclasses can override this method, which is implemented to throw
    * UnsupportedOperationException, if they can instantiate an object with
    * the Method Injection specified in the given RootArtifactDefinition.
    * Instantiation should use the given constructor and parameters.
    */
   protected Object instantiateWithMethodInjection(RootArtifactDefinition bd, @Nullable String beanName,
                                                   ArtifactFactory owner, @Nullable Constructor<?> ctor, Object... args) {

      throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
   }

   @Override
   public Object instantiate(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner,
                             @Nullable Object factoryArtifact, final Method factoryMethod, Object... args) {

      try {
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
               ReflectionUtils.makeAccessible(factoryMethod);
               return null;
            });
         }
         else {
            ReflectionUtils.makeAccessible(factoryMethod);
         }

         Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
         try {
            currentlyInvokedFactoryMethod.set(factoryMethod);
            Object result = factoryMethod.invoke(factoryArtifact, args);
            if (result == null) {
               result = new NullArtifact();
            }
            return result;
         }
         finally {
            if (priorInvokedFactoryMethod != null) {
               currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
            }
            else {
               currentlyInvokedFactoryMethod.remove();
            }
         }
      }
      catch (IllegalArgumentException ex) {
         throw new ArtifactInstantiationException(factoryMethod,
            "Illegal arguments to factory method '" + factoryMethod.getName() + "'; " +
               "args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
      }
      catch (IllegalAccessException ex) {
         throw new ArtifactInstantiationException(factoryMethod,
            "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
      }
      catch (InvocationTargetException ex) {
         String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
         if (bd.getFactoryArtifactName() != null && owner instanceof ConfigurableArtifactFactory &&
            ((ConfigurableArtifactFactory) owner).isCurrentlyInCreation(bd.getFactoryArtifactName())) {
            msg = "Circular reference involving containing bean '" + bd.getFactoryArtifactName() + "' - consider " +
               "declaring the factory method as static for independence from its containing instance. " + msg;
         }
         throw new ArtifactInstantiationException(factoryMethod, msg, ex.getTargetException());
      }
   }
}
