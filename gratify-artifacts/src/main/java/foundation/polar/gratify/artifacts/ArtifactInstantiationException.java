package foundation.polar.gratify.artifacts;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Exception thrown when instantiation of a bean failed.
 * Carries the offending bean class.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ArtifactInstantiationException extends FatalArtifactException {
   private final Class<?> beanClass;

   @Nullable
   private final Constructor<?> constructor;

   @Nullable
   private final Method constructingMethod;
   
   /**
    * Create a new ArtifactInstantiationException.
    * @param beanClass the offending bean class
    * @param msg the detail message
    */
   public ArtifactInstantiationException(Class<?> beanClass, String msg) {
      this(beanClass, msg, null);
   }

   /**
    * Create a new ArtifactInstantiationException.
    * @param beanClass the offending bean class
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactInstantiationException(Class<?> beanClass, String msg, @Nullable Throwable cause) {
      super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
      this.beanClass = beanClass;
      this.constructor = null;
      this.constructingMethod = null;
   }

   /**
    * Create a new ArtifactInstantiationException.
    * @param constructor the offending constructor
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactInstantiationException(Constructor<?> constructor, String msg, @Nullable Throwable cause) {
      super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
      this.beanClass = constructor.getDeclaringClass();
      this.constructor = constructor;
      this.constructingMethod = null;
   }

   /**
    * Create a new ArtifactInstantiationException.
    * @param constructingMethod the delegate for bean construction purposes
    * (typically, but not necessarily, a static factory method)
    * @param msg the detail message
    * @param cause the root cause
    */
   public ArtifactInstantiationException(Method constructingMethod, String msg, @Nullable Throwable cause) {
      super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
      this.beanClass = constructingMethod.getReturnType();
      this.constructor = null;
      this.constructingMethod = constructingMethod;
   }

   /**
    * Return the offending bean class (never {@code null}).
    * @return the class that was to be instantiated
    */
   public Class<?> getArtifactClass() {
      return this.beanClass;
   }

   /**
    * Return the offending constructor, if known.
    * @return the constructor in use, or {@code null} in case of a
    * factory method or in case of default instantiation
    */
   @Nullable
   public Constructor<?> getConstructor() {
      return this.constructor;
   }

   /**
    * Return the delegate for bean construction purposes, if known.
    * @return the method in use (typically a static factory method),
    * or {@code null} in case of constructor-based instantiation
    */
   @Nullable
   public Method getConstructingMethod() {
      return this.constructingMethod;
   }
}
