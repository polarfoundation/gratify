package foundation.polar.gratify.annotations.checker;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface PostconditionAnnotation {
   Class<? extends Annotation> qualifier();
}
