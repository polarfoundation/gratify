package foundation.polar.gratify.annotations.checker.compilermsgs;

import foundation.polar.gratify.annotations.checker.SubtypeOf;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownCompilerMessageKey.class)
public @interface CompilerMessageKey {}
