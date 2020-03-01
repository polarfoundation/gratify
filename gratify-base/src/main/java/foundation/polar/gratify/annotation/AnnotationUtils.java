package foundation.polar.gratify.annotation;

import foundation.polar.gratify.ds.ConcurrentReferenceHashMap;
import foundation.polar.gratify.utils.ReflectionUtils;
import foundation.polar.gratify.annotation.MergedAnnotations.SearchStrategy;
import foundation.polar.gratify.utils.StringUtils;
import foundation.polar.gratify.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import foundation.polar.gratify.annotation.MergedAnnotation.Adapt;
import foundation.polar.gratify.core.BridgeMethodResolver;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * General utility methods for working with annotations, handling meta-annotations,
 * bridge methods (which the compiler generates for generic declarations) as well
 * as super methods (for optional <em>annotation inheritance</em>).
 *
 * <p>Note that most of the features of this class are not provided by the
 * JDK's introspection facilities themselves.
 *
 * <p>As a general rule for runtime-retained application annotations (e.g. for
 * transaction control, authorization, or service exposure), always use the
 * lookup methods on this class (e.g. {@link #findAnnotation(Method, Class)} or
 * {@link #getAnnotation(Method, Class)}) instead of the plain annotation lookup
 * methods in the JDK. You can still explicitly choose between a <em>get</em>
 * lookup on the given class level only ({@link #getAnnotation(Method, Class)})
 * and a <em>find</em> lookup in the entire inheritance hierarchy of the given
 * method ({@link #findAnnotation(Method, Class)}).
 *
 * <h3>Terminology</h3>
 * The terms <em>directly present</em>, <em>indirectly present</em>, and
 * <em>present</em> have the same meanings as defined in the class-level
 * javadoc for {@link AnnotatedElement} (in Java 8).
 *
 * <p>An annotation is <em>meta-present</em> on an element if the annotation
 * is declared as a meta-annotation on some other annotation which is
 * <em>present</em> on the element. Annotation {@code A} is <em>meta-present</em>
 * on another annotation if {@code A} is either <em>directly present</em> or
 * <em>meta-present</em> on the other annotation.
 *
 * <h3>Meta-annotation Support</h3>
 * <p>Most {@code find*()} methods and some {@code get*()} methods in this class
 * provide support for finding annotations used as meta-annotations. Consult the
 * javadoc for each method in this class for details. For fine-grained support for
 * meta-annotations with <em>attribute overrides</em> in <em>composed annotations</em>,
 * consider using {@link AnnotatedElementUtils}'s more specific methods instead.
 *
 * <h3>Attribute Aliases</h3>
 * <p>All public methods in this class that return annotations, arrays of
 * annotations, or {@link AnnotationAttributes} transparently support attribute
 * aliases configured via {@link AliasFor @AliasFor}. Consult the various
 * {@code synthesizeAnnotation*(..)} methods for details.
 *
 * <h3>Search Scope</h3>
 * <p>The search algorithms used by methods in this class stop searching for
 * an annotation once the first annotation of the specified type has been
 * found. As a consequence, additional annotations of the specified type will
 * be silently ignored.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Fisher
 * @author Chris Beams
 * @author Phillip Webb
 * @author Oleg Zhurakousky
 * @see AliasFor
 * @see AnnotationAttributes
 * @see AnnotatedElementUtils
 * @see BridgeMethodResolver
 * @see java.lang.reflect.AnnotatedElement#getAnnotations()
 * @see java.lang.reflect.AnnotatedElement#getAnnotation(Class)
 * @see java.lang.reflect.AnnotatedElement#getDeclaredAnnotations()
 */
public abstract class AnnotationUtils {
   /**
    * The attribute name for annotations with a single element.
    */
   public static final String VALUE = MergedAnnotation.VALUE;

   private static final AnnotationFilter JAVA_LANG_ANNOTATION_FILTER =
      AnnotationFilter.packages("java.lang.annotation");

   private static final Map<Class<? extends java.lang.annotation.Annotation>, Map<String, DefaultValueHolder>> defaultValuesCache =
      new ConcurrentReferenceHashMap<>();


   /**
    * Determine whether the given class is a candidate for carrying one of the specified
    * annotations (at type, method or field level).
    * @param clazz the class to introspect
    * @param annotationTypes the searchable annotation types
    * @return {@code false} if the class is known to have no such annotations at any level;
    * {@code true} otherwise. Callers will usually perform full method/field introspection
    * if {@code true} is being returned here.
    * @see #isCandidateClass(Class, Class)
    * @see #isCandidateClass(Class, String)
    */
   public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends java.lang.annotation.Annotation>> annotationTypes) {
      for (Class<? extends java.lang.annotation.Annotation> annotationType : annotationTypes) {
         if (isCandidateClass(clazz, annotationType)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine whether the given class is a candidate for carrying the specified annotation
    * (at type, method or field level).
    * @param clazz the class to introspect
    * @param annotationType the searchable annotation type
    * @return {@code false} if the class is known to have no such annotations at any level;
    * {@code true} otherwise. Callers will usually perform full method/field introspection
    * if {@code true} is being returned here.
    * @see #isCandidateClass(Class, String)
    */
   public static boolean isCandidateClass(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotationType) {
      return isCandidateClass(clazz, annotationType.getName());
   }

   /**
    * Determine whether the given class is a candidate for carrying the specified annotation
    * (at type, method or field level).
    * @param clazz the class to introspect
    * @param annotationName the fully-qualified name of the searchable annotation type
    * @return {@code false} if the class is known to have no such annotations at any level;
    * {@code true} otherwise. Callers will usually perform full method/field introspection
    * if {@code true} is being returned here.
    * @see #isCandidateClass(Class, Class)
    */
   public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
      if (annotationName.startsWith("java.")) {
         return true;
      }
      if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
         return false;
      }
      return true;
   }

   /**
    * Get a single {@link java.lang.annotation.Annotation} of {@code annotationType} from the supplied
    * annotation: either the given annotation itself or a direct meta-annotation
    * thereof.
    * <p>Note that this method supports only a single level of meta-annotations.
    * For support for arbitrary levels of meta-annotations, use one of the
    * {@code find*()} methods instead.
    * @param annotation the Annotation to check
    * @param annotationType the annotation type to look for, both locally and as a meta-annotation
    * @return the first matching annotation, or {@code null} if not found
    */
   @SuppressWarnings("unchecked")
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A getAnnotation(java.lang.annotation.Annotation annotation, Class<A> annotationType) {
      // Shortcut: directly present on the element, with no merging needed?
      if (annotationType.isInstance(annotation)) {
         return synthesizeAnnotation((A) annotation, annotationType);
      }
      // Shortcut: no searchable annotations to be found on plain Java classes and core Spring types...
      if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotation)) {
         return null;
      }
      // Exhaustive retrieval of merged annotations...
      return MergedAnnotations.from(annotation, new java.lang.annotation.Annotation[] {annotation}, RepeatableContainers.none())
         .get(annotationType).withNonMergedAttributes()
         .synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
   }

   /**
    * Get a single {@link java.lang.annotation.Annotation} of {@code annotationType} from the supplied
    * {@link AnnotatedElement}, where the annotation is either <em>present</em> or
    * <em>meta-present</em> on the {@code AnnotatedElement}.
    * <p>Note that this method supports only a single level of meta-annotations.
    * For support for arbitrary levels of meta-annotations, use
    * {@link #findAnnotation(AnnotatedElement, Class)} instead.
    * @param annotatedElement the {@code AnnotatedElement} from which to get the annotation
    * @param annotationType the annotation type to look for, both locally and as a meta-annotation
    * @return the first matching annotation, or {@code null} if not found
    */
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
      // Shortcut: directly present on the element, with no merging needed?
      if (AnnotationFilter.PLAIN.matches(annotationType) ||
         AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
         return annotatedElement.getAnnotation(annotationType);
      }
      // Exhaustive retrieval of merged annotations...
      return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
         .get(annotationType).withNonMergedAttributes()
         .synthesize(AnnotationUtils::isSingleLevelPresent).orElse(null);
   }

   private static <A extends java.lang.annotation.Annotation> boolean isSingleLevelPresent(MergedAnnotation<A> mergedAnnotation) {
      int distance = mergedAnnotation.getDistance();
      return (distance == 0 || distance == 1);
   }

   /**
    * Get a single {@link java.lang.annotation.Annotation} of {@code annotationType} from the
    * supplied {@link Method}, where the annotation is either <em>present</em>
    * or <em>meta-present</em> on the method.
    * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
    * <p>Note that this method supports only a single level of meta-annotations.
    * For support for arbitrary levels of meta-annotations, use
    * {@link #findAnnotation(Method, Class)} instead.
    * @param method the method to look for annotations on
    * @param annotationType the annotation type to look for
    * @return the first matching annotation, or {@code null} if not found
    * @see foundation.polar.gratify.core.BridgeMethodResolver#findBridgedMethod(Method)
    * @see #getAnnotation(AnnotatedElement, Class)
    */
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A getAnnotation(Method method, Class<A> annotationType) {
      Method resolvedMethod = BridgeMethodResolver.findBridgedMethod(method);
      return getAnnotation((AnnotatedElement) resolvedMethod, annotationType);
   }

   /**
    * Find a single {@link java.lang.annotation.Annotation} of {@code annotationType} on the
    * supplied {@link AnnotatedElement}.
    * <p>Meta-annotations will be searched if the annotation is not
    * <em>directly present</em> on the supplied element.
    * <p><strong>Warning</strong>: this method operates generically on
    * annotated elements. In other words, this method does not execute
    * specialized search algorithms for classes or methods. If you require
    * the more specific semantics of {@link #findAnnotation(Class, Class)}
    * or {@link #findAnnotation(Method, Class)}, invoke one of those methods
    * instead.
    * @param annotatedElement the {@code AnnotatedElement} on which to find the annotation
    * @param annotationType the annotation type to look for, both locally and as a meta-annotation
    * @return the first matching annotation, or {@code null} if not found
    */
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A findAnnotation(
      AnnotatedElement annotatedElement, @Nullable Class<A> annotationType) {

      if (annotationType == null) {
         return null;
      }

      // Shortcut: directly present on the element, with no merging needed?
      if (AnnotationFilter.PLAIN.matches(annotationType) ||
         AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
         return annotatedElement.getDeclaredAnnotation(annotationType);
      }

      // Exhaustive retrieval of merged annotations...
      return MergedAnnotations.from(annotatedElement, SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none())
         .get(annotationType).withNonMergedAttributes()
         .synthesize(MergedAnnotation::isPresent).orElse(null);
   }

   /**
    * Find a single {@link java.lang.annotation.Annotation} of {@code annotationType} on the supplied
    * {@link Method}, traversing its super methods (i.e. from superclasses and
    * interfaces) if the annotation is not <em>directly present</em> on the given
    * method itself.
    * <p>Correctly handles bridge {@link Method Methods} generated by the compiler.
    * <p>Meta-annotations will be searched if the annotation is not
    * <em>directly present</em> on the method.
    * <p>Annotations on methods are not inherited by default, so we need to handle
    * this explicitly.
    * @param method the method to look for annotations on
    * @param annotationType the annotation type to look for
    * @return the first matching annotation, or {@code null} if not found
    * @see #getAnnotation(Method, Class)
    */
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
      if (annotationType == null) {
         return null;
      }

      // Shortcut: directly present on the element, with no merging needed?
      if (AnnotationFilter.PLAIN.matches(annotationType) ||
         AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
         return method.getDeclaredAnnotation(annotationType);
      }

      // Exhaustive retrieval of merged annotations...
      return MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
         .get(annotationType).withNonMergedAttributes()
         .synthesize(MergedAnnotation::isPresent).orElse(null);
   }

   /**
    * Find a single {@link java.lang.annotation.Annotation} of {@code annotationType} on the
    * supplied {@link Class}, traversing its interfaces, annotations, and
    * superclasses if the annotation is not <em>directly present</em> on
    * the given class itself.
    * <p>This method explicitly handles class-level annotations which are not
    * declared as {@link java.lang.annotation.Inherited inherited} <em>as well
    * as meta-annotations and annotations on interfaces</em>.
    * <p>The algorithm operates as follows:
    * <ol>
    * <li>Search for the annotation on the given class and return it if found.
    * <li>Recursively search through all annotations that the given class declares.
    * <li>Recursively search through all interfaces that the given class declares.
    * <li>Recursively search through the superclass hierarchy of the given class.
    * </ol>
    * <p>Note: in this context, the term <em>recursively</em> means that the search
    * process continues by returning to step #1 with the current interface,
    * annotation, or superclass as the class to look for annotations on.
    * @param clazz the class to look for annotations on
    * @param annotationType the type of annotation to look for
    * @return the first matching annotation, or {@code null} if not found
    */
   @Nullable
   public static <A extends java.lang.annotation.Annotation> A findAnnotation(Class<?> clazz, @Nullable Class<A> annotationType) {
      if (annotationType == null) {
         return null;
      }

      // Shortcut: directly present on the element, with no merging needed?
      if (AnnotationFilter.PLAIN.matches(annotationType) ||
         AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
         A annotation = clazz.getDeclaredAnnotation(annotationType);
         if (annotation != null) {
            return annotation;
         }
         // For backwards compatibility, perform a superclass search with plain annotations
         // even if not marked as @Inherited: e.g. a findAnnotation search for @Deprecated
         Class<?> superclass = clazz.getSuperclass();
         if (superclass == null || superclass == Object.class) {
            return null;
         }
         return findAnnotation(superclass, annotationType);
      }

      // Exhaustive retrieval of merged annotations...
      return MergedAnnotations.from(clazz, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
         .get(annotationType).withNonMergedAttributes()
         .synthesize(MergedAnnotation::isPresent).orElse(null);
   }

   /**
    * Determine whether an annotation of the specified {@code annotationType}
    * is declared locally (i.e. <em>directly present</em>) on the supplied
    * {@code clazz}.
    * <p>The supplied {@link Class} may represent any type.
    * <p>Meta-annotations will <em>not</em> be searched.
    * <p>Note: This method does <strong>not</strong> determine if the annotation
    * is {@linkplain java.lang.annotation.Inherited inherited}.
    * @param annotationType the annotation type to look for
    * @param clazz the class to check for the annotation on
    * @return {@code true} if an annotation of the specified {@code annotationType}
    * is <em>directly present</em>
    * @see java.lang.Class#getDeclaredAnnotations()
    * @see java.lang.Class#getDeclaredAnnotation(Class)
    */
   public static boolean isAnnotationDeclaredLocally(Class<? extends java.lang.annotation.Annotation> annotationType, Class<?> clazz) {
      return MergedAnnotations.from(clazz).get(annotationType).isDirectlyPresent();
   }

   /**
    * Determine if the supplied {@link java.lang.annotation.Annotation} is defined in the core JDK
    * {@code java.lang.annotation} package.
    * @param annotation the annotation to check
    * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
    */
   public static boolean isInJavaLangAnnotationPackage(java.lang.annotation.Annotation annotation) {
      return (annotation != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotation));
   }

   /**
    * Determine if the {@link java.lang.annotation.Annotation} with the supplied name is defined
    * in the core JDK {@code java.lang.annotation} package.
    * @param annotationType the name of the annotation type to check
    * @return {@code true} if the annotation is in the {@code java.lang.annotation} package
    */
   public static boolean isInJavaLangAnnotationPackage(@Nullable String annotationType) {
      return (annotationType != null && JAVA_LANG_ANNOTATION_FILTER.matches(annotationType));
   }

   /**
    * Check the declared attributes of the given annotation, in particular covering
    * Google App Engine's late arrival of {@code TypeNotPresentExceptionProxy} for
    * {@code Class} values (instead of early {@code Class.getAnnotations() failure}.
    * <p>This method not failing indicates that {@link #getAnnotationAttributes(java.lang.annotation.Annotation)}
    * won't failure either (when attempted later on).
    * @param annotation the annotation to validate
    * @throws IllegalStateException if a declared {@code Class} attribute could not be read
    * @see Class#getAnnotations()
    * @see #getAnnotationAttributes(java.lang.annotation.Annotation)
    */
   public static void validateAnnotation(java.lang.annotation.Annotation annotation) {
      AttributeMethods.forAnnotationType(annotation.annotationType()).validate(annotation);
   }

   /**
    * Retrieve the given annotation's attributes as a {@link Map}, preserving all
    * attribute types.
    * <p>Equivalent to calling {@link #getAnnotationAttributes(java.lang.annotation.Annotation, boolean, boolean)}
    * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
    * set to {@code false}.
    * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
    * However, the {@code Map} signature has been preserved for binary compatibility.
    * @param annotation the annotation to retrieve the attributes for
    * @return the Map of annotation attributes, with attribute names as keys and
    * corresponding attribute values as values (never {@code null})
    * @see #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation)
    * @see #getAnnotationAttributes(java.lang.annotation.Annotation, boolean, boolean)
    * @see #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation, boolean, boolean)
    */
   public static Map<String, Object> getAnnotationAttributes(java.lang.annotation.Annotation annotation) {
      return getAnnotationAttributes(null, annotation);
   }

   /**
    * Retrieve the given annotation's attributes as a {@link Map}.
    * <p>Equivalent to calling {@link #getAnnotationAttributes(java.lang.annotation.Annotation, boolean, boolean)}
    * with the {@code nestedAnnotationsAsMap} parameter set to {@code false}.
    * <p>Note: This method actually returns an {@link AnnotationAttributes} instance.
    * However, the {@code Map} signature has been preserved for binary compatibility.
    * @param annotation the annotation to retrieve the attributes for
    * @param classValuesAsString whether to convert Class references into Strings (for
    * compatibility with {@link foundation.polar.gratify.core.type.AnnotationMetadata})
    * or to preserve them as Class references
    * @return the Map of annotation attributes, with attribute names as keys and
    * corresponding attribute values as values (never {@code null})
    * @see #getAnnotationAttributes(java.lang.annotation.Annotation, boolean, boolean)
    */
   public static Map<String, Object> getAnnotationAttributes(
      java.lang.annotation.Annotation annotation, boolean classValuesAsString) {

      return getAnnotationAttributes(annotation, classValuesAsString, false);
   }

   /**
    * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
    * <p>This method provides fully recursive annotation reading capabilities on par with
    * the reflection-based {@link foundation.polar.gratify.core.type.StandardAnnotationMetadata}.
    * @param annotation the annotation to retrieve the attributes for
    * @param classValuesAsString whether to convert Class references into Strings (for
    * compatibility with {@link foundation.polar.gratify.core.type.AnnotationMetadata})
    * or to preserve them as Class references
    * @param nestedAnnotationsAsMap whether to convert nested annotations into
    * {@link AnnotationAttributes} maps (for compatibility with
    * {@link foundation.polar.gratify.core.type.AnnotationMetadata}) or to preserve them as
    * {@code Annotation} instances
    * @return the annotation attributes (a specialized Map) with attribute names as keys
    * and corresponding attribute values as values (never {@code null})
    */
   public static AnnotationAttributes getAnnotationAttributes(
      java.lang.annotation.Annotation annotation, boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

      return getAnnotationAttributes(null, annotation, classValuesAsString, nestedAnnotationsAsMap);
   }

   /**
    * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
    * <p>Equivalent to calling {@link #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation, boolean, boolean)}
    * with the {@code classValuesAsString} and {@code nestedAnnotationsAsMap} parameters
    * set to {@code false}.
    * @param annotatedElement the element that is annotated with the supplied annotation;
    * may be {@code null} if unknown
    * @param annotation the annotation to retrieve the attributes for
    * @return the annotation attributes (a specialized Map) with attribute names as keys
    * and corresponding attribute values as values (never {@code null})
    * @see #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation, boolean, boolean)
    */
   public static AnnotationAttributes getAnnotationAttributes(
      @Nullable AnnotatedElement annotatedElement, java.lang.annotation.Annotation annotation) {

      return getAnnotationAttributes(annotatedElement, annotation, false, false);
   }

   /**
    * Retrieve the given annotation's attributes as an {@link AnnotationAttributes} map.
    * <p>This method provides fully recursive annotation reading capabilities on par with
    * the reflection-based {@link foundation.polar.gratify.core.type.StandardAnnotationMetadata}.
    * @param annotatedElement the element that is annotated with the supplied annotation;
    * may be {@code null} if unknown
    * @param annotation the annotation to retrieve the attributes for
    * @param classValuesAsString whether to convert Class references into Strings (for
    * compatibility with {@link foundation.polar.gratify.core.type.AnnotationMetadata})
    * or to preserve them as Class references
    * @param nestedAnnotationsAsMap whether to convert nested annotations into
    * {@link AnnotationAttributes} maps (for compatibility with
    * {@link foundation.polar.gratify.core.type.AnnotationMetadata}) or to preserve them as
    * {@code Annotation} instances
    * @return the annotation attributes (a specialized Map) with attribute names as keys
    * and corresponding attribute values as values (never {@code null})
    */
   public static AnnotationAttributes getAnnotationAttributes(
      @Nullable AnnotatedElement annotatedElement, java.lang.annotation.Annotation annotation,
      boolean classValuesAsString, boolean nestedAnnotationsAsMap) {

      Adapt[] adaptations = Adapt.values(classValuesAsString, nestedAnnotationsAsMap);
      return MergedAnnotation.from(annotatedElement, annotation)
         .withNonMergedAttributes()
         .asMap(mergedAnnotation ->
            new AnnotationAttributes(mergedAnnotation.getType(), true), adaptations);
   }

   /**
    * Register the annotation-declared default values for the given attributes,
    * if available.
    * @param attributes the annotation attributes to process
    */
   public static void registerDefaultValues(AnnotationAttributes attributes) {
      Class<? extends java.lang.annotation.Annotation> annotationType = attributes.annotationType();
      if (annotationType != null && Modifier.isPublic(annotationType.getModifiers()) &&
         !AnnotationFilter.PLAIN.matches(annotationType)) {
         Map<String, DefaultValueHolder> defaultValues = getDefaultValues(annotationType);
         defaultValues.forEach(attributes::putIfAbsent);
      }
   }

   private static Map<String, DefaultValueHolder> getDefaultValues(
      Class<? extends java.lang.annotation.Annotation> annotationType) {

      return defaultValuesCache.computeIfAbsent(annotationType,
         AnnotationUtils::computeDefaultValues);
   }

   private static Map<String, DefaultValueHolder> computeDefaultValues(
      Class<? extends java.lang.annotation.Annotation> annotationType) {

      AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
      if (!methods.hasDefaultValueMethod()) {
         return Collections.emptyMap();
      }
      Map<String, DefaultValueHolder> result = new LinkedHashMap<>(methods.size());
      if (!methods.hasNestedAnnotation()) {
         // Use simpler method if there are no nested annotations
         for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            Object defaultValue = method.getDefaultValue();
            if (defaultValue != null) {
               result.put(method.getName(), new DefaultValueHolder(defaultValue));
            }
         }
      }
      else {
         // If we have nested annotations, we need them as nested maps
         AnnotationAttributes attributes = MergedAnnotation.of(annotationType)
            .asMap(annotation ->
               new AnnotationAttributes(annotation.getType(), true), Adapt.ANNOTATION_TO_MAP);
         for (Map.Entry<String, Object> element : attributes.entrySet()) {
            result.put(element.getKey(), new DefaultValueHolder(element.getValue()));
         }
      }
      return result;
   }

   /**
    * Post-process the supplied {@link AnnotationAttributes}, preserving nested
    * annotations as {@code Annotation} instances.
    * <p>Specifically, this method enforces <em>attribute alias</em> semantics
    * for annotation attributes that are annotated with {@link AliasFor @AliasFor}
    * and replaces default value placeholders with their original default values.
    * @param annotatedElement the element that is annotated with an annotation or
    * annotation hierarchy from which the supplied attributes were created;
    * may be {@code null} if unknown
    * @param attributes the annotation attributes to post-process
    * @param classValuesAsString whether to convert Class references into Strings (for
    * compatibility with {@link foundation.polar.gratify.core.type.AnnotationMetadata})
    * or to preserve them as Class references
    * @see #getDefaultValue(Class, String)
    */
   public static void postProcessAnnotationAttributes(@Nullable Object annotatedElement,
                                                      @Nullable AnnotationAttributes attributes, boolean classValuesAsString) {

      if (attributes == null) {
         return;
      }
      if (!attributes.validated) {
         Class<? extends java.lang.annotation.Annotation> annotationType = attributes.annotationType();
         if (annotationType == null) {
            return;
         }
         AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
         for (int i = 0; i < mapping.getMirrorSets().size(); i++) {
            MirrorSet mirrorSet = mapping.getMirrorSets().get(i);
            int resolved = mirrorSet.resolve(attributes.displayName, attributes,
               AnnotationUtils::getAttributeValueForMirrorResolution);
            if (resolved != -1) {
               Method attribute = mapping.getAttributes().get(resolved);
               Object value = attributes.get(attribute.getName());
               for (int j = 0; j < mirrorSet.size(); j++) {
                  Method mirror = mirrorSet.get(j);
                  if (mirror != attribute) {
                     attributes.put(mirror.getName(),
                        adaptValue(annotatedElement, value, classValuesAsString));
                  }
               }
            }
         }
      }
      for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
         String attributeName = attributeEntry.getKey();
         Object value = attributeEntry.getValue();
         if (value instanceof DefaultValueHolder) {
            value = ((DefaultValueHolder) value).defaultValue;
            attributes.put(attributeName,
               adaptValue(annotatedElement, value, classValuesAsString));
         }
      }
   }

   private static Object getAttributeValueForMirrorResolution(Method attribute, Object attributes) {
      Object result = ((AnnotationAttributes) attributes).get(attribute.getName());
      return (result instanceof DefaultValueHolder ? ((DefaultValueHolder) result).defaultValue : result);
   }

   @Nullable
   private static Object adaptValue(
      @Nullable Object annotatedElement, @Nullable Object value, boolean classValuesAsString) {

      if (classValuesAsString) {
         if (value instanceof Class) {
            return ((Class<?>) value).getName();
         }
         if (value instanceof Class[]) {
            Class<?>[] classes = (Class<?>[]) value;
            String[] names = new String[classes.length];
            for (int i = 0; i < classes.length; i++) {
               names[i] = classes[i].getName();
            }
            return names;
         }
      }
      if (value instanceof java.lang.annotation.Annotation) {
         java.lang.annotation.Annotation annotation = (java.lang.annotation.Annotation) value;
         return MergedAnnotation.from(annotatedElement, annotation).synthesize();
      }
      if (value instanceof java.lang.annotation.Annotation[]) {
         java.lang.annotation.Annotation[] annotations = (java.lang.annotation.Annotation[]) value;
         java.lang.annotation.Annotation[] synthesized = (java.lang.annotation.Annotation[]) Array.newInstance(
            annotations.getClass().getComponentType(), annotations.length);
         for (int i = 0; i < annotations.length; i++) {
            synthesized[i] = MergedAnnotation.from(annotatedElement, annotations[i]).synthesize();
         }
         return synthesized;
      }
      return value;
   }

   /**
    * Retrieve the <em>value</em> of the {@code value} attribute of a
    * single-element Annotation, given an annotation instance.
    * @param annotation the annotation instance from which to retrieve the value
    * @return the attribute value, or {@code null} if not found unless the attribute
    * value cannot be retrieved due to an {@link AnnotationConfigurationException},
    * in which case such an exception will be rethrown
    * @see #getValue(java.lang.annotation.Annotation, String)
    */
   @Nullable
   public static Object getValue(java.lang.annotation.Annotation annotation) {
      return getValue(annotation, VALUE);
   }

   /**
    * Retrieve the <em>value</em> of a named attribute, given an annotation instance.
    * @param annotation the annotation instance from which to retrieve the value
    * @param attributeName the name of the attribute value to retrieve
    * @return the attribute value, or {@code null} if not found unless the attribute
    * value cannot be retrieved due to an {@link AnnotationConfigurationException},
    * in which case such an exception will be rethrown
    * @see #getValue(java.lang.annotation.Annotation)
    */
   @Nullable
   public static Object getValue(java.lang.annotation.Annotation annotation, @Nullable String attributeName) {
      if (annotation == null || !StringUtils.hasText(attributeName)) {
         return null;
      }
      try {
         Method method = annotation.annotationType().getDeclaredMethod(attributeName);
         ReflectionUtils.makeAccessible(method);
         return method.invoke(annotation);
      }
      catch (NoSuchMethodException ex) {
         return null;
      }
      catch (InvocationTargetException ex) {
         rethrowAnnotationConfigurationException(ex.getTargetException());
         throw new IllegalStateException("Could not obtain value for annotation attribute '" +
            attributeName + "' in " + annotation, ex);
      }
      catch (Throwable ex) {
         handleIntrospectionFailure(annotation.getClass(), ex);
         return null;
      }
   }

   /**
    * If the supplied throwable is an {@link AnnotationConfigurationException},
    * it will be cast to an {@code AnnotationConfigurationException} and thrown,
    * allowing it to propagate to the caller.
    * <p>Otherwise, this method does nothing.
    * @param ex the throwable to inspect
    */
   static void rethrowAnnotationConfigurationException(Throwable ex) {
      if (ex instanceof AnnotationConfigurationException) {
         throw (AnnotationConfigurationException) ex;
      }
   }

   /**
    * Handle the supplied annotation introspection exception.
    * <p>If the supplied exception is an {@link AnnotationConfigurationException},
    * it will simply be thrown, allowing it to propagate to the caller, and
    * nothing will be logged.
    * <p>Otherwise, this method logs an introspection failure (in particular for
    * a {@link TypeNotPresentException}) before moving on, assuming nested
    * {@code Class} values were not resolvable within annotation attributes and
    * thereby effectively pretending there were no annotations on the specified
    * element.
    * @param element the element that we tried to introspect annotations on
    * @param ex the exception that we encountered
    * @see #rethrowAnnotationConfigurationException
    * @see IntrospectionFailureLogger
    */
   static void handleIntrospectionFailure(@Nullable AnnotatedElement element, Throwable ex) {
      rethrowAnnotationConfigurationException(ex);
      IntrospectionFailureLogger logger = IntrospectionFailureLogger.INFO;
      boolean meta = false;
      if (element instanceof Class && java.lang.annotation.Annotation.class.isAssignableFrom((Class<?>) element)) {
         // Meta-annotation or (default) value lookup on an annotation type
         logger = IntrospectionFailureLogger.DEBUG;
         meta = true;
      }
      if (logger.isEnabled()) {
         String message = meta ?
            "Failed to meta-introspect annotation " :
            "Failed to introspect annotations on ";
         logger.log(message + element + ": " + ex);
      }
   }

   /**
    * Retrieve the <em>default value</em> of the {@code value} attribute
    * of a single-element Annotation, given an annotation instance.
    * @param annotation the annotation instance from which to retrieve the default value
    * @return the default value, or {@code null} if not found
    * @see #getDefaultValue(java.lang.annotation.Annotation, String)
    */
   @Nullable
   public static Object getDefaultValue(java.lang.annotation.Annotation annotation) {
      return getDefaultValue(annotation, VALUE);
   }

   /**
    * Retrieve the <em>default value</em> of a named attribute, given an annotation instance.
    * @param annotation the annotation instance from which to retrieve the default value
    * @param attributeName the name of the attribute value to retrieve
    * @return the default value of the named attribute, or {@code null} if not found
    * @see #getDefaultValue(Class, String)
    */
   @Nullable
   public static Object getDefaultValue(java.lang.annotation.Annotation annotation, @Nullable String attributeName) {
      return (annotation != null ? getDefaultValue(annotation.annotationType(), attributeName) : null);
   }

   /**
    * Retrieve the <em>default value</em> of the {@code value} attribute
    * of a single-element Annotation, given the {@link Class annotation type}.
    * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
    * @return the default value, or {@code null} if not found
    * @see #getDefaultValue(Class, String)
    */
   @Nullable
   public static Object getDefaultValue(Class<? extends java.lang.annotation.Annotation> annotationType) {
      return getDefaultValue(annotationType, VALUE);
   }

   /**
    * Retrieve the <em>default value</em> of a named attribute, given the
    * {@link Class annotation type}.
    * @param annotationType the <em>annotation type</em> for which the default value should be retrieved
    * @param attributeName the name of the attribute value to retrieve.
    * @return the default value of the named attribute, or {@code null} if not found
    * @see #getDefaultValue(java.lang.annotation.Annotation, String)
    */
   @Nullable
   public static Object getDefaultValue(
      @Nullable Class<? extends java.lang.annotation.Annotation> annotationType, @Nullable String attributeName) {

      if (annotationType == null || !StringUtils.hasText(attributeName)) {
         return null;
      }
      return MergedAnnotation.of(annotationType).getDefaultValue(attributeName).orElse(null);
   }

   /**
    * <em>Synthesize</em> an annotation from the supplied {@code annotation}
    * by wrapping it in a dynamic proxy that transparently enforces
    * <em>attribute alias</em> semantics for annotation attributes that are
    * annotated with {@link AliasFor @AliasFor}.
    * @param annotation the annotation to synthesize
    * @param annotatedElement the element that is annotated with the supplied
    * annotation; may be {@code null} if unknown
    * @return the synthesized annotation if the supplied annotation is
    * <em>synthesizable</em>; {@code null} if the supplied annotation is
    * {@code null}; otherwise the supplied annotation unmodified
    * @throws AnnotationConfigurationException if invalid configuration of
    * {@code @AliasFor} is detected
    * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
    * @see #synthesizeAnnotation(Class)
    */
   public static <A extends java.lang.annotation.Annotation> A synthesizeAnnotation(
      A annotation, @Nullable AnnotatedElement annotatedElement) {

      if (annotation instanceof SynthesizedAnnotation || AnnotationFilter.PLAIN.matches(annotation)) {
         return annotation;
      }
      return MergedAnnotation.from(annotatedElement, annotation).synthesize();
   }

   /**
    * <em>Synthesize</em> an annotation from its default attributes values.
    * <p>This method simply delegates to
    * {@link #synthesizeAnnotation(Map, Class, AnnotatedElement)},
    * supplying an empty map for the source attribute values and {@code null}
    * for the {@link AnnotatedElement}.
    * @param annotationType the type of annotation to synthesize
    * @return the synthesized annotation
    * @throws IllegalArgumentException if a required attribute is missing
    * @throws AnnotationConfigurationException if invalid configuration of
    * {@code @AliasFor} is detected
    * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
    * @see #synthesizeAnnotation(java.lang.annotation.Annotation, AnnotatedElement)
    */
   public static <A extends java.lang.annotation.Annotation> A synthesizeAnnotation(Class<A> annotationType) {
      return synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
   }

   /**
    * <em>Synthesize</em> an annotation from the supplied map of annotation
    * attributes by wrapping the map in a dynamic proxy that implements an
    * annotation of the specified {@code annotationType} and transparently
    * enforces <em>attribute alias</em> semantics for annotation attributes
    * that are annotated with {@link AliasFor @AliasFor}.
    * <p>The supplied map must contain a key-value pair for every attribute
    * defined in the supplied {@code annotationType} that is not aliased or
    * does not have a default value. Nested maps and nested arrays of maps
    * will be recursively synthesized into nested annotations or nested
    * arrays of annotations, respectively.
    * <p>Note that {@link AnnotationAttributes} is a specialized type of
    * {@link Map} that is an ideal candidate for this method's
    * {@code attributes} argument.
    * @param attributes the map of annotation attributes to synthesize
    * @param annotationType the type of annotation to synthesize
    * @param annotatedElement the element that is annotated with the annotation
    * corresponding to the supplied attributes; may be {@code null} if unknown
    * @return the synthesized annotation
    * @throws IllegalArgumentException if a required attribute is missing or if an
    * attribute is not of the correct type
    * @throws AnnotationConfigurationException if invalid configuration of
    * {@code @AliasFor} is detected
    * @see #synthesizeAnnotation(java.lang.annotation.Annotation, AnnotatedElement)
    * @see #synthesizeAnnotation(Class)
    * @see #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation)
    * @see #getAnnotationAttributes(AnnotatedElement, java.lang.annotation.Annotation, boolean, boolean)
    */
   public static <A extends java.lang.annotation.Annotation> A synthesizeAnnotation(Map<String, Object> attributes,
                                                                                    Class<A> annotationType, @Nullable AnnotatedElement annotatedElement) {

      try {
         return MergedAnnotation.of(annotatedElement, annotationType, attributes).synthesize();
      }
      catch (NoSuchElementException | IllegalStateException ex) {
         throw new IllegalArgumentException(ex);
      }
   }

   /**
    * <em>Synthesize</em> an array of annotations from the supplied array
    * of {@code annotations} by creating a new array of the same size and
    * type and populating it with {@linkplain #synthesizeAnnotation(java.lang.annotation.Annotation,
    * AnnotatedElement) synthesized} versions of the annotations from the input
    * array.
    * @param annotations the array of annotations to synthesize
    * @param annotatedElement the element that is annotated with the supplied
    * array of annotations; may be {@code null} if unknown
    * @return a new array of synthesized annotations, or {@code null} if
    * the supplied array is {@code null}
    * @throws AnnotationConfigurationException if invalid configuration of
    * {@code @AliasFor} is detected
    * @see #synthesizeAnnotation(java.lang.annotation.Annotation, AnnotatedElement)
    * @see #synthesizeAnnotation(Map, Class, AnnotatedElement)
    */
   static java.lang.annotation.Annotation[] synthesizeAnnotationArray(java.lang.annotation.Annotation[] annotations, AnnotatedElement annotatedElement) {
      if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(annotatedElement)) {
         return annotations;
      }
      java.lang.annotation.Annotation[] synthesized = (Annotation[]) Array.newInstance(
         annotations.getClass().getComponentType(), annotations.length);
      for (int i = 0; i < annotations.length; i++) {
         synthesized[i] = synthesizeAnnotation(annotations[i], annotatedElement);
      }
      return synthesized;
   }

   /**
    * Clear the internal annotation metadata cache.
    */
   public static void clearCache() {
      AnnotationTypeMappings.clearCache();
      AnnotationsScanner.clearCache();
   }


   /**
    * Internal holder used to wrap default values.
    */
   private static class DefaultValueHolder {

      final Object defaultValue;

      public DefaultValueHolder(Object defaultValue) {
         this.defaultValue = defaultValue;
      }

      @Override
      public String toString() {
         return "*" + this.defaultValue;
      }
   }
}
