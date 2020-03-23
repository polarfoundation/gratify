package foundation.polar.gratify.artifacts.factory.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as being eligible for Gratify-driven configuration.
 *
 * <p>Typically used with the AspectJ {@code AnnotationArtifactConfigurerAspect}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Configurable {

   /**
    * The name of the bean definition that serves as the configuration template.
    */
   String value() default "";

   /**
    * Are dependencies to be injected via autowiring?
    */
   Autowire autowire() default Autowire.NO;

   /**
    * Is dependency checking to be performed for configured objects?
    */
   boolean dependencyCheck() default false;

   /**
    * Are dependencies to be injected prior to the construction of an object?
    */
   boolean preConstruction() default false;
}
