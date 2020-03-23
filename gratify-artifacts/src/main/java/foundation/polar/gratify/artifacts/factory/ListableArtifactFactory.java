package foundation.polar.gratify.artifacts.factory;


import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.core.ResolvableType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Extension of the {@link ArtifactFactory} interface to be implemented by bean factories
 * that can enumerate all their bean instances, rather than attempting bean lookup
 * by name one by one as requested by clients. ArtifactFactory implementations that
 * preload all their bean definitions (such as XML-based factories) may implement
 * this interface.
 *
 * <p>If this is a {@link HierarchicalArtifactFactory}, the return values will <i>not</i>
 * take any ArtifactFactory hierarchy into account, but will relate only to the beans
 * defined in the current factory. Use the {@link ArtifactFactoryUtils} helper class
 * to consider beans in ancestor factories too.
 *
 * <p>The methods in this interface will just respect bean definitions of this factory.
 * They will ignore any singleton beans that have been registered by other means like
 * {@link foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory}'s
 * {@code registerSingleton} method, with the exception of
 * {@code getArtifactNamesOfType} and {@code getArtifactsOfType} which will check
 * such manually registered singletons too. Of course, ArtifactFactory's {@code getArtifact}
 * does allow transparent access to such special beans as well. However, in typical
 * scenarios, all beans will be defined by external bean definitions anyway, so most
 * applications don't need to worry about this differentiation.
 *
 * <p><b>NOTE:</b> With the exception of {@code getArtifactDefinitionCount}
 * and {@code containsArtifactDefinition}, the methods in this interface
 * are not designed for frequent invocation. Implementations may be slow.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 *
 * @see HierarchicalArtifactFactory
 * @see ArtifactFactoryUtils
 */
public interface ListableArtifactFactory extends ArtifactFactory{
   /**
    * Check if this bean factory contains a bean definition with the given name.
    * <p>Does not consider any hierarchy this factory may participate in,
    * and ignores any singleton beans that have been registered by
    * other means than bean definitions.
    * @param artifactName the name of the bean to look for
    * @return if this bean factory contains a bean definition with the given name
    * @see #containsArtifact
    */
   boolean containsArtifactDefinition(String artifactName);

   /**
    * Return the number of beans defined in the factory.
    * <p>Does not consider any hierarchy this factory may participate in,
    * and ignores any singleton beans that have been registered by
    * other means than bean definitions.
    * @return the number of beans defined in the factory
    */
   int getArtifactDefinitionCount();

   /**
    * Return the names of all beans defined in this factory.
    * <p>Does not consider any hierarchy this factory may participate in,
    * and ignores any singleton beans that have been registered by
    * other means than bean definitions.
    * @return the names of all beans defined in this factory,
    * or an empty array if none defined
    */
   String[] getArtifactDefinitionNames();

   /**
    * Return the names of beans matching the given type (including subclasses),
    * judging from either bean definitions or the value of {@code getObjectType}
    * in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code artifactNamesForTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>This version of {@code getArtifactNamesForType} matches all kinds of beans,
    * be it singletons, prototypes, or FactoryArtifacts. In most implementations, the
    * result will be the same as for {@code getArtifactNamesForType(type, true, true)}.
    * <p>Artifact names returned by this method should always return bean names <i>in the
    * order of definition</i> in the backend configuration, as far as possible.
    * @param type the generically typed class or interface to match
    * @return the names of beans (or objects created by FactoryArtifacts) matching
    * the given object type (including subclasses), or an empty array if none
    *
    * @see #isTypeMatch(String, ResolvableType)
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#artifactNamesForTypeIncludingAncestors(ListableArtifactFactory, ResolvableType)
    */
   String[] getArtifactNamesForType(ResolvableType type);

   /**
    * Return the names of beans matching the given type (including subclasses),
    * judging from either bean definitions or the value of {@code getObjectType}
    * in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit" flag is set,
    * which means that FactoryArtifacts will get initialized. If the object created by the
    * FactoryArtifact doesn't match, the raw FactoryArtifact itself will be matched against the
    * type. If "allowEagerInit" is not set, only raw FactoryArtifacts will be checked
    * (which doesn't require initialization of each FactoryArtifact).
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code artifactNamesForTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>Artifact names returned by this method should always return bean names <i>in the
    * order of definition</i> in the backend configuration, as far as possible.
    * @param type the generically typed class or interface to match
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the names of beans (or objects created by FactoryArtifacts) matching
    * the given object type (including subclasses), or an empty array if none
    *
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#artifactNamesForTypeIncludingAncestors(ListableArtifactFactory, ResolvableType, boolean, boolean)
    */
   String[] getArtifactNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit);

   /**
    * Return the names of beans matching the given type (including subclasses),
    * judging from either bean definitions or the value of {@code getObjectType}
    * in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code artifactNamesForTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>This version of {@code getArtifactNamesForType} matches all kinds of beans,
    * be it singletons, prototypes, or FactoryArtifacts. In most implementations, the
    * result will be the same as for {@code getArtifactNamesForType(type, true, true)}.
    * <p>Artifact names returned by this method should always return bean names <i>in the
    * order of definition</i> in the backend configuration, as far as possible.
    * @param type the class or interface to match, or {@code null} for all bean names
    * @return the names of beans (or objects created by FactoryArtifacts) matching
    * the given object type (including subclasses), or an empty array if none
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#artifactNamesForTypeIncludingAncestors(ListableArtifactFactory, Class)
    */
   String[] getArtifactNamesForType(@Nullable Class<?> type);

   /**
    * Return the names of beans matching the given type (including subclasses),
    * judging from either bean definitions or the value of {@code getObjectType}
    * in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit" flag is set,
    * which means that FactoryArtifacts will get initialized. If the object created by the
    * FactoryArtifact doesn't match, the raw FactoryArtifact itself will be matched against the
    * type. If "allowEagerInit" is not set, only raw FactoryArtifacts will be checked
    * (which doesn't require initialization of each FactoryArtifact).
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code artifactNamesForTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>Artifact names returned by this method should always return bean names <i>in the
    * order of definition</i> in the backend configuration, as far as possible.
    * @param type the class or interface to match, or {@code null} for all bean names
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return the names of beans (or objects created by FactoryArtifacts) matching
    * the given object type (including subclasses), or an empty array if none
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#artifactNamesForTypeIncludingAncestors(ListableArtifactFactory, Class, boolean, boolean)
    */
   String[] getArtifactNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);

   /**
    * Return the bean instances that match the given object type (including
    * subclasses), judging from either bean definitions or the value of
    * {@code getObjectType} in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts, which means that FactoryArtifacts
    * will get initialized. If the object created by the FactoryArtifact doesn't match,
    * the raw FactoryArtifact itself will be matched against the type.
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code beansOfTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>This version of getArtifactsOfType matches all kinds of beans, be it
    * singletons, prototypes, or FactoryArtifacts. In most implementations, the
    * result will be the same as for {@code getArtifactsOfType(type, true, true)}.
    * <p>The Map returned by this method should always return bean names and
    * corresponding bean instances <i>in the order of definition</i> in the
    * backend configuration, as far as possible.
    * @param type the class or interface to match, or {@code null} for all concrete beans
    * @return a Map with the matching beans, containing the bean names as
    * keys and the corresponding bean instances as values
    * @throws ArtifactsException if a bean could not be created
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#beansOfTypeIncludingAncestors(ListableArtifactFactory, Class)
    */
   <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type) throws ArtifactsException;

   /**
    * Return the bean instances that match the given object type (including
    * subclasses), judging from either bean definitions or the value of
    * {@code getObjectType} in the case of FactoryArtifacts.
    * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
    * check nested beans which might match the specified type as well.
    * <p>Does consider objects created by FactoryArtifacts if the "allowEagerInit" flag is set,
    * which means that FactoryArtifacts will get initialized. If the object created by the
    * FactoryArtifact doesn't match, the raw FactoryArtifact itself will be matched against the
    * type. If "allowEagerInit" is not set, only raw FactoryArtifacts will be checked
    * (which doesn't require initialization of each FactoryArtifact).
    * <p>Does not consider any hierarchy this factory may participate in.
    * Use ArtifactFactoryUtils' {@code beansOfTypeIncludingAncestors}
    * to include beans in ancestor factories too.
    * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
    * by other means than bean definitions.
    * <p>The Map returned by this method should always return bean names and
    * corresponding bean instances <i>in the order of definition</i> in the
    * backend configuration, as far as possible.
    * @param type the class or interface to match, or {@code null} for all concrete beans
    * @param includeNonSingletons whether to include prototype or scoped beans too
    * or just singletons (also applies to FactoryArtifacts)
    * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
    * <i>objects created by FactoryArtifacts</i> (or by factory methods with a
    * "factory-bean" reference) for the type check. Note that FactoryArtifacts need to be
    * eagerly initialized to determine their type: So be aware that passing in "true"
    * for this flag will initialize FactoryArtifacts and "factory-bean" references.
    * @return a Map with the matching beans, containing the bean names as
    * keys and the corresponding bean instances as values
    * @throws ArtifactsException if a bean could not be created
    * @see FactoryArtifact#getObjectType
    * @see ArtifactFactoryUtils#beansOfTypeIncludingAncestors(ListableArtifactFactory, Class, boolean, boolean)
    */
   <T> Map<String, T> getArtifactsOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
      throws ArtifactsException;

   /**
    * Find all names of beans which are annotated with the supplied {@link Annotation}
    * type, without creating corresponding bean instances yet.
    * <p>Note that this method considers objects created by FactoryArtifacts, which means
    * that FactoryArtifacts will get initialized in order to determine their object type.
    * @param annotationType the type of annotation to look for
    * (at class, interface or factory method level of the specified bean)
    * @return the names of all matching beans
    *
    * @see #findAnnotationOnArtifact
    */
   String[] getArtifactNamesForAnnotation(Class<? extends Annotation> annotationType);

   /**
    * Find all beans which are annotated with the supplied {@link Annotation} type,
    * returning a Map of bean names with corresponding bean instances.
    * <p>Note that this method considers objects created by FactoryArtifacts, which means
    * that FactoryArtifacts will get initialized in order to determine their object type.
    * @param annotationType the type of annotation to look for
    * (at class, interface or factory method level of the specified bean)
    * @return a Map with the matching beans, containing the bean names as
    * keys and the corresponding bean instances as values
    * @throws ArtifactsException if a bean could not be created
    * @see #findAnnotationOnArtifact
    */
   Map<String, Object> getArtifactsWithAnnotation(Class<? extends Annotation> annotationType) throws ArtifactsException;

   /**
    * Find an {@link Annotation} of {@code annotationType} on the specified bean,
    * traversing its interfaces and super classes if no annotation can be found on
    * the given class itself, as well as checking the bean's factory method (if any).
    * @param artifactName the name of the bean to look for annotations on
    * @param annotationType the type of annotation to look for
    * (at class, interface or factory method level of the specified bean)
    * @return the annotation of the given type if found, or {@code null} otherwise
    * @throws NoSuchArtifactDefinitionException if there is no bean with the given name
    * @see #getArtifactNamesForAnnotation
    * @see #getArtifactsWithAnnotation
    */
   @Nullable
   <A extends Annotation> A findAnnotationOnArtifact(String artifactName, Class<A> annotationType)
      throws NoSuchArtifactDefinitionException;
}
