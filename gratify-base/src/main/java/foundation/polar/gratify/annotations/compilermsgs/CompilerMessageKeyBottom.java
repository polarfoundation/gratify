package foundation.polar.gratify.annotations.compilermsgs;

import foundation.polar.gratify.annotations.DefaultFor;
import foundation.polar.gratify.annotations.SubtypeOf;
import foundation.polar.gratify.annotations.TargetLocations;
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
