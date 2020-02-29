package foundation.polar.gratify.utils.compiler;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import foundation.polar.gratify.annotations.signature.DotSeparatedIdentifiers;
import foundation.polar.gratify.lang.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class TypesUtils {
   private TypesUtils() {
      throw new AssertionError("Class TypesUtils cannot be instantiated.");
   }

   public static @DotSeparatedIdentifiers
   Name getQualifiedName(DeclaredType type) {
      TypeElement element = (TypeElement) type.asElement();
      return element.getQualifiedName();
   }

   public static boolean isObject(TypeMirror type) {
      return isDeclaredOfName(type, "java.lang.Object");
   }

   public static boolean isClass(TypeMirror type) {
      return isDeclaredOfName(type, "java.lang.Class");
   }

   public static boolean isString(TypeMirror type) {
      return isDeclaredOfName(type, "java.lang.String");
   }

   public static boolean isBooleanType(TypeMirror type) {
      return isDeclaredOfName(type, "java.lang.Boolean") || type.getKind() == TypeKind.BOOLEAN;
   }

   public static boolean isDeclaredOfName(TypeMirror type, CharSequence qualifiedName) {
      return type.getKind() == TypeKind.DECLARED
         && getQualifiedName((DeclaredType) type).contentEquals(qualifiedName);
   }

   public static boolean isBoxedPrimitive(TypeMirror type) {
      if (type.getKind() != TypeKind.DECLARED) {
         return false;
      }

      String qualifiedName = getQualifiedName((DeclaredType) type).toString();

      return (qualifiedName.equals("java.lang.Boolean")
         || qualifiedName.equals("java.lang.Byte")
         || qualifiedName.equals("java.lang.Character")
         || qualifiedName.equals("java.lang.Short")
         || qualifiedName.equals("java.lang.Integer")
         || qualifiedName.equals("java.lang.Long")
         || qualifiedName.equals("java.lang.Double")
         || qualifiedName.equals("java.lang.Float"));
   }

   public static boolean isImmutableTypeInJdk(TypeMirror type) {
      return isPrimitive(type)
         || (type.getKind() == TypeKind.DECLARED
         && ImmutableTypes.isImmutable(
         getQualifiedName((DeclaredType) type).toString()));
   }

   public static boolean isThrowable(TypeMirror type) {
      while (type != null && type.getKind() == TypeKind.DECLARED) {
         DeclaredType dt = (DeclaredType) type;
         TypeElement elem = (TypeElement) dt.asElement();
         Name name = elem.getQualifiedName();
         if ("java.lang.Throwable".contentEquals(name)) {
            return true;
         }
         type = elem.getSuperclass();
      }
      return false;
   }

   public static boolean isAnonymous(TypeMirror type) {
      return (type instanceof DeclaredType)
         && ((TypeElement) ((DeclaredType) type).asElement()).getNestingKind()
         == NestingKind.ANONYMOUS;
   }

   public static boolean isPrimitive(TypeMirror type) {
      switch (type.getKind()) {
         case BOOLEAN:
         case BYTE:
         case CHAR:
         case DOUBLE:
         case FLOAT:
         case INT:
         case LONG:
         case SHORT:
            return true;
         default:
            return false;
      }
   }

   public static boolean areSameDeclaredTypes(Type.ClassType t1, Type.ClassType t2) {
      // Do a cheaper test first
      if (t1.tsym.name != t2.tsym.name) {
         return false;
      }
      return t1.toString().equals(t1.toString());
   }

   public static boolean areSamePrimitiveTypes(TypeMirror left, TypeMirror right) {
      if (!isPrimitive(left) || !isPrimitive(right)) {
         return false;
      }

      return (left.getKind() == right.getKind());
   }

   public static boolean isNumeric(TypeMirror type) {
      switch (type.getKind()) {
         case BYTE:
         case CHAR:
         case DOUBLE:
         case FLOAT:
         case INT:
         case LONG:
         case SHORT:
            return true;
         default:
            return false;
      }
   }

   public static boolean isIntegral(TypeMirror type) {
      switch (type.getKind()) {
         case BYTE:
         case CHAR:
         case INT:
         case LONG:
         case SHORT:
            return true;
         default:
            return false;
      }
   }

   public static boolean isFloating(TypeMirror type) {
      switch (type.getKind()) {
         case DOUBLE:
         case FLOAT:
            return true;
         default:
            return false;
      }
   }

   public static TypeKind widenedNumericType(TypeMirror left, TypeMirror right) {
      if (!isNumeric(left) || !isNumeric(right)) {
         return TypeKind.NONE;
      }

      TypeKind leftKind = left.getKind();
      TypeKind rightKind = right.getKind();

      if (leftKind == TypeKind.DOUBLE || rightKind == TypeKind.DOUBLE) {
         return TypeKind.DOUBLE;
      }

      if (leftKind == TypeKind.FLOAT || rightKind == TypeKind.FLOAT) {
         return TypeKind.FLOAT;
      }

      if (leftKind == TypeKind.LONG || rightKind == TypeKind.LONG) {
         return TypeKind.LONG;
      }

      return TypeKind.INT;
   }

   public static TypeMirror upperBound(TypeMirror type) {
      do {
         if (type instanceof TypeVariable) {
            TypeVariable tvar = (TypeVariable) type;
            if (tvar.getUpperBound() != null) {
               type = tvar.getUpperBound();
            } else {
               break;
            }
         } else if (type instanceof WildcardType) {
            WildcardType wc = (WildcardType) type;
            if (wc.getExtendsBound() != null) {
               type = wc.getExtendsBound();
            } else {
               break;
            }
         } else {
            break;
         }
      } while (true);
      return type;
   }

   public static @Nullable
   TypeParameterElement wildcardToTypeParam(
      final Type.WildcardType wildcard) {

      final Element typeParamElement;
      if (wildcard.bound != null) {
         typeParamElement = wildcard.bound.asElement();
      } else {
         typeParamElement = null;
      }

      return (TypeParameterElement) typeParamElement;
   }

   public static Type wildUpperBound(TypeMirror tm, ProcessingEnvironment env) {
      Type t = (Type) tm;
      if (t.hasTag(TypeTag.WILDCARD)) {
         Context context = ((JavacProcessingEnvironment) env).getContext();
         Type.WildcardType w = (Type.WildcardType) TypeAnnotationUtils.unannotatedType(t);
         if (w.isSuperBound()) { // returns true if w is unbound
            Symtab syms = Symtab.instance(context);
            // w.bound is null if the wildcard is from bytecode.
            return w.bound == null ? syms.objectType : w.bound.getUpperBound();
         } else {
            return wildUpperBound(w.type, env);
         }
      } else {
         return TypeAnnotationUtils.unannotatedType(t);
      }
   }

   public static Type wildLowerBound(TypeMirror tm, ProcessingEnvironment env) {
      Type t = (Type) tm;
      if (t.hasTag(TypeTag.WILDCARD)) {
         Context context = ((JavacProcessingEnvironment) env).getContext();
         Symtab syms = Symtab.instance(context);
         Type.WildcardType w = (Type.WildcardType) TypeAnnotationUtils.unannotatedType(t);
         return w.isExtendsBound() ? syms.botType : wildLowerBound(w.type, env);
      } else {
         return TypeAnnotationUtils.unannotatedType(t);
      }
   }

   public static TypeMirror typeFromClass(Class<?> clazz, Types types, Elements elements) {
      if (clazz == void.class) {
         return types.getNoType(TypeKind.VOID);
      } else if (clazz.isPrimitive()) {
         String primitiveName = clazz.getName().toUpperCase();
         TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
         return types.getPrimitiveType(primitiveKind);
      } else if (clazz.isArray()) {
         TypeMirror componentType = typeFromClass(clazz.getComponentType(), types, elements);
         return types.getArrayType(componentType);
      } else {
         String name = clazz.getCanonicalName();
         assert name != null : "@AssumeAssertion(nullness): assumption";
         TypeElement element = elements.getTypeElement(name);
         if (element == null) {
            throw new RuntimeException("Unrecognized class: " + clazz);
         }
         return element.asType();
      }
   }

   public static ArrayType createArrayType(TypeMirror componentType, Types types) {
      JavacTypes t = (JavacTypes) types;
      return t.getArrayType(componentType);
   }

   public static boolean isBoxOf(TypeMirror declaredType, TypeMirror primitiveType) {
      if (declaredType.getKind() != TypeKind.DECLARED) {
         return false;
      }

      final String qualifiedName = getQualifiedName((DeclaredType) declaredType).toString();
      switch (primitiveType.getKind()) {
         case BOOLEAN:
            return qualifiedName.equals("java.lang.Boolean");
         case BYTE:
            return qualifiedName.equals("java.lang.Byte");
         case CHAR:
            return qualifiedName.equals("java.lang.Character");
         case DOUBLE:
            return qualifiedName.equals("java.lang.Double");
         case FLOAT:
            return qualifiedName.equals("java.lang.Float");
         case INT:
            return qualifiedName.equals("java.lang.Integer");
         case LONG:
            return qualifiedName.equals("java.lang.Long");
         case SHORT:
            return qualifiedName.equals("java.lang.Short");

         default:
            return false;
      }
   }

   public static @Nullable TypeMirror findConcreteUpperBound(final TypeMirror boundedType) {
      TypeMirror effectiveUpper = boundedType;
      outerLoop:
      while (true) {
         switch (effectiveUpper.getKind()) {
            case WILDCARD:
               effectiveUpper =
                  ((javax.lang.model.type.WildcardType) effectiveUpper).getExtendsBound();
               if (effectiveUpper == null) {
                  return null;
               }
               break;

            case TYPEVAR:
               effectiveUpper = ((TypeVariable) effectiveUpper).getUpperBound();
               break;

            default:
               break outerLoop;
         }
      }
      return effectiveUpper;
   }

   public static boolean isErasedSubtype(TypeMirror subtype, TypeMirror supertype, Types types) {
      return types.isSubtype(types.erasure(subtype), types.erasure(supertype));
   }

   public static boolean isCaptured(TypeMirror typeVar) {
      if (typeVar.getKind() != TypeKind.TYPEVAR) {
         return false;
      }
      return ((Type.TypeVar) TypeAnnotationUtils.unannotatedType(typeVar)).isCaptured();
   }

   /** If typeVar is a captured wildcard, returns that wildcard; otherwise returns {@code null}. */
   public static @Nullable WildcardType getCapturedWildcard(TypeVariable typeVar) {
      if (isCaptured(typeVar)) {
         return ((Type.CapturedType) TypeAnnotationUtils.unannotatedType(typeVar)).wildcard;
      }
      return null;
   }

   public static boolean isClassType(TypeMirror type) {
      return (type instanceof Type.ClassType);
   }

   public static TypeMirror leastUpperBound(
      TypeMirror tm1, TypeMirror tm2, ProcessingEnvironment processingEnv) {
      Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
      Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
      JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
      com.sun.tools.javac.code.Types types =
         com.sun.tools.javac.code.Types.instance(javacEnv.getContext());
      // Handle the 'null' type manually (not done by types.lub).
      if (t1.getKind() == TypeKind.NULL) {
         return t2;
      }
      if (t2.getKind() == TypeKind.NULL) {
         return t1;
      }
      if (t1.getKind() == TypeKind.WILDCARD) {
         WildcardType wc1 = (WildcardType) t1;
         Type bound = (Type) wc1.getExtendsBound();
         if (bound == null) {
            // Implicit upper bound of java.lang.Object
            Elements elements = processingEnv.getElementUtils();
            return elements.getTypeElement("java.lang.Object").asType();
         }
         t1 = bound;
      }
      if (t2.getKind() == TypeKind.WILDCARD) {
         WildcardType wc2 = (WildcardType) t2;
         Type bound = (Type) wc2.getExtendsBound();
         if (bound == null) {
            // Implicit upper bound of java.lang.Object
            Elements elements = processingEnv.getElementUtils();
            return elements.getTypeElement("java.lang.Object").asType();
         }
         t2 = bound;
      }
      if (types.isSameType(t1, t2)) {
         // Special case if the two types are equal.
         return t1;
      }
      // Special case for primitives.
      if (isPrimitive(t1) || isPrimitive(t2)) {
         if (types.isAssignable(t1, t2)) {
            return t2;
         } else if (types.isAssignable(t2, t1)) {
            return t1;
         } else {
            Elements elements = processingEnv.getElementUtils();
            return elements.getTypeElement("java.lang.Object").asType();
         }
      }
      return types.lub(t1, t2);
   }

   public static TypeMirror greatestLowerBound(
      TypeMirror tm1, TypeMirror tm2, ProcessingEnvironment processingEnv) {
      Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
      Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
      JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
      com.sun.tools.javac.code.Types types =
         com.sun.tools.javac.code.Types.instance(javacEnv.getContext());
      if (types.isSameType(t1, t2)) {
         // Special case if the two types are equal.
         return t1;
      }
      // Handle the 'null' type manually.
      if (t1.getKind() == TypeKind.NULL) {
         return t1;
      }
      if (t2.getKind() == TypeKind.NULL) {
         return t2;
      }
      // Special case for primitives.
      if (isPrimitive(t1) || isPrimitive(t2)) {
         if (types.isAssignable(t1, t2)) {
            return t1;
         } else if (types.isAssignable(t2, t1)) {
            return t2;
         } else {
            // Javac types.glb returns TypeKind.Error when the GLB does
            // not exist, but we can't create one.  Use TypeKind.NONE
            // instead.
            return processingEnv.getTypeUtils().getNoType(TypeKind.NONE);
         }
      }
      if (t1.getKind() == TypeKind.WILDCARD) {
         return t2;
      }
      if (t2.getKind() == TypeKind.WILDCARD) {
         return t1;
      }

      // If neither type is a primitive type, null type, or wildcard
      // and if the types are not the same, use javac types.glb
      return types.glb(t1, t2);
   }

   public static TypeMirror substituteMethodReturnType(
      Element methodElement, TypeMirror substitutedReceiverType, ProcessingEnvironment env) {

      com.sun.tools.javac.code.Types types =
         com.sun.tools.javac.code.Types.instance(InternalUtils.getJavacContext(env));

      Type substitutedMethodType =
         types.memberType((Type) substitutedReceiverType, (Symbol) methodElement);
      return substitutedMethodType.getReturnType();
   }

   public static @Nullable TypeElement getTypeElement(TypeMirror type) {
      Element element = ((Type) type).asElement();
      if (ElementUtils.isClassElement(element)) {
         return (TypeElement) element;
      }
      return null;
   }

   public static TypeMirror asSuper(
      TypeMirror type, TypeMirror superType, ProcessingEnvironment env) {
      Context ctx = ((JavacProcessingEnvironment) env).getContext();
      com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);
      return javacTypes.asSuper((Type) type, ((Type) superType).tsym);
   }

   public static boolean isFunctionalInterface(TypeMirror type, ProcessingEnvironment env) {
      Context ctx = ((JavacProcessingEnvironment) env).getContext();
      com.sun.tools.javac.code.Types javacTypes = com.sun.tools.javac.code.Types.instance(ctx);
      return javacTypes.isFunctionalInterface((Type) type);
   }
}
