package foundation.polar.gratify.lang;

import foundation.polar.gratify.annotations.checker.MonotonicQualifier;
import foundation.polar.gratify.annotations.checker.SubtypeOf;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@SubtypeOf(Nullable.class)
@MonotonicQualifier(NonNull.class)
public @interface MonotonicNonNull {
}
