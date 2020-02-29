package foundation.polar.gratify.annotations.checker.compilermsgs;

import foundation.polar.gratify.annotations.checker.DefaultFor;
import foundation.polar.gratify.annotations.checker.SubtypeOf;
import foundation.polar.gratify.annotations.checker.TargetLocations;
import foundation.polar.gratify.annotations.utils.TypeUseLocation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(CompilerMessageKey.class)
@TargetLocations({
   TypeUseLocation.EXPLICIT_LOWER_BOUND,
   TypeUseLocation.EXPLICIT_UPPER_BOUND
})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface CompilerMessageKeyBottom {}
