package foundation.polar.gratify.annotations.checker.signature;

import foundation.polar.gratify.annotations.checker.QualifierForLiterals;
import foundation.polar.gratify.annotations.checker.SubtypeOf;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({ClassGetName.class, FqBinaryName.class})
@QualifierForLiterals(
   stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\$[A-Za-z_0-9]+)*$")
public @interface BinaryName {
}
