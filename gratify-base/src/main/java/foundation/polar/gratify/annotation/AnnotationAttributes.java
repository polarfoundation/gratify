package foundation.polar.gratify.annotation;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LinkedHashMap} subclass representing annotation attribute
 * <em>key-value</em> pairs as read by {@link AnnotationUtils},
 * {@link AnnotatedElementUtils}, and Spring's reflection- and ASM-based
 * {@link foundation.polar.gratify.core.type.AnnotationMetadata} implementations.
 *
 * <p>Provides 'pseudo-reification' to avoid noisy Map generics in the calling
 * code as well as convenience methods for looking up annotation attributes
 * in a type-safe fashion.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {
   private static final String UNKNOWN = "unknown";

   @Nullable
   private final Class<? extends java.lang.annotation.Annotation> annotationType;

   final String displayName;

   boolean validated = false;

   /**
    * Create a new, empty {@link AnnotationAttributes} instance.
    */
   public AnnotationAttributes() {
      this.annotationType = null;
      this.displayName = UNKNOWN;
   }

   /**
    * Create a new, empty {@link AnnotationAttributes} instance with the
    * given initial capacity to optimize performance.
    * @param initialCapacity initial size of the underlying map
    */
   public AnnotationAttributes(int initialCapacity) {
      super(initialCapacity);
      this.annotationType = null;
      this.displayName = UNKNOWN;
   }

   /**
    * Create a new {@link AnnotationAttributes} instance, wrapping the provided
    * map and all its <em>key-value</em> pairs.
    * @param map original source of annotation attribute <em>key-value</em> pairs
    * @see #fromMap(Map)
    */
   public AnnotationAttributes(Map<String, Object> map) {
      super(map);
      this.annotationType = null;
      this.displayName = UNKNOWN;
   }

   /**
    * Create a new {@link AnnotationAttributes} instance, wrapping the provided
    * map and all its <em>key-value</em> pairs.
    * @param other original source of annotation attribute <em>key-value</em> pairs
    * @see #fromMap(Map)
    */
   public AnnotationAttributes(AnnotationAttributes other) {
      super(other);
      this.annotationType = other.annotationType;
      this.displayName = other.displayName;
      this.validated = other.validated;
   }

   /**
    * Create a new, empty {@link AnnotationAttributes} instance for the
    * specified {@code annotationType}.
    * @param annotationType the type of annotation represented by this
    * {@code AnnotationAttributes} instance; never {@code null}
    */
   public AnnotationAttributes(Class<? extends java.lang.annotation.Annotation> annotationType) {
      AssertUtils.notNull(annotationType, "'annotationType' must not be null");
      this.annotationType = annotationType;
      this.displayName = annotationType.getName();
   }

   /**
    * Create a possibly already validated new, empty
    * {@link AnnotationAttributes} instance for the specified
    * {@code annotationType}.
    * @param annotationType the type of annotation represented by this
    * {@code AnnotationAttributes} instance; never {@code null}
    * @param validated if the attributes are considered already validated
    */
   AnnotationAttributes(Class<? extends java.lang.annotation.Annotation> annotationType, boolean validated) {
      AssertUtils.notNull(annotationType, "'annotationType' must not be null");
      this.annotationType = annotationType;
      this.displayName = annotationType.getName();
      this.validated = validated;
   }

   /**
    * Create a new, empty {@link AnnotationAttributes} instance for the
    * specified {@code annotationType}.
    * @param annotationType the annotation type name represented by this
    * {@code AnnotationAttributes} instance; never {@code null}
    * @param classLoader the ClassLoader to try to load the annotation type on,
    * or {@code null} to just store the annotation type name
    */
   public AnnotationAttributes(String annotationType, @Nullable ClassLoader classLoader) {
      AssertUtils.notNull(annotationType, "'annotationType' must not be null");
      this.annotationType = getAnnotationType(annotationType, classLoader);
      this.displayName = annotationType;
   }

   @SuppressWarnings("unchecked")
   @Nullable
   private static Class<? extends java.lang.annotation.Annotation> getAnnotationType(String annotationType, @Nullable ClassLoader classLoader) {
      if (classLoader != null) {
         try {
            return (Class<? extends java.lang.annotation.Annotation>) classLoader.loadClass(annotationType);
         }
         catch (ClassNotFoundException ex) {
            // Annotation Class not resolvable
         }
      }
      return null;
   }


   /**
    * Get the type of annotation represented by this {@code AnnotationAttributes}.
    * @return the annotation type, or {@code null} if unknown
    */
   @Nullable
   public Class<? extends java.lang.annotation.Annotation> annotationType() {
      return this.annotationType;
   }

   /**
    * Get the value stored under the specified {@code attributeName} as a string.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public String getString(String attributeName) {
      return getRequiredAttribute(attributeName, String.class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as an
    * array of strings.
    * <p>If the value stored under the specified {@code attributeName} is
    * a string, it will be wrapped in a single-element array before
    * returning it.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public String[] getStringArray(String attributeName) {
      return getRequiredAttribute(attributeName, String[].class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as a boolean.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public boolean getBoolean(String attributeName) {
      return getRequiredAttribute(attributeName, Boolean.class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as a number.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   @SuppressWarnings("unchecked")
   public <N extends Number> N getNumber(String attributeName) {
      return (N) getRequiredAttribute(attributeName, Number.class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as an enum.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   @SuppressWarnings("unchecked")
   public <E extends Enum<?>> E getEnum(String attributeName) {
      return (E) getRequiredAttribute(attributeName, Enum.class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as a class.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   @SuppressWarnings("unchecked")
   public <T> Class<? extends T> getClass(String attributeName) {
      return getRequiredAttribute(attributeName, Class.class);
   }

   /**
    * Get the value stored under the specified {@code attributeName} as an
    * array of classes.
    * <p>If the value stored under the specified {@code attributeName} is a class,
    * it will be wrapped in a single-element array before returning it.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public Class<?>[] getClassArray(String attributeName) {
      return getRequiredAttribute(attributeName, Class[].class);
   }

   /**
    * Get the {@link AnnotationAttributes} stored under the specified
    * {@code attributeName}.
    * <p>Note: if you expect an actual annotation, invoke
    * {@link #getAnnotation(String, Class)} instead.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the {@code AnnotationAttributes}
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public AnnotationAttributes getAnnotation(String attributeName) {
      return getRequiredAttribute(attributeName, AnnotationAttributes.class);
   }

   /**
    * Get the annotation of type {@code annotationType} stored under the
    * specified {@code attributeName}.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @param annotationType the expected annotation type; never {@code null}
    * @return the annotation
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public <A extends java.lang.annotation.Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
      return getRequiredAttribute(attributeName, annotationType);
   }

   /**
    * Get the array of {@link AnnotationAttributes} stored under the specified
    * {@code attributeName}.
    * <p>If the value stored under the specified {@code attributeName} is
    * an instance of {@code AnnotationAttributes}, it will be wrapped in
    * a single-element array before returning it.
    * <p>Note: if you expect an actual array of annotations, invoke
    * {@link #getAnnotationArray(String, Class)} instead.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @return the array of {@code AnnotationAttributes}
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   public AnnotationAttributes[] getAnnotationArray(String attributeName) {
      return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
   }

   /**
    * Get the array of type {@code annotationType} stored under the specified
    * {@code attributeName}.
    * <p>If the value stored under the specified {@code attributeName} is
    * an {@code Annotation}, it will be wrapped in a single-element array
    * before returning it.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @param annotationType the expected annotation type; never {@code null}
    * @return the annotation array
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   @SuppressWarnings("unchecked")
   public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
      Object array = Array.newInstance(annotationType, 0);
      return (A[]) getRequiredAttribute(attributeName, array.getClass());
   }

   /**
    * Get the value stored under the specified {@code attributeName},
    * ensuring that the value is of the {@code expectedType}.
    * <p>If the {@code expectedType} is an array and the value stored
    * under the specified {@code attributeName} is a single element of the
    * component type of the expected array type, the single element will be
    * wrapped in a single-element array of the appropriate type before
    * returning it.
    * @param attributeName the name of the attribute to get;
    * never {@code null} or empty
    * @param expectedType the expected type; never {@code null}
    * @return the value
    * @throws IllegalArgumentException if the attribute does not exist or
    * if it is not of the expected type
    */
   @SuppressWarnings("unchecked")
   private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
      AssertUtils.hasText(attributeName, "'attributeName' must not be null or empty");
      Object value = get(attributeName);
      assertAttributePresence(attributeName, value);
      assertNotException(attributeName, value);
      if (!expectedType.isInstance(value) && expectedType.isArray() &&
         expectedType.getComponentType().isInstance(value)) {
         Object array = Array.newInstance(expectedType.getComponentType(), 1);
         Array.set(array, 0, value);
         value = array;
      }
      assertAttributeType(attributeName, value, expectedType);
      return (T) value;
   }

   private void assertAttributePresence(String attributeName, Object attributeValue) {
      AssertUtils.notNull(attributeValue, () -> String.format(
         "Attribute '%s' not found in attributes for annotation [%s]",
         attributeName, this.displayName));
   }

   private void assertNotException(String attributeName, Object attributeValue) {
      if (attributeValue instanceof Throwable) {
         throw new IllegalArgumentException(String.format(
            "Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
            attributeName, this.displayName, attributeValue), (Throwable) attributeValue);
      }
   }

   private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
      if (!expectedType.isInstance(attributeValue)) {
         throw new IllegalArgumentException(String.format(
            "Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
            attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
            this.displayName));
      }
   }

   @Override
   public String toString() {
      Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
      StringBuilder sb = new StringBuilder("{");
      while (entries.hasNext()) {
         Map.Entry<String, Object> entry = entries.next();
         sb.append(entry.getKey());
         sb.append('=');
         sb.append(valueToString(entry.getValue()));
         sb.append(entries.hasNext() ? ", " : "");
      }
      sb.append("}");
      return sb.toString();
   }

   private String valueToString(Object value) {
      if (value == this) {
         return "(this Map)";
      }
      if (value instanceof Object[]) {
         return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
      }
      return String.valueOf(value);
   }


   /**
    * Return an {@link AnnotationAttributes} instance based on the given map.
    * <p>If the map is already an {@code AnnotationAttributes} instance, it
    * will be cast and returned immediately without creating a new instance.
    * Otherwise a new instance will be created by passing the supplied map
    * to the {@link #AnnotationAttributes(Map)} constructor.
    * @param map original source of annotation attribute <em>key-value</em> pairs
    */
   @Nullable
   public static AnnotationAttributes fromMap(@Nullable Map<String, Object> map) {
      if (map == null) {
         return null;
      }
      if (map instanceof AnnotationAttributes) {
         return (AnnotationAttributes) map;
      }
      return new AnnotationAttributes(map);
   }
}
