package foundation.polar.gratify.artifacts.factory;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convenience methods operating on bean factories, in particular
 * on the {@link ListableArtifactFactory} interface.
 *
 * <p>Returns bean counts, bean names or bean instances,
 * taking into account the nesting hierarchy of a bean factory
 * (which the methods defined on the ListableArtifactFactory interface don't,
 * in contrast to the methods defined on the ArtifactFactory interface).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public abstract class ArtifactFactoryUtils {
   /**
    * Separator for generated bean names. If a class name or parent name is not
    * unique, "#1", "#2" etc will be appended, until the name becomes unique.
    */
   public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

   /**
    * Cache from name with factory bean prefix to stripped name without dereference.
    * @see ArtifactFactory#FACTORY_BEAN_PREFIX
    */
   private static final Map<String, String> transformedArtifactNameCache = new ConcurrentHashMap<>();


   /**
    * Return whether the given name is a factory dereference
    * (beginning with the factory dereference prefix).
    * @param name the name of the bean
    * @return whether the given name is a factory dereference
    * @see ArtifactFactory#FACTORY_BEAN_PREFIX
    */
   public static boolean isFactoryDereference(@Nullable String name) {
      return (name != null && name.startsWith(ArtifactFactory.FACTORY_BEAN_PREFIX));
   }

   /**
    * Return the actual bean name, stripping out the factory dereference
    * prefix (if any, also stripping repeated factory prefixes if found).
    * @param name the name of the bean
    * @return the transformed name
    * @see ArtifactFactory#FACTORY_BEAN_PREFIX
    */
   public static String transformedArtifactName(String name) {
      AssertUtils.notNull(name, "'name' must not be null");
      if (!name.startsWith(ArtifactFactory.FACTORY_BEAN_PREFIX)) {
         return name;
      }
      return transformedArtifactNameCache.computeIfAbsent(name, artifactName -> {
         do {
            artifactName = artifactName.substring(ArtifactFactory.FACTORY_BEAN_PREFIX.length());
         }
         while (artifactName.startsWith(ArtifactFactory.FACTORY_BEAN_PREFIX));
         return artifactName;
      });
   }

   /**
    * Return whether the given name is a bean name which has been generated
    * by the default naming strategy (containing a "#..." part).
    * @param name the name of the bean
    * @return whether the given name is a generated bean name
    * @see #GENERATED_BEAN_NAME_SEPARATOR
    * @see foundation.polar.gratify.artifacts.factory.support.ArtifactDefinitionReaderUtils#generateArtifactName
    * @see foundation.polar.gratify.artifacts.factory.support.DefaultArtifactNameGenerator
    */
   public static boolean isGeneratedArtifactName(@Nullable String name) {
      return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
   }

   /**
    * Extract the "raw" bean name from the given (potentially generated) bean name,
    * excluding any "#..." suffixes which might have been added for uniqueness.
    * @param name the potentially generated bean name
    * @return the raw bean name
    * @see #GENERATED_BEAN_NAME_SEPARATOR
    */
   public static String originalArtifactName(String name) {
      AssertUtils.notNull(name, "'name' must not be null");
      int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
      return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
   }


   // Retrieval of bean names

   /**
    * Count all beans in any hierarchy in which this factory participates.
    * Includes counts of ancestor bean factories.
    * <p>Artifacts that are "overridden" (specified in a descendant factory
    * with the same name) are only counted once.
    * @param lbf the bean factory
    * @return count of beans including those defined in ancestor factories
    * @see #artifactNamesIncludingAncestors
    */
   public static int countArtifactsIncludingAncestors(ListableArtifactFactory lbf) {
      return artifactNamesIncludingAncestors(lbf).length;
   }

   /**
    * Return all bean names in the factory, including ancestor factories.
    * @param lbf the bean factory
    * @return the array of matching bean names, or an empty array if none
    * @see #artifactNamesForTypeIncludingAncestors
    */
   public static String[] artifactNamesIncludingAncestors(ListableArtifactFactory lbf) {
      return artifactNamesForTypeIncludingAncestors(lbf, Object.class);
   }

   /**
    * Get all bean names for the given type, including those defined in ancestor
    * factories. Will return unique names in case of overridden bean definitions.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>This version of {@code artifactNamesForTypeIncludingAncestors} automatically
    * includes prototypes and FactoryArtifacts.
    * @param lbf the bean factory
    * @param type the type that beans must match (as a {@code ResolvableType})
    * @return the array of matching bean names, or an empty array if none
    *
    * @see ListableArtifactFactory#getArtifactNamesForType(ResolvableType)
    */
   public static String[] artifactNamesForTypeIncludingAncestors(ListableArtifactFactory lbf, ResolvableType type) {
      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      String[] result = lbf.getArtifactNamesForType(type);
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            String[] parentResult = artifactNamesForTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type);
            result = mergeNamesWithParent(result, parentResult, hbf);
         }
      }
      return result;
   }

   /**
    * Get all bean names for the given type, including those defined in ancestor
    * factories. Will return unique names in case of overridden bean definitions.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit"
    * flag is set, which means that FactoryArtifacts will get initialized. If the
    * object created by the FactoryArtifact doesn't match, the raw FactoryArtifact itself
    * will be matched against the type. If "allowEagerInit" is not set,
    * only raw FactoryArtifacts will be checked (which doesn't require initialization
    * of each FactoryArtifact).
    * @param lbf the bean factory
    * @param type the type that beans must match (as a {@code ResolvableType})
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the array of matching bean names, or an empty array if none
    *
    * @see ListableArtifactFactory#getArtifactNamesForType(ResolvableType, boolean, boolean)
    */
   public static String[] artifactNamesForTypeIncludingAncestors(
      ListableArtifactFactory lbf, ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      String[] result = lbf.getArtifactNamesForType(type, includeNonSingletons, allowEagerInit);
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            String[] parentResult = artifactNamesForTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type, includeNonSingletons, allowEagerInit);
            result = mergeNamesWithParent(result, parentResult, hbf);
         }
      }
      return result;
   }

   /**
    * Get all bean names for the given type, including those defined in ancestor
    * factories. Will return unique names in case of overridden bean definitions.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>This version of {@code artifactNamesForTypeIncludingAncestors} automatically
    * includes prototypes and FactoryArtifacts.
    * @param lbf the bean factory
    * @param type the type that beans must match (as a {@code Class})
    * @return the array of matching bean names, or an empty array if none
    * @see ListableArtifactFactory#getArtifactNamesForType(Class)
    */
   public static String[] artifactNamesForTypeIncludingAncestors(ListableArtifactFactory lbf, Class<?> type) {
      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      String[] result = lbf.getArtifactNamesForType(type);
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            String[] parentResult = artifactNamesForTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type);
            result = mergeNamesWithParent(result, parentResult, hbf);
         }
      }
      return result;
   }

   /**
    * Get all bean names for the given type, including those defined in ancestor
    * factories. Will return unique names in case of overridden bean definitions.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit"
    * flag is set, which means that FactoryArtifacts will get initialized. If the
    * object created by the FactoryArtifact doesn't match, the raw FactoryArtifact itself
    * will be matched against the type. If "allowEagerInit" is not set,
    * only raw FactoryArtifacts will be checked (which doesn't require initialization
    * of each FactoryArtifact).
    * @param lbf the bean factory
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @param type the type that beans must match
    * @return the array of matching bean names, or an empty array if none
    * @see ListableArtifactFactory#getArtifactNamesForType(Class, boolean, boolean)
    */
   public static String[] artifactNamesForTypeIncludingAncestors(
      ListableArtifactFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      String[] result = lbf.getArtifactNamesForType(type, includeNonSingletons, allowEagerInit);
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            String[] parentResult = artifactNamesForTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type, includeNonSingletons, allowEagerInit);
            result = mergeNamesWithParent(result, parentResult, hbf);
         }
      }
      return result;
   }

   /**
    * Get all bean names whose {@code Class} has the supplied {@link Annotation}
    * type, including those defined in ancestor factories, without creating any bean
    * instances yet. Will return unique names in case of overridden bean definitions.
    * @param lbf the bean factory
    * @param annotationType the type of annotation to look for
    * @return the array of matching bean names, or an empty array if none
    *
    * @see ListableArtifactFactory#getArtifactNamesForAnnotation(Class)
    */
   public static String[] artifactNamesForAnnotationIncludingAncestors(
      ListableArtifactFactory lbf, Class<? extends Annotation> annotationType) {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      String[] result = lbf.getArtifactNamesForAnnotation(annotationType);
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            String[] parentResult = artifactNamesForAnnotationIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), annotationType);
            result = mergeNamesWithParent(result, parentResult, hbf);
         }
      }
      return result;
   }

   // Retrieval of bean instances

   /**
    * Return all beans of the given type or subtypes, also picking up beans defined in
    * ancestor bean factories if the current bean factory is a HierarchicalArtifactFactory.
    * The returned Map will only contain beans of this type.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p><b>Note: Artifacts of the same name will take precedence at the 'lowest' factory level,
    * i.e. such beans will be returned from the lowest factory that they are being found in,
    * hiding corresponding beans in ancestor factories.</b> This feature allows for
    * 'replacing' beans by explicitly choosing the same bean name in a child factory;
    * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
    * @param lbf the bean factory
    * @param type type of bean to match
    * @return the Map of matching bean instances, or an empty Map if none
    * @throws ArtifactsException if a bean could not be created
    * @see ListableArtifactFactory#getArtifactsOfType(Class)
    */
   public static <T> Map<String, T> beansOfTypeIncludingAncestors(ListableArtifactFactory lbf, Class<T> type)
      throws ArtifactsException {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      Map<String, T> result = new LinkedHashMap<>(4);
      result.putAll(lbf.getArtifactsOfType(type));
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            Map<String, T> parentResult = beansOfTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type);
            parentResult.forEach((artifactName, beanInstance) -> {
               if (!result.containsKey(artifactName) && !hbf.containsLocalArtifact(artifactName)) {
                  result.put(artifactName, beanInstance);
               }
            });
         }
      }
      return result;
   }

   /**
    * Return all beans of the given type or subtypes, also picking up beans defined in
    * ancestor bean factories if the current bean factory is a HierarchicalArtifactFactory.
    * The returned Map will only contain beans of this type.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit" flag is set,
    * which means that FactoryArtifacts will get initialized. If the object created by the
    * FactoryArtifact doesn't match, the raw FactoryArtifact itself will be matched against the
    * type. If "allowEagerInit" is not set, only raw FactoryArtifacts will be checked
    * (which doesn't require initialization of each FactoryArtifact).
    * <p><b>Note: Artifacts of the same name will take precedence at the 'lowest' factory level,
    * i.e. such beans will be returned from the lowest factory that they are being found in,
    * hiding corresponding beans in ancestor factories.</b> This feature allows for
    * 'replacing' beans by explicitly choosing the same bean name in a child factory;
    * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
    * @param lbf the bean factory
    * @param type type of bean to match
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the Map of matching bean instances, or an empty Map if none
    * @throws ArtifactsException if a bean could not be created
    * @see ListableArtifactFactory#getArtifactsOfType(Class, boolean, boolean)
    */
   public static <T> Map<String, T> beansOfTypeIncludingAncestors(
      ListableArtifactFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      Map<String, T> result = new LinkedHashMap<>(4);
      result.putAll(lbf.getArtifactsOfType(type, includeNonSingletons, allowEagerInit));
      if (lbf instanceof HierarchicalArtifactFactory) {
         HierarchicalArtifactFactory hbf = (HierarchicalArtifactFactory) lbf;
         if (hbf.getParentArtifactFactory() instanceof ListableArtifactFactory) {
            Map<String, T> parentResult = beansOfTypeIncludingAncestors(
               (ListableArtifactFactory) hbf.getParentArtifactFactory(), type, includeNonSingletons, allowEagerInit);
            parentResult.forEach((artifactName, beanInstance) -> {
               if (!result.containsKey(artifactName) && !hbf.containsLocalArtifact(artifactName)) {
                  result.put(artifactName, beanInstance);
               }
            });
         }
      }
      return result;
   }

   /**
    * Return a single bean of the given type or subtypes, also picking up beans
    * defined in ancestor bean factories if the current bean factory is a
    * HierarchicalArtifactFactory. Useful convenience method when we expect a
    * single bean and don't care about the bean name.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>This version of {@code beanOfTypeIncludingAncestors} automatically includes
    * prototypes and FactoryArtifacts.
    * <p><b>Note: Artifacts of the same name will take precedence at the 'lowest' factory level,
    * i.e. such beans will be returned from the lowest factory that they are being found in,
    * hiding corresponding beans in ancestor factories.</b> This feature allows for
    * 'replacing' beans by explicitly choosing the same bean name in a child factory;
    * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
    * @param lbf the bean factory
    * @param type type of bean to match
    * @return the matching bean instance
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    * @throws ArtifactsException if the bean could not be created
    * @see #beansOfTypeIncludingAncestors(ListableArtifactFactory, Class)
    */
   public static <T> T beanOfTypeIncludingAncestors(ListableArtifactFactory lbf, Class<T> type)
      throws ArtifactsException {

      Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type);
      return uniqueArtifact(type, beansOfType);
   }

   /**
    * Return a single bean of the given type or subtypes, also picking up beans
    * defined in ancestor bean factories if the current bean factory is a
    * HierarchicalArtifactFactory. Useful convenience method when we expect a
    * single bean and don't care about the bean name.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit" flag is set,
    * which means that FactoryArtifacts will get initialized. If the object created by the
    * FactoryArtifact doesn't match, the raw FactoryArtifact itself will be matched against the
    * type. If "allowEagerInit" is not set, only raw FactoryArtifacts will be checked
    * (which doesn't require initialization of each FactoryArtifact).
    * <p><b>Note: Artifacts of the same name will take precedence at the 'lowest' factory level,
    * i.e. such beans will be returned from the lowest factory that they are being found in,
    * hiding corresponding beans in ancestor factories.</b> This feature allows for
    * 'replacing' beans by explicitly choosing the same bean name in a child factory;
    * the bean in the ancestor factory won't be visible then, not even for by-type lookups.
    * @param lbf the bean factory
    * @param type type of bean to match
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the matching bean instance
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    * @throws ArtifactsException if the bean could not be created
    * @see #beansOfTypeIncludingAncestors(ListableArtifactFactory, Class, boolean, boolean)
    */
   public static <T> T beanOfTypeIncludingAncestors(
      ListableArtifactFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException {

      Map<String, T> beansOfType = beansOfTypeIncludingAncestors(lbf, type, includeNonSingletons, allowEagerInit);
      return uniqueArtifact(type, beansOfType);
   }

   /**
    * Return a single bean of the given type or subtypes, not looking in ancestor
    * factories. Useful convenience method when we expect a single bean and
    * don't care about the bean name.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>This version of {@code beanOfType} automatically includes
    * prototypes and FactoryArtifacts.
    * @param lbf the bean factory
    * @param type type of bean to match
    * @return the matching bean instance
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    * @throws ArtifactsException if the bean could not be created
    * @see ListableArtifactFactory#getArtifactsOfType(Class)
    */
   public static <T> T beanOfType(ListableArtifactFactory lbf, Class<T> type) throws ArtifactsException {
      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      Map<String, T> beansOfType = lbf.getArtifactsOfType(type);
      return uniqueArtifact(type, beansOfType);
   }

   /**
    * Return a single bean of the given type or subtypes, not looking in ancestor
    * factories. Useful convenience method when we expect a single bean and
    * don't care about the bean name.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit"
    * flag is set, which means that FactoryArtifacts will get initialized. If the
    * object created by the FactoryArtifact doesn't match, the raw FactoryArtifact itself
    * will be matched against the type. If "allowEagerInit" is not set,
    * only raw FactoryArtifacts will be checked (which doesn't require initialization
    * of each FactoryArtifact).
    * @param lbf the bean factory
    * @param type type of bean to match
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the matching bean instance
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    * @throws ArtifactsException if the bean could not be created
    * @see ListableArtifactFactory#getArtifactsOfType(Class, boolean, boolean)
    */
   public static <T> T beanOfType(
      ListableArtifactFactory lbf, Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException {

      AssertUtils.notNull(lbf, "ListableArtifactFactory must not be null");
      Map<String, T> beansOfType = lbf.getArtifactsOfType(type, includeNonSingletons, allowEagerInit);
      return uniqueArtifact(type, beansOfType);
   }


   /**
    * Merge the given bean names result with the given parent result.
    * @param result the local bean name result
    * @param parentResult the parent bean name result (possibly empty)
    * @param hbf the local bean factory
    * @return the merged result (possibly the local result as-is)
    */
   private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalArtifactFactory hbf) {
      if (parentResult.length == 0) {
         return result;
      }
      List<String> merged = new ArrayList<>(result.length + parentResult.length);
      merged.addAll(Arrays.asList(result));
      for (String artifactName : parentResult) {
         if (!merged.contains(artifactName) && !hbf.containsLocalArtifact(artifactName)) {
            merged.add(artifactName);
         }
      }
      return StringUtils.toStringArray(merged);
   }

   /**
    * Extract a unique bean for the given type from the given Map of matching beans.
    * @param type type of bean to match
    * @param matchingArtifacts all matching beans found
    * @return the unique bean instance
    * @throws NoSuchArtifactDefinitionException if no bean of the given type was found
    * @throws NoUniqueArtifactDefinitionException if more than one bean of the given type was found
    */
   private static <T> T uniqueArtifact(Class<T> type, Map<String, T> matchingArtifacts) {
      int count = matchingArtifacts.size();
      if (count == 1) {
         return matchingArtifacts.values().iterator().next();
      }
      else if (count > 1) {
         throw new NoUniqueArtifactDefinitionException(type, matchingArtifacts.keySet());
      }
      else {
         throw new NoSuchArtifactDefinitionException(type);
      }
   }

}
