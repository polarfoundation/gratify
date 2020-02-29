package foundation.polar.gratify.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface ConditionalPostconditionAnnotation {
   Class<? extends Annotation> qualifier();
}
