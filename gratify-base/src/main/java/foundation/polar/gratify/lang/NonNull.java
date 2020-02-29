package foundation.polar.gratify.lang;

import foundation.polar.gratify.annotations.*;
import foundation.polar.gratify.annotations.utils.LiteralKind;
import foundation.polar.gratify.annotations.utils.TypeUseLocation;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(MonotonicNonNull.class)
@QualifierForLiterals(LiteralKind.STRING)
@DefaultQualifierInHierarchy
@DefaultFor(TypeUseLocation.EXCEPTION_PARAMETER)
@UpperBoundFor(
   typeKinds = {
      TypeKind.PACKAGE,
      TypeKind.INT,
      TypeKind.BOOLEAN,
      TypeKind.CHAR,
      TypeKind.DOUBLE,
      TypeKind.FLOAT,
      TypeKind.LONG,
      TypeKind.SHORT,
      TypeKind.BYTE
   })
@DefaultInUncheckedCodeFor({TypeUseLocation.PARAMETER, TypeUseLocation.LOWER_BOUND})
public @interface NonNull {}
