package foundation.polar.gratify.annotations.checker;

import foundation.polar.gratify.annotations.utils.TypeUseLocation;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultFor {
   TypeUseLocation[] value() default {};
   TypeKind[] typeKinds() default {};
   Class<?>[] types() default {};
}
