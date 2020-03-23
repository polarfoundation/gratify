package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactFactoryPostProcessor;

/**
 * Extension to the standard {@link ArtifactFactoryPostProcessor} SPI, allowing for
 * the registration of further bean definitions <i>before</i> regular
 * ArtifactFactoryPostProcessor detection kicks in. In particular,
 * ArtifactDefinitionRegistryPostProcessor may register further bean definitions
 * which in turn define ArtifactFactoryPostProcessor instances.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.context.annotation.ConfigurationClassPostProcessor
 */
public interface ArtifactDefinitionRegistryPostProcessor extends ArtifactFactoryPostProcessor {
   /**
    * Modify the application context's internal bean definition registry after its
    * standard initialization. All regular bean definitions will have been loaded,
    * but no beans will have been instantiated yet. This allows for adding further
    * bean definitions before the next post-processing phase kicks in.
    * @param registry the bean definition registry used by the application context
    * @throwsfoundation.polar.gratify.artifacts.ArtifactsException in case of errors
    */
   void postProcessArtifactDefinitionRegistry(ArtifactDefinitionRegistry registry) throws ArtifactsException;
}
