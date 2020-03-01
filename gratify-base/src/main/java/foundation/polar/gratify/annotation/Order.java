package foundation.polar.gratify.annotation;

import foundation.polar.gratify.core.Ordered;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Order {
   int value() default Ordered.LOWEST_PRECEDENCE;
}
