package foundation.polar.gratify.annotations.interning;

import foundation.polar.gratify.annotations.DefaultFor;
import foundation.polar.gratify.annotations.QualifierForLiterals;
import foundation.polar.gratify.annotations.SubtypeOf;
import foundation.polar.gratify.annotations.utils.LiteralKind;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(UnknownInterned.class)
@QualifierForLiterals({LiteralKind.PRIMITIVE, LiteralKind.STRING}) // everything but NULL
@DefaultFor(
   typeKinds = {
      TypeKind.BOOLEAN,
      TypeKind.BYTE,
      TypeKind.CHAR,
      TypeKind.DOUBLE,
      TypeKind.FLOAT,
      TypeKind.INT,
      TypeKind.LONG,
      TypeKind.SHORT
   })
public @interface Interned {}
