package foundation.polar.gratify.artifacts;

/**
 * Simple factory facade for obtaining {@link PropertyAccessor} instances,
 * in particular for {@link ArtifactWrapper} instances. Conceals the actual
 * target implementation classes and their extended public signature.
 *
 * @author Juergen Hoeller
 */
public class PropertyAccessorFactory {
   private PropertyAccessorFactory() {}

   /**
    * Obtain a ArtifactWrapper for the given target object,
    * accessing properties in JavaBeans style.
    * @param target the target object to wrap
    * @return the property accessor
    * @see ArtifactWrapperImpl
    */
   public static ArtifactWrapper forArtifactPropertyAccess(Object target) {
      return new ArtifactWrapperImpl(target);
   }

   /**
    * Obtain a PropertyAccessor for the given target object,
    * accessing properties in direct field style.
    * @param target the target object to wrap
    * @return the property accessor
    * @see DirectFieldAccessor
    */
   public static ConfigurablePropertyAccessor forDirectFieldAccess(Object target) {
      return new DirectFieldAccessor(target);
   }
}
