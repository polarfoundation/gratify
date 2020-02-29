package foundation.polar.gratify.lang;

import foundation.polar.gratify.annotations.ConditionalPostconditionAnnotation;
import foundation.polar.gratify.annotations.InheritedAnnotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@ConditionalPostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
public @interface EnsuresNonNullIf {
   String[] expression();
   boolean result();
}
