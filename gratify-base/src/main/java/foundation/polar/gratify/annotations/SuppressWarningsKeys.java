package foundation.polar.gratify.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SuppressWarningsKeys {
   String[] value();
}