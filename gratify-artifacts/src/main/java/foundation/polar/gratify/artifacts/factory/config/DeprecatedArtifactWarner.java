package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Artifact factory post processor that logs a warning for {@link Deprecated @Deprecated} beans.
 *
 * @author Arjen Poutsma
 */
public class DeprecatedArtifactWarner implements ArtifactFactoryPostProcessor {
   /**
    * Logger available to subclasses.
    */
   protected transient Log logger = LogFactory.getLog(getClass());

   /**
    * Set the name of the logger to use.
    * The name will be passed to the underlying logger implementation through Commons Logging,
    * getting interpreted as log category according to the logger's configuration.
    * <p>This can be specified to not log into the category of this warner class but rather
    * into a specific named category.
    * @see org.apache.commons.logging.LogFactory#getLog(String)
    * @see java.util.logging.Logger#getLogger(String)
    */
   public void setLoggerName(String loggerName) {
      this.logger = LogFactory.getLog(loggerName);
   }
   
   @Override
   public void postProcessArtifactFactory(ConfigurableListableArtifactFactory artifactFactory) throws ArtifactsException {
      if (isLogEnabled()) {
         String[] beanNames = artifactFactory.getArtifactDefinitionNames();
         for (String beanName : beanNames) {
            String nameToLookup = beanName;
            if (artifactFactory.isFactoryArtifact(beanName)) {
               nameToLookup = ArtifactFactory.FACTORY_BEAN_PREFIX + beanName;
            }
            Class<?> beanType = artifactFactory.getType(nameToLookup);
            if (beanType != null) {
               Class<?> userClass = ClassUtils.getUserClass(beanType);
               if (userClass.isAnnotationPresent(Deprecated.class)) {
                  ArtifactDefinition artifactDefinition = artifactFactory.getArtifactDefinition(beanName);
                  logDeprecatedArtifact(beanName, beanType, artifactDefinition);
               }
            }
         }
      }
   }

   /**
    * Logs a warning for a bean annotated with {@link Deprecated @Deprecated}.
    * @param beanName the name of the deprecated bean
    * @param beanType the user-specified type of the deprecated bean
    * @param artifactDefinition the definition of the deprecated bean
    */
   protected void logDeprecatedArtifact(String beanName, Class<?> beanType, ArtifactDefinition artifactDefinition) {
      StringBuilder builder = new StringBuilder();
      builder.append(beanType);
      builder.append(" ['");
      builder.append(beanName);
      builder.append('\'');
      String resourceDescription = artifactDefinition.getResourceDescription();
      if (StringUtils.hasLength(resourceDescription)) {
         builder.append(" in ");
         builder.append(resourceDescription);
      }
      builder.append("] has been deprecated");
      writeToLog(builder.toString());
   }

   /**
    * Actually write to the underlying log.
    * <p>The default implementations logs the message at "warn" level.
    * @param message the message to write
    */
   protected void writeToLog(String message) {
      logger.warn(message);
   }

   /**
    * Determine whether the {@link #logger} field is enabled.
    * <p>Default is {@code true} when the "warn" level is enabled.
    * Subclasses can override this to change the level under which logging occurs.
    */
   protected boolean isLogEnabled() {
      return logger.isWarnEnabled();
   }
}
