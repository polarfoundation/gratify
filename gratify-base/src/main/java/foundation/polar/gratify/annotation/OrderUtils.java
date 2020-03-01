package foundation.polar.gratify.annotation;

import foundation.polar.gratify.ds.ConcurrentReferenceHashMap;
import foundation.polar.gratify.annotation.MergedAnnotations.SearchStrategy;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * General utility for determining the order of an object based on its type declaration.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see Order
 */
public abstract class OrderUtils {
   /** Cache marker for a non-annotated Class. */
   private static final Object NOT_ANNOTATED = new Object();

   private static final String JAVAX_PRIORITY_ANNOTATION = "javax.annotation.Priority";

   /** Cache for @Order value (or NOT_ANNOTATED marker) per Class. */
   private static final Map<AnnotatedElement, Object> orderCache = new ConcurrentReferenceHashMap<>(64);

   /**
    * Return the order on the specified {@code type}, or the specified
    * default value if none can be found.
    * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
    * @param type the type to handle
    * @return the priority value, or the specified default order if none can be found
    * @see #getPriority(Class)
    */
   public static int getOrder(Class<?> type, int defaultOrder) {
      Integer order = getOrder(type);
      return (order != null ? order : defaultOrder);
   }

   /**
    * Return the order on the specified {@code type}, or the specified
    * default value if none can be found.
    * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
    * @param type the type to handle
    * @return the priority value, or the specified default order if none can be found
    * @see #getPriority(Class)
    */
   @Nullable
   public static Integer getOrder(Class<?> type, @Nullable Integer defaultOrder) {
      Integer order = getOrder(type);
      return (order != null ? order : defaultOrder);
   }

   /**
    * Return the order on the specified {@code type}.
    * <p>Takes care of {@link Order @Order} and {@code @javax.annotation.Priority}.
    * @param type the type to handle
    * @return the order value, or {@code null} if none can be found
    * @see #getPriority(Class)
    */
   @Nullable
   public static Integer getOrder(Class<?> type) {
      return getOrderFromAnnotations(type, MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY));
   }

   /**
    * Return the order from the specified annotations collection.
    * <p>Takes care of {@link Order @Order} and
    * {@code @javax.annotation.Priority}.
    * @param element the source element
    * @param annotations the annotation to consider
    * @return the order value, or {@code null} if none can be found
    */
   @Nullable
   static Integer getOrderFromAnnotations(AnnotatedElement element, MergedAnnotations annotations) {
      if (!(element instanceof Class)) {
         return findOrder(annotations);
      }
      Object cached = orderCache.get(element);
      if (cached != null) {
         return (cached instanceof Integer ? (Integer) cached : null);
      }
      Integer result = findOrder(annotations);
      orderCache.put(element, result != null ? result : NOT_ANNOTATED);
      return result;
   }

   @Nullable
   private static Integer findOrder(MergedAnnotations annotations) {
      MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
      if (orderAnnotation.isPresent()) {
         return orderAnnotation.getInt(MergedAnnotation.VALUE);
      }
      MergedAnnotation<?> priorityAnnotation = annotations.get(JAVAX_PRIORITY_ANNOTATION);
      if (priorityAnnotation.isPresent()) {
         return priorityAnnotation.getInt(MergedAnnotation.VALUE);
      }
      return null;
   }

   /**
    * Return the value of the {@code javax.annotation.Priority} annotation
    * declared on the specified type, or {@code null} if none.
    * @param type the type to handle
    * @return the priority value if the annotation is declared, or {@code null} if none
    */
   @Nullable
   public static Integer getPriority(Class<?> type) {
      return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).get(JAVAX_PRIORITY_ANNOTATION)
         .getValue(MergedAnnotation.VALUE, Integer.class).orElse(null);
   }
}
