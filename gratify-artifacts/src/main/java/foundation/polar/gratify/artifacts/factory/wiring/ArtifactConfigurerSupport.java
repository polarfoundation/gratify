package foundation.polar.gratify.artifacts.factory.wiring;


import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Convenient base class for bean configurers that can perform Dependency Injection
 * on objects (however they may be created). Typically subclassed by AspectJ aspects.
 *
 * <p>Subclasses may also need a custom metadata resolution strategy, in the
 * {@link ArtifactWiringInfoResolver} interface. The default implementation looks for
 * a bean with the same name as the fully-qualified class name. (This is the default
 * name of the bean in a Gratify XML file if the '{@code id}' attribute is not used.)

 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Adrian Colyer
 *
 * @see #setArtifactWiringInfoResolver
 * @see ClassNameArtifactWiringInfoResolver
 */
public class ArtifactConfigurerSupport
   implements ArtifactFactoryAware, InitializingArtifact, DisposableArtifact {
   /** Logger available to subclasses. */
   protected final Log logger = LogFactory.getLog(getClass());

   @Nullable
   private volatile ArtifactWiringInfoResolver beanWiringInfoResolver;

   @Nullable
   private volatile ConfigurableListableArtifactFactory artifactFactory;

   /**
    * Set the {@link ArtifactWiringInfoResolver} to use.
    * <p>The default behavior is to look for a bean with the same name as the class.
    * As an alternative, consider using annotation-driven bean wiring.
    * @see ClassNameArtifactWiringInfoResolver
    * @see foundation.polar.gratify.artifacts.factory.annotation.AnnotationArtifactWiringInfoResolver
    */
   public void setArtifactWiringInfoResolver(ArtifactWiringInfoResolver beanWiringInfoResolver) {
      AssertUtils.notNull(beanWiringInfoResolver, "ArtifactWiringInfoResolver must not be null");
      this.beanWiringInfoResolver = beanWiringInfoResolver;
   }

   /**
    * Set the {@link ArtifactFactory} in which this aspect must configure beans.
    */
   @Override
   public void setArtifactFactory(ArtifactFactory artifactFactory) {
      if (!(artifactFactory instanceof ConfigurableListableArtifactFactory)) {
         throw new IllegalArgumentException(
            "Artifact configurer aspect needs to run in a ConfigurableListableArtifactFactory: " + artifactFactory);
      }
      this.artifactFactory = (ConfigurableListableArtifactFactory) artifactFactory;
      if (this.beanWiringInfoResolver == null) {
         this.beanWiringInfoResolver = createDefaultArtifactWiringInfoResolver();
      }
   }

   /**
    * Create the default ArtifactWiringInfoResolver to be used if none was
    * specified explicitly.
    * <p>The default implementation builds a {@link ClassNameArtifactWiringInfoResolver}.
    * @return the default ArtifactWiringInfoResolver (never {@code null})
    */
   @Nullable
   protected ArtifactWiringInfoResolver createDefaultArtifactWiringInfoResolver() {
      return new ClassNameArtifactWiringInfoResolver();
   }

   /**
    * Check that a {@link ArtifactFactory} has been set.
    */
   @Override
   public void afterPropertiesSet() {
      AssertUtils.notNull(this.artifactFactory, "ArtifactFactory must be set");
   }

   /**
    * Release references to the {@link ArtifactFactory} and
    * {@link ArtifactWiringInfoResolver} when the container is destroyed.
    */
   @Override
   public void destroy() {
      this.artifactFactory = null;
      this.beanWiringInfoResolver = null;
   }

   /**
    * Configure the bean instance.
    * <p>Subclasses can override this to provide custom configuration logic.
    * Typically called by an aspect, for all bean instances matched by a pointcut.
    * @param beanInstance the bean instance to configure (must <b>not</b> be {@code null})
    */
   public void configureArtifact(Object beanInstance) {
      if (this.artifactFactory == null) {
         if (logger.isDebugEnabled()) {
            logger.debug("ArtifactFactory has not been set on " + ClassUtils.getShortName(getClass()) + ": " +
               "Make sure this configurer runs in a Gratify container. Unable to configure bean of type [" +
               ClassUtils.getDescriptiveType(beanInstance) + "]. Proceeding without injection.");
         }
         return;
      }

      ArtifactWiringInfoResolver bwiResolver = this.beanWiringInfoResolver;
      AssertUtils.state(bwiResolver != null, "No ArtifactWiringInfoResolver available");
      ArtifactWiringInfo bwi = bwiResolver.resolveWiringInfo(beanInstance);
      if (bwi == null) {
         // Skip the bean if no wiring info given.
         return;
      }

      ConfigurableListableArtifactFactory artifactFactory = this.artifactFactory;
      AssertUtils.state(artifactFactory != null, "No ArtifactFactory available");
      try {
         String beanName = bwi.getArtifactName();
         if (bwi.indicatesAutowiring() || (bwi.isDefaultArtifactName() && beanName != null &&
            !artifactFactory.containsArtifact(beanName))) {
            // Perform autowiring (also applying standard factory / post-processor callbacks).
            artifactFactory.autowireArtifactProperties(beanInstance, bwi.getAutowireMode(), bwi.getDependencyCheck());
            artifactFactory.initializeArtifact(beanInstance, (beanName != null ? beanName : ""));
         }
         else {
            // Perform explicit wiring based on the specified bean definition.
            artifactFactory.configureArtifact(beanInstance, (beanName != null ? beanName : ""));
         }
      }
      catch (ArtifactCreationException ex) {
         Throwable rootCause = ex.getMostSpecificCause();
         if (rootCause instanceof ArtifactCurrentlyInCreationException) {
            ArtifactCreationException bce = (ArtifactCreationException) rootCause;
            String bceArtifactName = bce.getArtifactName();
            if (bceArtifactName != null && artifactFactory.isCurrentlyInCreation(bceArtifactName)) {
               if (logger.isDebugEnabled()) {
                  logger.debug("Failed to create target bean '" + bce.getArtifactName() +
                     "' while configuring object of type [" + beanInstance.getClass().getName() +
                     "] - probably due to a circular reference. This is a common startup situation " +
                     "and usually not fatal. Proceeding without injection. Original exception: " + ex);
               }
               return;
            }
         }
         throw ex;
      }
   }
}
