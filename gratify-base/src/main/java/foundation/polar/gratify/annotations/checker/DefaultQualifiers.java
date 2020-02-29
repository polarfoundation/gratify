package foundation.polar.gratify.annotations.checker;

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
public @interface DefaultQualifiers {
   DefaultQualifier[] value() default {};
}
