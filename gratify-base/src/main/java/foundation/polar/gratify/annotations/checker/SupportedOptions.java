package foundation.polar.gratify.annotations.checker;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SupportedOptions {
   String[] value();
}
