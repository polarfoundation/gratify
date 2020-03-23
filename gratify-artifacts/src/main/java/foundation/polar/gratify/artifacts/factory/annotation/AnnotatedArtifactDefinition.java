package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.core.type.AnnotationMetadata;
import foundation.polar.gratify.core.type.MethodMetadata;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extended {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition}
 * interface that exposes {@link foundation.polar.gratify.core.type.AnnotationMetadata}
 * about its bean class - without requiring the class to be loaded yet.
 *
 * @author Juergen Hoeller
 *
 * @see AnnotatedGenericArtifactDefinition
 * @see foundation.polar.gratify.core.type.AnnotationMetadata
 */
public interface AnnotatedArtifactDefinition extends ArtifactDefinition {

   /**
    * Obtain the annotation metadata (as well as basic class metadata)
    * for this bean definition's bean class.
    * @return the annotation metadata object (never {@code null})
    */
   AnnotationMetadata getMetadata();

   /**
    * Obtain metadata for this bean definition's factory method, if any.
    * @return the factory method metadata, or {@code null} if none
    */
   @Nullable
   MethodMetadata getFactoryMethodMetadata();
}
