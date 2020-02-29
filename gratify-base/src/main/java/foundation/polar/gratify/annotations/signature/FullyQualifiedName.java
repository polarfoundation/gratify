package foundation.polar.gratify.annotations.signature;

import foundation.polar.gratify.annotations.QualifierForLiterals;
import foundation.polar.gratify.annotations.SubtypeOf;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(FqBinaryName.class)
@QualifierForLiterals(
   stringPatterns = "^[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)*(\\[\\])*$")
public @interface FullyQualifiedName {}