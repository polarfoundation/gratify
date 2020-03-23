package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.artifacts.factory.support.GenericArtifactDefinition;
import foundation.polar.gratify.core.type.AnnotationMetadata;
import foundation.polar.gratify.core.type.MethodMetadata;
import foundation.polar.gratify.core.type.StandardAnnotationMetadata;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extension of the {@link foundation.polar.gratify.artifacts.factory.support.GenericArtifactDefinition}
 * class, adding support for annotation metadata exposed through the
 * {@link AnnotatedArtifactDefinition} interface.
 *
 * <p>This GenericArtifactDefinition variant is mainly useful for testing code that expects
 * to operate on an AnnotatedArtifactDefinition, for example strategy implementations
 * in Gratify's component scanning support (where the default definition class is
 * {@link foundation.polar.gratify.context.annotation.ScannedGenericArtifactDefinition},
 * which also implements the AnnotatedArtifactDefinition interface).
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 *
 * @see AnnotatedArtifactDefinition#getMetadata()
 * @see foundation.polar.gratify.core.type.StandardAnnotationMetadata
 */
@SuppressWarnings("serial")
public class AnnotatedGenericArtifactDefinition
   extends GenericArtifactDefinition implements AnnotatedArtifactDefinition {
   private final AnnotationMetadata metadata;

   @Nullable
   private MethodMetadata factoryMethodMetadata;

   /**
    * Create a new AnnotatedGenericArtifactDefinition for the given bean class.
    * @param beanClass the loaded bean class
    */
   public AnnotatedGenericArtifactDefinition(Class<?> beanClass) {
      setArtifactClass(beanClass);
      this.metadata = AnnotationMetadata.introspect(beanClass);
   }

   /**
    * Create a new AnnotatedGenericArtifactDefinition for the given annotation metadata,
    * allowing for ASM-based processing and avoidance of early loading of the bean class.
    * Note that this constructor is functionally equivalent to
    * {@link foundation.polar.gratify.context.annotation.ScannedGenericArtifactDefinition
    * ScannedGenericArtifactDefinition}, however the semantics of the latter indicate that a
    * bean was discovered specifically via component-scanning as opposed to other means.
    * @param metadata the annotation metadata for the bean class in question
    */
   public AnnotatedGenericArtifactDefinition(AnnotationMetadata metadata) {
      AssertUtils.notNull(metadata, "AnnotationMetadata must not be null");
      if (metadata instanceof StandardAnnotationMetadata) {
         setArtifactClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
      }
      else {
         setArtifactClassName(metadata.getClassName());
      }
      this.metadata = metadata;
   }

   /**
    * Create a new AnnotatedGenericArtifactDefinition for the given annotation metadata,
    * based on an annotated class and a factory method on that class.
    * @param metadata the annotation metadata for the bean class in question
    * @param factoryMethodMetadata metadata for the selected factory method
    */
   public AnnotatedGenericArtifactDefinition(AnnotationMetadata metadata, MethodMetadata factoryMethodMetadata) {
      this(metadata);
      AssertUtils.notNull(factoryMethodMetadata, "MethodMetadata must not be null");
      setFactoryMethodName(factoryMethodMetadata.getMethodName());
      this.factoryMethodMetadata = factoryMethodMetadata;
   }

   @Override
   public final AnnotationMetadata getMetadata() {
      return this.metadata;
   }

   @Override
   @Nullable
   public final MethodMetadata getFactoryMethodMetadata() {
      return this.factoryMethodMetadata;
   }
}
