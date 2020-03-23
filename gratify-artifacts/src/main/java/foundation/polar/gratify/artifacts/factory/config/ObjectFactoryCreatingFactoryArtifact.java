package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.ObjectFactory;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

/**
 * A {@link foundation.polar.gratify.artifacts.factory.FactoryArtifact} implementation that
 * returns a value which is an {@link foundation.polar.gratify.artifacts.factory.ObjectFactory}
 * that in turn returns a bean sourced from a {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}.
 *
 * <p>As such, this may be used to avoid having a client object directly calling
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact(String)} to get
 * a (typically prototype) bean from a
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}, which would be a
 * violation of the inversion of control principle. Instead, with the use
 * of this class, the client object can be fed an
 * {@link foundation.polar.gratify.artifacts.factory.ObjectFactory} instance as a
 * property which directly returns only the one target bean (again, which is
 * typically a prototype bean).
 *
 * <p>A sample config in an XML-based
 * {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory} might look as follows:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="foundation.polar.gratify.beans.factory.config.ObjectFactoryCreatingFactoryArtifact"&gt;
 *     &lt;property name="targetArtifactName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientArtifact" class="a.b.c.MyClientArtifact"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>The attendant {@code MyClientArtifact} class implementation might look
 * something like this:
 *
 * <pre class="code">package a.b.c;
 *
 * importfoundation.polar.gratify.artifacts.factory.ObjectFactory;
 *
 * public class MyClientArtifact {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // get a 'fresh', brand new MyService instance
 *     MyService service = this.myServiceFactory.getObject();
 *     // use the service object to effect the business logic...
 *   }
 * }</pre>
 *
 * <p>An alternate approach to this application of an object creational pattern
 * would be to use the {@link ServiceLocatorFactoryArtifact}
 * to source (prototype) beans. The {@link ServiceLocatorFactoryArtifact} approach
 * has the advantage of the fact that one doesn't have to depend on any
 * Gratify-specific interface such as {@link foundation.polar.gratify.artifacts.factory.ObjectFactory},
 * but has the disadvantage of requiring runtime class generation. Please do
 * consult the {@link ServiceLocatorFactoryArtifact ServiceLocatorFactoryArtifact JavaDoc}
 * for a fuller discussion of this issue.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.ObjectFactory
 * @see ServiceLocatorFactoryArtifact
 */
public class ObjectFactoryCreatingFactoryArtifact extends AbstractFactoryArtifact<ObjectFactory<Object>> {
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
      return ObjectFactory.class;
   }

   @Override
   protected ObjectFactory<Object> createInstance() {
      ArtifactFactory beanFactory = getArtifactFactory();
      AssertUtils.state(beanFactory != null, "No ArtifactFactory available");
      AssertUtils.state(this.targetArtifactName != null, "No target bean name specified");
      return new TargetArtifactObjectFactory(beanFactory, this.targetArtifactName);
   }

   /**
    * Independent inner class - for serialization purposes.
    */
   @SuppressWarnings("serial")
   private static class TargetArtifactObjectFactory implements ObjectFactory<Object>, Serializable {

      private final ArtifactFactory beanFactory;

      private final String targetArtifactName;

      public TargetArtifactObjectFactory(ArtifactFactory beanFactory, String targetArtifactName) {
         this.beanFactory = beanFactory;
         this.targetArtifactName = targetArtifactName;
      }

      @Override
      public Object getObject() throws ArtifactsException {
         return this.beanFactory.getArtifact(this.targetArtifactName);
      }
   }
}
