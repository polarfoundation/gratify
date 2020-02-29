package foundation.polar.gratify.annotations;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface UpperBoundFor {
   TypeKind[] typeKinds() default {};
   Class<?>[] types() default {};
}
