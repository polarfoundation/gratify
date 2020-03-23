package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactInstantiationException;
import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableArtifactFactory;
import foundation.polar.gratify.core.ClassLoaderAwareGeneratorStrategy;
import foundation.polar.gratify.core.GratifyNamingPolicy;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import net.sf.cglib.proxy.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Default object instantiation strategy for use in ArtifactFactories.
 *
 * <p>Uses CGLIB to generate subclasses dynamically if methods need to be
 * overridden by the container to implement <em>Method Injection</em>.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {
   /**
    * Index in the CGLIB callback array for passthrough behavior,
    * in which case the subclass won't override the original class.
    */
   private static final int PASSTHROUGH = 0;

   /**
    * Index in the CGLIB callback array for a method that should
    * be overridden to provide <em>method lookup</em>.
    */
   private static final int LOOKUP_OVERRIDE = 1;

   /**
    * Index in the CGLIB callback array for a method that should
    * be overridden using generic <em>method replacer</em> functionality.
    */
   private static final int METHOD_REPLACER = 2;


   @Override
   protected Object instantiateWithMethodInjection(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner) {
      return instantiateWithMethodInjection(bd, beanName, owner, null);
   }

   @Override
   protected Object instantiateWithMethodInjection(RootArtifactDefinition bd, @Nullable String beanName, ArtifactFactory owner,
                                                   @Nullable Constructor<?> ctor, Object... args) {

      // Must generate CGLIB subclass...
      return new CglibSubclassCreator(bd, owner).instantiate(ctor, args);
   }


   /**
    * An inner class created for historical reasons to avoid external CGLIB dependency
    * in Gratify versions earlier than 3.2.
    */
   private static class CglibSubclassCreator {

      private static final Class<?>[] CALLBACK_TYPES = new Class<?>[]
         {NoOp.class, LookupOverrideMethodInterceptor.class, ReplaceOverrideMethodInterceptor.class};

      private final RootArtifactDefinition beanDefinition;

      private final ArtifactFactory owner;

      CglibSubclassCreator(RootArtifactDefinition beanDefinition, ArtifactFactory owner) {
         this.beanDefinition = beanDefinition;
         this.owner = owner;
      }

      /**
       * Create a new instance of a dynamically generated subclass implementing the
       * required lookups.
       * @param ctor constructor to use. If this is {@code null}, use the
       * no-arg constructor (no parameterization, or Setter Injection)
       * @param args arguments to use for the constructor.
       * Ignored if the {@code ctor} parameter is {@code null}.
       * @return new instance of the dynamically generated subclass
       */
      public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
         Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
         Object instance;
         if (ctor == null) {
            instance = ArtifactUtils.instantiateClass(subclass);
         }
         else {
            try {
               Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
               instance = enhancedSubclassConstructor.newInstance(args);
            }
            catch (Exception ex) {
               throw new ArtifactInstantiationException(this.beanDefinition.getArtifactClass(),
                  "Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
            }
         }
         // SPR-10785: set callbacks directly on the instance instead of in the
         // enhanced class (via the Enhancer) in order to avoid memory leaks.
         Factory factory = (Factory) instance;
         factory.setCallbacks(new Callback[] {NoOp.INSTANCE,
            new LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
            new ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
         return instance;
      }

      /**
       * Create an enhanced subclass of the bean class for the provided bean
       * definition, using CGLIB.
       */
      private Class<?> createEnhancedSubclass(RootArtifactDefinition beanDefinition) {
         Enhancer enhancer = new Enhancer();
         enhancer.setSuperclass(beanDefinition.getArtifactClass());
         enhancer.setNamingPolicy(GratifyNamingPolicy.INSTANCE);
         if (this.owner instanceof ConfigurableArtifactFactory) {
            ClassLoader cl = ((ConfigurableArtifactFactory) this.owner).getArtifactClassLoader();
            enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(cl));
         }
         enhancer.setCallbackFilter(new MethodOverrideCallbackFilter(beanDefinition));
         enhancer.setCallbackTypes(CALLBACK_TYPES);
         return enhancer.createClass();
      }
   }

   /**
    * Class providing hashCode and equals methods required by CGLIB to
    * ensure that CGLIB doesn't generate a distinct class per bean.
    * Identity is based on class and bean definition.
    */
   private static class CglibIdentitySupport {

      private final RootArtifactDefinition beanDefinition;

      public CglibIdentitySupport(RootArtifactDefinition beanDefinition) {
         this.beanDefinition = beanDefinition;
      }

      public RootArtifactDefinition getArtifactDefinition() {
         return this.beanDefinition;
      }

      @Override
      public boolean equals(@Nullable Object other) {
         return (other != null && getClass() == other.getClass() &&
            this.beanDefinition.equals(((CglibIdentitySupport) other).beanDefinition));
      }

      @Override
      public int hashCode() {
         return this.beanDefinition.hashCode();
      }
   }


   /**
    * CGLIB callback for filtering method interception behavior.
    */
   private static class MethodOverrideCallbackFilter extends CglibIdentitySupport implements CallbackFilter {

      private static final Log logger = LogFactory.getLog(MethodOverrideCallbackFilter.class);

      public MethodOverrideCallbackFilter(RootArtifactDefinition beanDefinition) {
         super(beanDefinition);
      }

      @Override
      public int accept(Method method) {
         MethodOverride methodOverride = getArtifactDefinition().getMethodOverrides().getOverride(method);
         if (logger.isTraceEnabled()) {
            logger.trace("MethodOverride for " + method + ": " + methodOverride);
         }
         if (methodOverride == null) {
            return PASSTHROUGH;
         }
         else if (methodOverride instanceof LookupOverride) {
            return LOOKUP_OVERRIDE;
         }
         else if (methodOverride instanceof ReplaceOverride) {
            return METHOD_REPLACER;
         }
         throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
            methodOverride.getClass().getName());
      }
   }

   /**
    * CGLIB MethodInterceptor to override methods, replacing them with an
    * implementation that returns a bean looked up in the container.
    */
   private static class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

      private final ArtifactFactory owner;

      public LookupOverrideMethodInterceptor(RootArtifactDefinition beanDefinition, ArtifactFactory owner) {
         super(beanDefinition);
         this.owner = owner;
      }

      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
         // Cast is safe, as CallbackFilter filters are used selectively.
         LookupOverride lo = (LookupOverride) getArtifactDefinition().getMethodOverrides().getOverride(method);
         AssertUtils.state(lo != null, "LookupOverride not found");
         Object[] argsToUse = (args.length > 0 ? args : null);  // if no-arg, don't insist on args at all
         if (StringUtils.hasText(lo.getArtifactName())) {
            return (argsToUse != null ? this.owner.getArtifact(lo.getArtifactName(), argsToUse) :
               this.owner.getArtifact(lo.getArtifactName()));
         }
         else {
            return (argsToUse != null ? this.owner.getArtifact(method.getReturnType(), argsToUse) :
               this.owner.getArtifact(method.getReturnType()));
         }
      }
   }


   /**
    * CGLIB MethodInterceptor to override methods, replacing them with a call
    * to a generic MethodReplacer.
    */
   private static class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

      private final ArtifactFactory owner;

      public ReplaceOverrideMethodInterceptor(RootArtifactDefinition beanDefinition, ArtifactFactory owner) {
         super(beanDefinition);
         this.owner = owner;
      }

      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
         ReplaceOverride ro = (ReplaceOverride) getArtifactDefinition().getMethodOverrides().getOverride(method);
         AssertUtils.state(ro != null, "ReplaceOverride not found");
         // TODO could cache if a singleton for minor performance optimization
         MethodReplacer mr = this.owner.getArtifact(ro.getMethodReplacerArtifactName(), MethodReplacer.class);
         return mr.reimplement(obj, method, args);
      }
   }
}
