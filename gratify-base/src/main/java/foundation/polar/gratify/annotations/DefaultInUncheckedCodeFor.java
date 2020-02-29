package foundation.polar.gratify.annotations;

import foundation.polar.gratify.annotations.utils.TypeUseLocation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DefaultInUncheckedCodeFor {
   TypeUseLocation[] value();
}
