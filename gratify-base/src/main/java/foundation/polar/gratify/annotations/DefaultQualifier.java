package foundation.polar.gratify.annotations;

import foundation.polar.gratify.annotations.utils.TypeUseLocation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({
   ElementType.PACKAGE,
   ElementType.TYPE,
   ElementType.CONSTRUCTOR,
   ElementType.METHOD,
   ElementType.FIELD,
   ElementType.LOCAL_VARIABLE,
   ElementType.PARAMETER
})
@Repeatable(DefaultQualifiers.class)
public @interface DefaultQualifier {
   Class<? extends Annotation> value();
   TypeUseLocation[] locations() default {TypeUseLocation.ALL};
}
