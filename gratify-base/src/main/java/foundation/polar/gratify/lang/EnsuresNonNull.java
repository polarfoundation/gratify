package foundation.polar.gratify.lang;

import foundation.polar.gratify.annotations.checker.InheritedAnnotation;
import foundation.polar.gratify.annotations.checker.PostconditionAnnotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = NonNull.class)
@InheritedAnnotation
public @interface EnsuresNonNull {
   String[] value();
}
