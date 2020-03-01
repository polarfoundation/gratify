package foundation.polar.gratify.annotation;

import foundation.polar.gratify.ds.ConcurrentReferenceHashMap;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.ReflectionUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Strategy used to determine annotations that act as containers for other
 * annotations. The {@link #standardRepeatables()} method provides a default
 * strategy that respects Java's {@link Repeatable @Repeatable} support and
 * should be suitable for most situations.
 *
 * <p>The {@link #of} method can be used to register relationships for
 * annotations that do not wish to use {@link Repeatable @Repeatable}.
 *
 * <p>To completely disable repeatable support use {@link #none()}.
 *
 * @author Phillip Webb
 */
public abstract class RepeatableContainers {
   @Nullable
   private final RepeatableContainers parent;

   private RepeatableContainers(@Nullable RepeatableContainers parent) {
      this.parent = parent;
   }

   /**
    * Add an additional explicit relationship between a contained and
    * repeatable annotation.
    * @param container the container type
    * @param repeatable the contained repeatable type
    * @return a new {@link RepeatableContainers} instance
    */
   public RepeatableContainers and(Class<? extends java.lang.annotation.Annotation> container,
                                   Class<? extends java.lang.annotation.Annotation> repeatable) {
      return new ExplicitRepeatableContainer(this, repeatable, container);
   }

   @Nullable
   java.lang.annotation.Annotation[] findRepeatedAnnotations(java.lang.annotation.Annotation annotation) {
      if (this.parent == null) {
         return null;
      }
      return this.parent.findRepeatedAnnotations(annotation);
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (other == this) {
         return true;
      }
      if (other == null || getClass() != other.getClass()) {
         return false;
      }
      return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
   }

   @Override
   public int hashCode() {
      return ObjectUtils.nullSafeHashCode(this.parent);
   }


   /**
    * Create a {@link RepeatableContainers} instance that searches using Java's
    * {@link Repeatable @Repeatable} annotation.
    * @return a {@link RepeatableContainers} instance
    */
   public static RepeatableContainers standardRepeatables() {
      return StandardRepeatableContainers.INSTANCE;
   }

   /**
    * Create a {@link RepeatableContainers} instance that uses a defined
    * container and repeatable type.
    * @param repeatable the contained repeatable annotation
    * @param container the container annotation or {@code null}. If specified,
    * this annotation must declare a {@code value} attribute returning an array
    * of repeatable annotations. If not specified, the container will be
    * deduced by inspecting the {@code @Repeatable} annotation on
    * {@code repeatable}.
    * @return a {@link RepeatableContainers} instance
    */
   public static RepeatableContainers of(
      Class<? extends java.lang.annotation.Annotation> repeatable, @Nullable Class<? extends java.lang.annotation.Annotation> container) {

      return new ExplicitRepeatableContainer(null, repeatable, container);
   }

   /**
    * Create a {@link RepeatableContainers} instance that does not expand any
    * repeatable annotations.
    * @return a {@link RepeatableContainers} instance
    */
   public static RepeatableContainers none() {
      return NoRepeatableContainers.INSTANCE;
   }


   /**
    * Standard {@link RepeatableContainers} implementation that searches using
    * Java's {@link Repeatable @Repeatable} annotation.
    */
   private static class StandardRepeatableContainers extends RepeatableContainers {
      private static final Map<Class<? extends java.lang.annotation.Annotation>, Object> cache = new ConcurrentReferenceHashMap<>();
      private static final Object NONE = new Object();
      private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

      StandardRepeatableContainers() {
         super(null);
      }

      @Override
      @Nullable
      java.lang.annotation.Annotation[] findRepeatedAnnotations(java.lang.annotation.Annotation annotation) {
         Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
         if (method != null) {
            return (java.lang.annotation.Annotation[]) ReflectionUtils.invokeMethod(method, annotation);
         }
         return super.findRepeatedAnnotations(annotation);
      }

      @Nullable
      private static Method getRepeatedAnnotationsMethod(Class<? extends java.lang.annotation.Annotation> annotationType) {
         Object result = cache.computeIfAbsent(annotationType,
            StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
         return (result != NONE ? (Method) result : null);
      }

      private static Object computeRepeatedAnnotationsMethod(Class<? extends java.lang.annotation.Annotation> annotationType) {
         AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
         if (methods.hasOnlyValueAttribute()) {
            Method method = methods.get(0);
            Class<?> returnType = method.getReturnType();
            if (returnType.isArray()) {
               Class<?> componentType = returnType.getComponentType();
               if (java.lang.annotation.Annotation.class.isAssignableFrom(componentType) &&
                  componentType.isAnnotationPresent(Repeatable.class)) {
                  return method;
               }
            }
         }
         return NONE;
      }
   }

   /**
    * A single explicit mapping.
    */
   private static class ExplicitRepeatableContainer extends RepeatableContainers {
      private final Class<? extends java.lang.annotation.Annotation> repeatable;
      private final Class<? extends java.lang.annotation.Annotation> container;
      private final Method valueMethod;

      ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
                                  Class<? extends java.lang.annotation.Annotation> repeatable, @Nullable Class<? extends java.lang.annotation.Annotation> container) {

         super(parent);
         AssertUtils.notNull(repeatable, "Repeatable must not be null");
         if (container == null) {
            container = deduceContainer(repeatable);
         }
         Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
         try {
            if (valueMethod == null) {
               throw new NoSuchMethodException("No value method found");
            }
            Class<?> returnType = valueMethod.getReturnType();
            if (!returnType.isArray() || returnType.getComponentType() != repeatable) {
               throw new AnnotationConfigurationException("Container type [" +
                  container.getName() +
                  "] must declare a 'value' attribute for an array of type [" +
                  repeatable.getName() + "]");
            }
         }
         catch (AnnotationConfigurationException ex) {
            throw ex;
         }
         catch (Throwable ex) {
            throw new AnnotationConfigurationException(
               "Invalid declaration of container type [" + container.getName() +
                  "] for repeatable annotation [" + repeatable.getName() + "]",
               ex);
         }
         this.repeatable = repeatable;
         this.container = container;
         this.valueMethod = valueMethod;
      }

      private Class<? extends java.lang.annotation.Annotation> deduceContainer(Class<? extends java.lang.annotation.Annotation> repeatable) {
         Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
         AssertUtils.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " +
            "failed to resolve container type for " + repeatable.getName());
         return annotation.value();
      }

      @Override
      @Nullable
      java.lang.annotation.Annotation[] findRepeatedAnnotations(java.lang.annotation.Annotation annotation) {
         if (this.container.isAssignableFrom(annotation.annotationType())) {
            return (Annotation[]) ReflectionUtils.invokeMethod(this.valueMethod, annotation);
         }
         return super.findRepeatedAnnotations(annotation);
      }

      @Override
      public boolean equals(@Nullable Object other) {
         if (!super.equals(other)) {
            return false;
         }
         ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
         return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
      }

      @Override
      public int hashCode() {
         int hashCode = super.hashCode();
         hashCode = 31 * hashCode + this.container.hashCode();
         hashCode = 31 * hashCode + this.repeatable.hashCode();
         return hashCode;
      }
   }

   /**
    * No repeatable containers.
    */
   private static class NoRepeatableContainers extends RepeatableContainers {

      private static NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

      NoRepeatableContainers() {
         super(null);
      }
   }
}
