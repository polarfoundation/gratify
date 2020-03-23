package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactWrapper;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.PropertyAccessorFactory;
import foundation.polar.gratify.artifacts.factory.*;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link FactoryArtifact} that evaluates a property path on a given target object.
 *
 * <p>The target object can be specified directly or via a bean name.
 *
 * <p>Usage examples:
 *
 * <pre class="code">&lt;!-- target bean to be referenced by name --&gt;
 * &lt;bean id="tb" class="foundation.polar.gratify.beans.TestArtifact" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="foundation.polar.gratify.beans.TestArtifact"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 12, which is the value of property 'age' of the inner bean --&gt;
 * &lt;bean id="propertyPath1" class="foundation.polar.gratify.beans.factory.config.PropertyPathFactoryArtifact"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="foundation.polar.gratify.beans.TestArtifact"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 11, which is the value of property 'spouse.age' of bean 'tb' --&gt;
 * &lt;bean id="propertyPath2" class="foundation.polar.gratify.beans.factory.config.PropertyPathFactoryArtifact"&gt;
 *   &lt;property name="targetArtifactName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;bean id="tb.age" class="foundation.polar.gratify.beans.factory.config.PropertyPathFactoryArtifact"/&gt;</pre>
 *
 * <p>If you are using Gratify 2.0 and XML Schema support in your configuration file(s),
 * you can also use the following style of configuration for property path access.
 * (See also the appendix entitled 'XML Schema-based configuration' in the Gratify
 * reference manual for more examples.)
 *
 * <pre class="code"> &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;util:property-path id="name" path="testArtifact.age"/&gt;</pre>
 *
 * Thanks to Matthias Ernst for the suggestion and initial prototype!
 *
 * @author Juergen Hoeller
 * @see #setTargetObject
 * @see #setTargetArtifactName
 * @see #setPropertyPath
 */
public class PropertyPathFactoryArtifact implements FactoryArtifact<Object>, ArtifactNameAware, ArtifactFactoryAware {
   private static final Log logger = LogFactory.getLog(PropertyPathFactoryArtifact.class);

   @Nullable
   private ArtifactWrapper targetArtifactWrapper;

   @Nullable
   private String targetArtifactName;

   @Nullable
   private String propertyPath;

   @Nullable
   private Class<?> resultType;

   @Nullable
   private String beanName;

   @Nullable
   private ArtifactFactory artifactFactory;

   /**
    * Specify a target object to apply the property path to.
    * Alternatively, specify a target bean name.
    * @param targetObject a target object, for example a bean reference
    * or an inner bean
    * @see #setTargetArtifactName
    */
   public void setTargetObject(Object targetObject) {
      this.targetArtifactWrapper = PropertyAccessorFactory.forArtifactPropertyAccess(targetObject);
   }

   /**
    * Specify the name of a target bean to apply the property path to.
    * Alternatively, specify a target object directly.
    * @param targetArtifactName the bean name to be looked up in the
    * containing bean factory (e.g. "testArtifact")
    * @see #setTargetObject
    */
   public void setTargetArtifactName(String targetArtifactName) {
      this.targetArtifactName = StringUtils.trimAllWhitespace(targetArtifactName);
   }

   /**
    * Specify the property path to apply to the target.
    * @param propertyPath the property path, potentially nested
    * (e.g. "age" or "spouse.age")
    */
   public void setPropertyPath(String propertyPath) {
      this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
   }

   /**
    * Specify the type of the result from evaluating the property path.
    * <p>Note: This is not necessary for directly specified target objects
    * or singleton target beans, where the type can be determined through
    * introspection. Just specify this in case of a prototype target,
    * provided that you need matching by type (for example, for autowiring).
    * @param resultType the result type, for example "java.lang.Integer"
    */
   public void setResultType(Class<?> resultType) {
      this.resultType = resultType;
   }

   /**
    * The bean name of this PropertyPathFactoryArtifact will be interpreted
    * as "beanName.property" pattern, if neither "targetObject" nor
    * "targetArtifactName" nor "propertyPath" have been specified.
    * This allows for concise bean definitions with just an id/name.
    */
   @Override
   public void setArtifactName(String beanName) {
      this.beanName = StringUtils.trimAllWhitespace(ArtifactFactoryUtils.originalArtifactName(beanName));
   }

   @Override
   public void setArtifactFactory(ArtifactFactory artifactFactory) {
      this.artifactFactory = artifactFactory;

      if (this.targetArtifactWrapper != null && this.targetArtifactName != null) {
         throw new IllegalArgumentException("Specify either 'targetObject' or 'targetArtifactName', not both");
      }

      if (this.targetArtifactWrapper == null && this.targetArtifactName == null) {
         if (this.propertyPath != null) {
            throw new IllegalArgumentException(
               "Specify 'targetObject' or 'targetArtifactName' in combination with 'propertyPath'");
         }

         // No other properties specified: check bean name.
         int dotIndex = (this.beanName != null ? this.beanName.indexOf('.') : -1);
         if (dotIndex == -1) {
            throw new IllegalArgumentException(
               "Neither 'targetObject' nor 'targetArtifactName' specified, and PropertyPathFactoryArtifact " +
                  "bean name '" + this.beanName + "' does not follow 'beanName.property' syntax");
         }
         this.targetArtifactName = this.beanName.substring(0, dotIndex);
         this.propertyPath = this.beanName.substring(dotIndex + 1);
      }

      else if (this.propertyPath == null) {
         // either targetObject or targetArtifactName specified
         throw new IllegalArgumentException("'propertyPath' is required");
      }

      if (this.targetArtifactWrapper == null && this.artifactFactory.isSingleton(this.targetArtifactName)) {
         // Eagerly fetch singleton target bean, and determine result type.
         Object bean = this.artifactFactory.getArtifact(this.targetArtifactName);
         this.targetArtifactWrapper = PropertyAccessorFactory.forArtifactPropertyAccess(bean);
         this.resultType = this.targetArtifactWrapper.getPropertyType(this.propertyPath);
      }
   }


   @Override
   @Nullable
   public Object getObject() throws ArtifactsException {
      ArtifactWrapper target = this.targetArtifactWrapper;
      if (target != null) {
         if (logger.isWarnEnabled() && this.targetArtifactName != null &&
            this.artifactFactory instanceof ConfigurableArtifactFactory &&
            ((ConfigurableArtifactFactory) this.artifactFactory).isCurrentlyInCreation(this.targetArtifactName)) {
            logger.warn("Target bean '" + this.targetArtifactName + "' is still in creation due to a circular " +
               "reference - obtained value for property '" + this.propertyPath + "' may be outdated!");
         }
      }
      else {
         // Fetch prototype target bean...
         AssertUtils.state(this.artifactFactory != null, "No ArtifactFactory available");
         AssertUtils.state(this.targetArtifactName != null, "No target bean name specified");
         Object bean = this.artifactFactory.getArtifact(this.targetArtifactName);
         target = PropertyAccessorFactory.forArtifactPropertyAccess(bean);
      }
      AssertUtils.state(this.propertyPath != null, "No property path specified");
      return target.getPropertyValue(this.propertyPath);
   }

   @Override
   public Class<?> getObjectType() {
      return this.resultType;
   }

   /**
    * While this FactoryArtifact will often be used for singleton targets,
    * the invoked getters for the property path might return a new object
    * for each call, so we have to assume that we're not returning the
    * same object for each {@link #getObject()} call.
    */
   @Override
   public boolean isSingleton() {
      return false;
   }
}
