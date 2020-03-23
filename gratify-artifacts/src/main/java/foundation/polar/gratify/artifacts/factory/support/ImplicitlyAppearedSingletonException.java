package foundation.polar.gratify.artifacts.factory.support;

/**
 * Internal exception to be propagated from {@link ConstructorResolver},
 * passed through to the initiating {@link DefaultSingletonArtifactRegistry}
 * (without wrapping in a {@code ArtifactCreationException}).
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class ImplicitlyAppearedSingletonException extends IllegalStateException {

   public ImplicitlyAppearedSingletonException() {
      super("About-to-be-created singleton instance implicitly appeared through the " +
         "creation of the factory bean that its bean definition points to");
   }
}
