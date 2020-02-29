package foundation.polar.gratify.annotations.interning;

import foundation.polar.gratify.annotations.DefaultQualifierInHierarchy;
import foundation.polar.gratify.annotations.InvisibleQualifier;
import foundation.polar.gratify.annotations.SubtypeOf;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface UnknownInterned {}
