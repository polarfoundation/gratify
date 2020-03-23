package foundation.polar.gratify.core.type;


import foundation.polar.gratify.annotation.*;
import foundation.polar.gratify.ds.MultiValueMap;
import foundation.polar.gratify.utils.ReflectionUtils;
import foundation.polar.gratify.annotation.MergedAnnotations.SearchStrategy;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link AnnotationMetadata} implementation that uses standard reflection
 * to introspect a given {@link Class}.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

   private final MergedAnnotations mergedAnnotations;

   private final boolean nestedAnnotationsAsMap;

   /**
    * Create a new {@code StandardAnnotationMetadata} wrapper for the given Class.
    * @param introspectedClass the Class to introspect
    * @see #StandardAnnotationMetadata(Class, boolean)
    * @deprecated since 5.2 in favor of the factory method {@link AnnotationMetadata#introspect(Class)}
    */
   @Deprecated
   public StandardAnnotationMetadata(Class<?> introspectedClass) {
      this(introspectedClass, false);
   }

   /**
    * Create a new {@link StandardAnnotationMetadata} wrapper for the given Class,
    * providing the option to return any nested annotations or annotation arrays in the
    * form of {@link foundation.polar.gratify.annotation.AnnotationAttributes} instead
    * of actual {@link Annotation} instances.
    * @param introspectedClass the Class to introspect
    * @param nestedAnnotationsAsMap return nested annotations and annotation arrays as
    * {@link foundation.polar.gratify.annotation.AnnotationAttributes} for compatibility
    * with ASM-based {@link AnnotationMetadata} implementations
    * @since 3.1.1
    * @deprecated since 5.2 in favor of the factory method {@link AnnotationMetadata#introspect(Class)}.
    * Use {@link MergedAnnotation#asMap(foundation.polar.gratify.annotation.MergedAnnotation.Adapt...) MergedAnnotation.asMap}
    * from {@link #getAnnotations()} rather than {@link #getAnnotationAttributes(String)}
    * if {@code nestedAnnotationsAsMap} is {@code false}
    */
   @Deprecated
   public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
      super(introspectedClass);
      this.mergedAnnotations = MergedAnnotations.from(introspectedClass,
         SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
         AnnotationFilter.NONE);
      this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
   }

   @Nullable
   private Set<String> annotationTypes;

   @Override
   public MergedAnnotations getAnnotations() {
      return this.mergedAnnotations;
   }

   @Override
   public Set<String> getAnnotationTypes() {
      Set<String> annotationTypes = this.annotationTypes;
      if (annotationTypes == null) {
         annotationTypes = Collections.unmodifiableSet(AnnotationMetadata.super.getAnnotationTypes());
         this.annotationTypes = annotationTypes;
      }
      return annotationTypes;
   }

   @Override
   @Nullable
   public Map<String, Object> getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
      if (this.nestedAnnotationsAsMap) {
         return AnnotationMetadata.super.getAnnotationAttributes(annotationName, classValuesAsString);
      }
      return AnnotatedElementUtils.getMergedAnnotationAttributes(
         getIntrospectedClass(), annotationName, classValuesAsString, false);
   }

   @Override
   @Nullable
   public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
      if (this.nestedAnnotationsAsMap) {
         return AnnotationMetadata.super.getAllAnnotationAttributes(annotationName, classValuesAsString);
      }
      return AnnotatedElementUtils.getAllAnnotationAttributes(
         getIntrospectedClass(), annotationName, classValuesAsString, false);
   }

   @Override
   public boolean hasAnnotatedMethods(String annotationName) {
      if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
         try {
            Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
            for (Method method : methods) {
               if (isAnnotatedMethod(method, annotationName)) {
                  return true;
               }
            }
         }
         catch (Throwable ex) {
            throw new IllegalStateException("Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
         }
      }
      return false;
   }

   @Override
   @SuppressWarnings("deprecation")
   public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
      Set<MethodMetadata> annotatedMethods = null;
      if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
         try {
            Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
            for (Method method : methods) {
               if (isAnnotatedMethod(method, annotationName)) {
                  if (annotatedMethods == null) {
                     annotatedMethods = new LinkedHashSet<>(4);
                  }
                  annotatedMethods.add(new StandardMethodMetadata(method, this.nestedAnnotationsAsMap));
               }
            }
         }
         catch (Throwable ex) {
            throw new IllegalStateException("Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
         }
      }
      return annotatedMethods != null ? annotatedMethods : Collections.emptySet();
   }

   private boolean isAnnotatedMethod(Method method, String annotationName) {
      return !method.isBridge() && method.getAnnotations().length > 0 &&
         AnnotatedElementUtils.isAnnotated(method, annotationName);
   }

   static AnnotationMetadata from(Class<?> introspectedClass) {
      return new StandardAnnotationMetadata(introspectedClass, true);
   }

}
