package foundation.polar.gratify.annotations.checker;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Pure {
   public static enum Kind {
      /** The method has no visible side effects. */
      SIDE_EFFECT_FREE,
      /** The method returns exactly the same value when called in the same environment. */
      DETERMINISTIC
   }
}
