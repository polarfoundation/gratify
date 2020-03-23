package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.inject.Provider;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

/**
 * A {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} implementation that
 * returns a value which is a JSR-330 {@link javax.inject.Provider} that in turn
 * returns a bean sourced from a {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}.
 *
 * <p>This is basically a JSR-330 compliant variant of Gratify's good old
 * {@link ObjectFactoryCreatingFactoryArtifact}. It can be used for traditional
 * external dependency injection configuration that targets a property or
 * constructor argument of type {@code javax.inject.Provider}, as an
 * alternative to JSR-330's {@code @Inject} annotation-driven approach.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.inject.Provider
 * @see ObjectFactoryCreatingFactoryArtifact
 */
public class ProviderCreatingFactoryArtifact extends AbstractFactoryArtifact<Provider<Object>> {
   @Nullable
   private String targetArtifactName;

   /**
    * Set the name of the target bean.
    * <p>The target does not <i>have</i> to be a non-singleton bean, but realistically
    * always will be (because if the target bean were a singleton, then said singleton
    * bean could simply be injected straight into the dependent object, thus obviating
    * the need for the extra level of indirection afforded by this factory approach).
    */
   public void setTargetArtifactName(String targetArtifactName) {
      this.targetArtifactName = targetArtifactName;
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      AssertUtils.hasText(this.targetArtifactName, "Property 'targetArtifactName' is required");
      super.afterPropertiesSet();
   }

   @Override
   public Class<?> getObjectType() {
      return Provider.class;
   }

   @Override
   protected Provider<Object> createInstance() {
      ArtifactFactory beanFactory = getArtifactFactory();
      AssertUtils.state(beanFactory != null, "No ArtifactFactory available");
      AssertUtils.state(this.targetArtifactName != null, "No target bean name specified");
      return new TargetArtifactProvider(beanFactory, this.targetArtifactName);
   }

   /**
    * Independent inner class - for serialization purposes.
    */
   @SuppressWarnings("serial")
   private static class TargetArtifactProvider implements Provider<Object>, Serializable {

      private final ArtifactFactory beanFactory;

      private final String targetArtifactName;

      public TargetArtifactProvider(ArtifactFactory beanFactory, String targetArtifactName) {
         this.beanFactory = beanFactory;
         this.targetArtifactName = targetArtifactName;
      }

      @Override
      public Object get() throws ArtifactsException {
         return this.beanFactory.getArtifact(this.targetArtifactName);
      }
   }
}
