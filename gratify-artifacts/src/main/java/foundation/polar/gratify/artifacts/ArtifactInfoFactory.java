package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * Strategy interface for creating {@link BeanInfo} instances for Gratify beans.
 * Can be used to plug in custom bean property resolution strategies (e.g. for other
 * languages on the JVM) or more efficient {@link BeanInfo} retrieval algorithms.
 *
 * <p>ArtifactInfoFactories are instantiated by the {@link CachedIntrospectionResults},
 * by using the {@link foundation.polar.gratify.core.io.support.GratifyFactoriesLoader}
 * utility class.
 *
 * When a {@link BeanInfo} is to be created, the {@code CachedIntrospectionResults}
 * will iterate through the discovered factories, calling {@link #getArtifactInfo(Class)}
 * on each one. If {@code null} is returned, the next factory will be queried.
 * If none of the factories support the class, a standard {@link BeanInfo} will be
 * created as a default.
 *
 * <p>Note that the {@link foundation.polar.gratify.core.io.support.GratifyFactoriesLoader}
 * sorts the {@code ArtifactInfoFactory} instances by
 * {@link foundation.polar.gratify.annotation.Order @Order}, so that ones with a
 * higher precedence come first.
 *
 * @author Arjen Poutsma
 * @see CachedIntrospectionResults
 * @see foundation.polar.gratify.core.io.support.GratifyFactoriesLoader
 */
public interface ArtifactInfoFactory {
   /**
    * Return the bean info for the given class, if supported.
    * @param beanClass the bean class
    * @return the ArtifactInfo, or {@code null} if the given class is not supported
    * @throws IntrospectionException in case of exceptions
    */
   @Nullable
   BeanInfo getArtifactInfo(Class<?> beanClass) throws IntrospectionException;
}
