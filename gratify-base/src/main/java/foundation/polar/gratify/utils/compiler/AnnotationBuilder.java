package foundation.polar.gratify.utils.compiler;

import foundation.polar.gratify.annotations.checker.dataflow.SideEffectFree;
import foundation.polar.gratify.annotations.checker.interning.Interned;
import foundation.polar.gratify.lang.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.*;

public class AnnotationBuilder {
   private final Elements elements;
   private final Types types;
   private final TypeElement annotationElt;
   private final DeclaredType annotationType;
   private final Map<ExecutableElement, AnnotationValue> elementValues;

   @SuppressWarnings("nullness") // getCanonicalName expected to be non-null
   public AnnotationBuilder(ProcessingEnvironment env, Class<? extends Annotation> anno) {
      this(env, anno.getCanonicalName());
   }

   public AnnotationBuilder(ProcessingEnvironment env, CharSequence name) {
      this.elements = env.getElementUtils();
      this.types = env.getTypeUtils();
      this.annotationElt = elements.getTypeElement(name);
      if (annotationElt == null) {
         throw new UserError("Could not find annotation: " + name + ". Is it on the classpath?");
      }
      assert annotationElt.getKind() == ElementKind.ANNOTATION_TYPE;
      this.annotationType = (DeclaredType) annotationElt.asType();
      this.elementValues = new LinkedHashMap<>();
   }

   public AnnotationBuilder(ProcessingEnvironment env, AnnotationMirror annotation) {
      this.elements = env.getElementUtils();
      this.types = env.getTypeUtils();

      this.annotationType = annotation.getAnnotationType();
      this.annotationElt = (TypeElement) annotationType.asElement();

      this.elementValues = new LinkedHashMap<>();
      // AnnotationValues are immutable so putAll should suffice
      this.elementValues.putAll(annotation.getElementValues());
   }

   public static AnnotationMirror fromClass(
      Elements elements, Class<? extends Annotation> aClass) {
      String name = aClass.getCanonicalName();
      assert name != null : "@AssumeAssertion(nullness): assumption";
      AnnotationMirror res = fromName(elements, name);
      if (res == null) {
         throw new UserError(
            "AnnotationBuilder: error: fromClass can't load Class %s%n"
               + "ensure the class is on the compilation classpath",
            name);
      }
      return res;
   }

   public static @Nullable AnnotationMirror fromName(Elements elements, CharSequence name) {
      final TypeElement annoElt = elements.getTypeElement(name);
      if (annoElt == null) {
         return null;
      }
      if (annoElt.getKind() != ElementKind.ANNOTATION_TYPE) {
         throw new RuntimeException(annoElt + " is not an annotation");
      }

      final DeclaredType annoType = (DeclaredType) annoElt.asType();
      if (annoType == null) {
         return null;
      }
      AnnotationMirror result =
         new CheckerFrameworkAnnotationMirror(annoType, Collections.emptyMap());
      return result;
   }


   private boolean wasBuilt = false;

   private void assertNotBuilt() {
      if (wasBuilt) {
         throw new RuntimeException("AnnotationBuilder: error: type was already built");
      }
   }

   public AnnotationMirror build() {
      assertNotBuilt();
      wasBuilt = true;
      return new CheckerFrameworkAnnotationMirror(annotationType, elementValues);
   }

   public void copyElementValuesFromAnnotation(
      AnnotationMirror valueHolder, String... ignorableElements) {
      Set<String> ignorableElementsSet = new HashSet<>(Arrays.asList(ignorableElements));
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> eltValToCopy :
         valueHolder.getElementValues().entrySet()) {
         Name eltNameToCopy = eltValToCopy.getKey().getSimpleName();
         if (ignorableElementsSet.contains(eltNameToCopy.toString())) {
            continue;
         }
         elementValues.put(findElement(eltNameToCopy), eltValToCopy.getValue());
      }
      return;
   }

   public void copyRenameElementValuesFromAnnotation(
      AnnotationMirror valueHolder, Map<String, String> elementNameRenaming) {

      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> eltValToCopy :
         valueHolder.getElementValues().entrySet()) {

         String sourceName = eltValToCopy.getKey().getSimpleName().toString();
         String targetName = elementNameRenaming.get(sourceName);
         if (targetName == null) {
            continue;
         }
         elementValues.put(findElement(targetName), eltValToCopy.getValue());
      }
   }

   public AnnotationBuilder setValue(CharSequence elementName, AnnotationMirror value) {
      setValue(elementName, (Object) value);
      return this;
   }

   public AnnotationBuilder setValue(CharSequence elementName, List<? extends Object> values) {
      assertNotBuilt();
      List<AnnotationValue> avalues = new ArrayList<>(values.size());
      ExecutableElement var = findElement(elementName);
      TypeMirror expectedType = var.getReturnType();
      if (expectedType.getKind() != TypeKind.ARRAY) {
         throw new RuntimeException("value is an array while expected type is not");
      }
      expectedType = ((ArrayType) expectedType).getComponentType();

      for (Object v : values) {
         checkSubtype(expectedType, v);
         avalues.add(createValue(v));
      }
      AnnotationValue aval = createValue(avalues);
      elementValues.put(var, aval);
      return this;
   }

   public AnnotationBuilder setValue(CharSequence elementName, Object[] values) {
      return setValue(elementName, Arrays.asList(values));
   }

   public AnnotationBuilder setValue(CharSequence elementName, Boolean value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Character value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Double value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Float value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Integer value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Long value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, Short value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder setValue(CharSequence elementName, String value) {
      return setValue(elementName, (Object) value);
   }

   public AnnotationBuilder removeElement(CharSequence elementName) {
      assertNotBuilt();
      ExecutableElement var = findElement(elementName);
      elementValues.remove(var);
      return this;
   }

   private TypeMirror getErasedOrBoxedType(TypeMirror type) {
      // See com.sun.tools.javac.code.Attribute.Class.makeClassType()
      return type.getKind().isPrimitive()
         ? types.boxedClass((PrimitiveType) type).asType()
         : types.erasure(type);
   }

   public AnnotationBuilder setValue(CharSequence elementName, TypeMirror value) {
      assertNotBuilt();
      value = getErasedOrBoxedType(value);
      AnnotationValue val = createValue(value);
      ExecutableElement var = findElement(elementName);
      // Check subtyping
      if (!TypesUtils.isClass(var.getReturnType())) {
         throw new RuntimeException("expected " + var.getReturnType());
      }

      elementValues.put(var, val);
      return this;
   }

   private TypeMirror typeFromClass(Class<?> clazz) {
      if (clazz == void.class) {
         return types.getNoType(TypeKind.VOID);
      } else if (clazz.isPrimitive()) {
         String primitiveName = clazz.getName().toUpperCase();
         TypeKind primitiveKind = TypeKind.valueOf(primitiveName);
         return types.getPrimitiveType(primitiveKind);
      } else if (clazz.isArray()) {
         TypeMirror componentType = typeFromClass(clazz.getComponentType());
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

   public AnnotationBuilder setValue(CharSequence elementName, Class<?> value) {
      TypeMirror type = typeFromClass(value);
      return setValue(elementName, getErasedOrBoxedType(type));
   }

   public AnnotationBuilder setValue(CharSequence elementName, Enum<?> value) {
      assertNotBuilt();
      VariableElement enumElt = findEnumElement(value);
      return setValue(elementName, enumElt);
   }

   public AnnotationBuilder setValue(CharSequence elementName, VariableElement value) {
      ExecutableElement var = findElement(elementName);
      if (var.getReturnType().getKind() != TypeKind.DECLARED) {
         throw new RuntimeException("expected a non enum: " + var.getReturnType());
      }
      if (!((DeclaredType) var.getReturnType()).asElement().equals(value.getEnclosingElement())) {
         throw new RuntimeException("expected a different type of enum: " + value.getEnclosingElement());
      }
      elementValues.put(var, createValue(value));
      return this;
   }

   // Keep this version synchronized with the VariableElement[] version below
   public AnnotationBuilder setValue(CharSequence elementName, Enum<?>[] values) {
      assertNotBuilt();

      if (values.length == 0) {
         setValue(elementName, Collections.emptyList());
         return this;
      }

      VariableElement enumElt = findEnumElement(values[0]);
      ExecutableElement var = findElement(elementName);

      TypeMirror expectedType = var.getReturnType();
      if (expectedType.getKind() != TypeKind.ARRAY) {
         throw new RuntimeException("expected a non array: " + var.getReturnType());
      }

      expectedType = ((ArrayType) expectedType).getComponentType();
      if (expectedType.getKind() != TypeKind.DECLARED) {
         throw new RuntimeException("expected a non enum component type: " + var.getReturnType());
      }
      if (!((DeclaredType) expectedType).asElement().equals(enumElt.getEnclosingElement())) {
         throw new RuntimeException(
            "expected a different type of enum: " + enumElt.getEnclosingElement());
      }

      List<AnnotationValue> res = new ArrayList<>(values.length);
      for (Enum<?> ev : values) {
         checkSubtype(expectedType, ev);
         enumElt = findEnumElement(ev);
         res.add(createValue(enumElt));
      }
      AnnotationValue val = createValue(res);
      elementValues.put(var, val);
      return this;
   }

   // Keep this version synchronized with the Enum<?>[] version above.
   // Which one is more useful/general? Unifying adds overhead of creating
   // another array.
   public AnnotationBuilder setValue(CharSequence elementName, VariableElement[] values) {
      assertNotBuilt();
      ExecutableElement var = findElement(elementName);

      TypeMirror expectedType = var.getReturnType();
      if (expectedType.getKind() != TypeKind.ARRAY) {
         throw new RuntimeException("expected an array, but found: " + expectedType);
      }

      expectedType = ((ArrayType) expectedType).getComponentType();
      if (expectedType.getKind() != TypeKind.DECLARED) {
         throw new RuntimeException(
            "expected a declared component type, but found: "
               + expectedType
               + " kind: "
               + expectedType.getKind());
      }
      if (!types.isSameType((DeclaredType) expectedType, values[0].asType())) {
         throw new RuntimeException(
            "expected a different declared component type: "
               + expectedType
               + " vs. "
               + values[0]);
      }

      List<AnnotationValue> res = new ArrayList<>(values.length);
      for (VariableElement ev : values) {
         checkSubtype(expectedType, ev);
         // Is there a better way to distinguish between enums and
         // references to constants?
         if (ev.getConstantValue() != null) {
            res.add(createValue(ev.getConstantValue()));
         } else {
            res.add(createValue(ev));
         }
      }
      AnnotationValue val = createValue(res);
      elementValues.put(var, val);
      return this;
   }

   private VariableElement findEnumElement(Enum<?> value) {
      String enumClass = value.getDeclaringClass().getCanonicalName();
      assert enumClass != null : "@AssumeAssertion(nullness): assumption";
      TypeElement enumClassElt = elements.getTypeElement(enumClass);
      assert enumClassElt != null;
      for (Element enumElt : enumClassElt.getEnclosedElements()) {
         if (enumElt.getSimpleName().contentEquals(value.name())) {
            return (VariableElement) enumElt;
         }
      }
      throw new RuntimeException("cannot be here");
   }

   private AnnotationBuilder setValue(CharSequence key, Object value) {
      assertNotBuilt();
      AnnotationValue val = createValue(value);
      ExecutableElement var = findElement(key);
      checkSubtype(var.getReturnType(), value);
      elementValues.put(var, val);
      return this;
   }

   public ExecutableElement findElement(CharSequence key) {
      for (ExecutableElement elt : ElementFilter.methodsIn(annotationElt.getEnclosedElements())) {
         if (elt.getSimpleName().contentEquals(key)) {
            return elt;
         }
      }
      throw new RuntimeException("Couldn't find " + key + " element in " + annotationElt);
   }

   private void checkSubtype(TypeMirror expected, Object givenValue) {
      if (expected.getKind().isPrimitive()) {
         expected = types.boxedClass((PrimitiveType) expected).asType();
      }

      if (expected.getKind() == TypeKind.DECLARED
         && TypesUtils.isClass(expected)
         && givenValue instanceof TypeMirror) {
         return;
      }

      TypeMirror found;
      boolean isSubtype;

      if (expected.getKind() == TypeKind.DECLARED
         && ((DeclaredType) expected).asElement().getKind() == ElementKind.ANNOTATION_TYPE
         && givenValue instanceof AnnotationMirror) {
         found = ((AnnotationMirror) givenValue).getAnnotationType();
         isSubtype =
            ((DeclaredType) expected)
               .asElement()
               .equals(((DeclaredType) found).asElement());
      } else if (givenValue instanceof AnnotationMirror) {
         found = ((AnnotationMirror) givenValue).getAnnotationType();
         // TODO: why is this always failing???
         isSubtype = false;
      } else if (givenValue instanceof VariableElement) {
         found = ((VariableElement) givenValue).asType();
         if (expected.getKind() == TypeKind.DECLARED) {
            isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
         } else {
            isSubtype = false;
         }
      } else {
         String name = givenValue.getClass().getCanonicalName();
         assert name != null : "@AssumeAssertion(nullness): assumption";
         found = elements.getTypeElement(name).asType();
         isSubtype = types.isSubtype(types.erasure(found), types.erasure(expected));
      }
      if (!isSubtype) {
         // Annotations in stub files sometimes are the same type, but Types#isSubtype fails
         // anyways.
         isSubtype = found.toString().equals(expected.toString());
      }

      if (!isSubtype) {
         throw new RuntimeException(
            "given value differs from expected; "
               + "found: "
               + found
               + "; expected: "
               + expected);
      }
   }

   private static AnnotationValue createValue(final Object obj) {
      return new CheckerFrameworkAnnotationValue(obj);
   }

   /** Implementation of AnnotationMirror used by the Checker Framework. */
   /* default visibility to allow access from within package. */
   static class CheckerFrameworkAnnotationMirror implements AnnotationMirror {
      /** The interned toString value. */
      private @Nullable @Interned String toStringVal;
      /** The annotation type. */
      private final DeclaredType annotationType;
      /** The element values. */
      private final Map<ExecutableElement, AnnotationValue> elementValues;
      /** The annotation name. */
      // default visibility to allow access from within package.
      final @Interned String annotationName;

      CheckerFrameworkAnnotationMirror(
         DeclaredType at, Map<ExecutableElement, AnnotationValue> ev) {
         this.annotationType = at;
         final TypeElement elm = (TypeElement) at.asElement();
         this.annotationName = elm.getQualifiedName().toString().intern();
         this.elementValues = ev;
      }

      @Override
      public DeclaredType getAnnotationType() {
         return annotationType;
      }

      @Override
      public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
         return Collections.unmodifiableMap(elementValues);
      }

      @SideEffectFree
      @Override
      public String toString() {
         if (toStringVal != null) {
            return toStringVal;
         }
         StringBuilder buf = new StringBuilder();
         buf.append("@");
         buf.append(annotationName);
         int len = elementValues.size();
         if (len > 0) {
            buf.append('(');
            boolean first = true;
            for (Map.Entry<ExecutableElement, AnnotationValue> pair :
               elementValues.entrySet()) {
               if (!first) {
                  buf.append(", ");
               }
               first = false;

               String name = pair.getKey().getSimpleName().toString();
               if (len > 1 || !name.equals("value")) {
                  buf.append(name);
                  buf.append('=');
               }
               buf.append(pair.getValue());
            }
            buf.append(')');
         }
         toStringVal = buf.toString().intern();
         return toStringVal;

         // return "@" + annotationType + "(" + elementValues + ")";
      }
   }

   private static class CheckerFrameworkAnnotationValue implements AnnotationValue {
      /** The value. */
      private final Object value;
      /** The interned value of toString. */
      private @Nullable @Interned String toStringVal;

      /** Create an annotation value. */
      CheckerFrameworkAnnotationValue(Object obj) {
         this.value = obj;
      }

      @Override
      public Object getValue() {
         return value;
      }

      @SideEffectFree
      @Override
      public String toString() {
         if (this.toStringVal != null) {
            return this.toStringVal;
         }
         String toStringVal;
         if (value instanceof String) {
            toStringVal = "\"" + value + "\"";
         } else if (value instanceof Character) {
            toStringVal = "\'" + value + "\'";
         } else if (value instanceof List<?>) {
            StringBuilder sb = new StringBuilder();
            List<?> list = (List<?>) value;
            sb.append('{');
            boolean isFirst = true;
            for (Object o : list) {
               if (!isFirst) {
                  sb.append(", ");
               }
               isFirst = false;
               sb.append(Objects.toString(o));
            }
            sb.append('}');
            toStringVal = sb.toString();
         } else if (value instanceof VariableElement) {
            // for Enums
            VariableElement var = (VariableElement) value;
            String encl = var.getEnclosingElement().toString();
            if (!encl.isEmpty()) {
               encl = encl + '.';
            }
            toStringVal = encl + var;
         } else if (value instanceof TypeMirror && TypesUtils.isClassType((TypeMirror) value)) {
            toStringVal = value.toString() + ".class";
         } else {
            toStringVal = value.toString();
         }
         this.toStringVal = toStringVal.intern();
         return this.toStringVal;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
         if (value instanceof AnnotationMirror) {
            return v.visitAnnotation((AnnotationMirror) value, p);
         } else if (value instanceof List) {
            return v.visitArray((List<? extends AnnotationValue>) value, p);
         } else if (value instanceof Boolean) {
            return v.visitBoolean((Boolean) value, p);
         } else if (value instanceof Character) {
            return v.visitChar((Character) value, p);
         } else if (value instanceof Double) {
            return v.visitDouble((Double) value, p);
         } else if (value instanceof VariableElement) {
            return v.visitEnumConstant((VariableElement) value, p);
         } else if (value instanceof Float) {
            return v.visitFloat((Float) value, p);
         } else if (value instanceof Integer) {
            return v.visitInt((Integer) value, p);
         } else if (value instanceof Long) {
            return v.visitLong((Long) value, p);
         } else if (value instanceof Short) {
            return v.visitShort((Short) value, p);
         } else if (value instanceof String) {
            return v.visitString((String) value, p);
         } else if (value instanceof TypeMirror) {
            return v.visitType((TypeMirror) value, p);
         } else {
            assert false : " unknown type : " + v.getClass();
            return v.visitUnknown(this, p);
         }
      }

      @Override
      public boolean equals(@Nullable Object obj) {
         // System.out.printf("Calling CFAV.equals()%n");
         if (!(obj instanceof AnnotationValue)) {
            return false;
         }
         AnnotationValue other = (AnnotationValue) obj;
         return Objects.equals(this.getValue(), other.getValue());
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(this.value);
      }
   }
}
