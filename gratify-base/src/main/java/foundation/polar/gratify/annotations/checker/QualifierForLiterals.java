package foundation.polar.gratify.annotations.checker;

import foundation.polar.gratify.annotations.utils.LiteralKind;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface QualifierForLiterals {
   LiteralKind[] value() default {};
   String[] stringPatterns() default {};
}
