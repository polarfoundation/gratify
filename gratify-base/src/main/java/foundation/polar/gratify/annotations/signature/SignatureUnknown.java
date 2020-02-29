package foundation.polar.gratify.annotations.signature;

import foundation.polar.gratify.annotations.DefaultQualifierInHierarchy;
import foundation.polar.gratify.annotations.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // empty target prevents programmers from writing this in a program
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface SignatureUnknown { }
