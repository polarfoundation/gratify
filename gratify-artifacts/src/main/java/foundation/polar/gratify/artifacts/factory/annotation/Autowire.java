package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory;

/**
 * Enumeration determining autowiring status: that is, whether a bean should
 * have its dependencies automatically injected by the Gratify container using
 * setter injection. This is a core concept in Gratify DI.
 *
 * <p>Available for use in annotation-based configurations, such as for the
 * AspectJ AnnotationArtifactConfigurer aspect.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.annotation.Configurable
 * @see foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory
 */
public enum Autowire {
   /**
    * Constant that indicates no autowiring at all.
    */
   NO(AutowireCapableArtifactFactory.AUTOWIRE_NO),

   /**
    * Constant that indicates autowiring bean properties by name.
    */
   BY_NAME(AutowireCapableArtifactFactory.AUTOWIRE_BY_NAME),

   /**
    * Constant that indicates autowiring bean properties by type.
    */
   BY_TYPE(AutowireCapableArtifactFactory.AUTOWIRE_BY_TYPE);

   private final int value;

   Autowire(int value) {
      this.value = value;
   }

   public int value() {
      return this.value;
   }

   /**
    * Return whether this represents an actual autowiring value.
    * @return whether actual autowiring was specified
    * (either BY_NAME or BY_TYPE)
    */
   public boolean isAutowire() {
      return (this == BY_NAME || this == BY_TYPE);
   }
}
