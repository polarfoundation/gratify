package foundation.polar.gratify.utils.compiler;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import foundation.polar.gratify.annotations.checker.signature.DotSeparatedIdentifiers;
import foundation.polar.gratify.annotations.utils.SourceTreeUtils;
import foundation.polar.gratify.lang.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.util.*;

public final class AnnotationUtils {

   private AnnotationUtils() {
      throw new AssertionError("Class AnnotationUtils cannot be instantiated.");
   }

   public static final String annotationName(AnnotationMirror annotation) {
      if (annotation instanceof AnnotationBuilder.CheckerFrameworkAnnotationMirror) {
         return ((AnnotationBuilder.CheckerFrameworkAnnotationMirror) annotation).annotationName;
      }
      final DeclaredType annoType = annotation.getAnnotationType();
      final TypeElement elm = (TypeElement) annoType.asElement();
      String name = elm.getQualifiedName().toString();
      return name;
   }

   public static boolean areSame(AnnotationMirror a1, AnnotationMirror a2) {
      if (a1 == a2) {
         return true;
      }

      if (!areSameByName(a1, a2)) {
         return false;
      }

      // This commented implementation is less efficient.  It is also wrong:  it requires a
      // particular order for fields, and it distinguishes the long constants "33" and "33L".
      // Map<? extends ExecutableElement, ? extends AnnotationValue> elval1 =
      //         getElementValuesWithDefaults(a1);
      // Map<? extends ExecutableElement, ? extends AnnotationValue> elval2 =
      //         getElementValuesWithDefaults(a2);
      // return elval1.toString().equals(elval2.toString());

      return sameElementValues(a1, a2);
   }

   public static boolean areSameByName(AnnotationMirror a1, AnnotationMirror a2) {
      if (a1 == a2) {
         return true;
      }
      if (a1 == null) {
         throw new RuntimeException("Unexpected null first argument to areSameByName");
      }
      if (a2 == null) {
         throw new RuntimeException("Unexpected null second argument to areSameByName");
      }

      if (a1 instanceof AnnotationBuilder.CheckerFrameworkAnnotationMirror
         && a2 instanceof AnnotationBuilder.CheckerFrameworkAnnotationMirror) {
         return ((AnnotationBuilder.CheckerFrameworkAnnotationMirror) a1).annotationName
            == ((AnnotationBuilder.CheckerFrameworkAnnotationMirror) a2).annotationName;
      }

      return annotationName(a1).equals(annotationName(a2));
   }

   public static boolean areSameByName(AnnotationMirror am, String aname) {
      return aname.equals(annotationName(am));
   }

   public static boolean areSameByClass(
      AnnotationMirror am, Class<? extends Annotation> annoClass) {
      String canonicalName = annoClass.getCanonicalName();
      assert canonicalName != null : "@AssumeAssertion(nullness): assumption";
      return areSameByName(am, canonicalName);
   }

   public static boolean areSame(
      Collection<? extends AnnotationMirror> c1, Collection<? extends AnnotationMirror> c2) {
      if (c1.size() != c2.size()) {
         return false;
      }
      if (c1.size() == 1) {
         return areSame(c1.iterator().next(), c2.iterator().next());
      }

      // while loop depends on SortedSet implementation.
      SortedSet<AnnotationMirror> s1 = createAnnotationSet();
      SortedSet<AnnotationMirror> s2 = createAnnotationSet();
      s1.addAll(c1);
      s2.addAll(c2);
      Iterator<AnnotationMirror> iter1 = s1.iterator();
      Iterator<AnnotationMirror> iter2 = s2.iterator();

      while (iter1.hasNext()) {
         AnnotationMirror anno1 = iter1.next();
         AnnotationMirror anno2 = iter2.next();
         if (!areSame(anno1, anno2)) {
            return false;
         }
      }
      return true;
   }

   public static boolean containsSame(
      Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
      return getSame(c, anno) != null;
   }

   public static @Nullable AnnotationMirror getSame(
      Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
      for (AnnotationMirror an : c) {
         if (AnnotationUtils.areSame(an, anno)) {
            return an;
         }
      }
      return null;
   }

   public static boolean containsSameByClass(
      Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
      return getAnnotationByClass(c, anno) != null;
   }

   public static @Nullable AnnotationMirror getAnnotationByClass(
      Collection<? extends AnnotationMirror> c, Class<? extends Annotation> anno) {
      for (AnnotationMirror an : c) {
         if (AnnotationUtils.areSameByClass(an, anno)) {
            return an;
         }
      }
      return null;
   }

   public static boolean containsSameByName(
      Collection<? extends AnnotationMirror> c, String anno) {
      return getAnnotationByName(c, anno) != null;
   }

   public static @Nullable AnnotationMirror getAnnotationByName(
      Collection<? extends AnnotationMirror> c, String anno) {
      for (AnnotationMirror an : c) {
         if (AnnotationUtils.areSameByName(an, anno)) {
            return an;
         }
      }
      return null;
   }

   public static boolean containsSameByName(
      Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
      return getSameByName(c, anno) != null;
   }

   /**
    * Returns the AnnotationMirror in {@code c} that is the same annotation as {@code anno}
    * ignoring values.
    *
    * @param c a collection of AnnotationMirrors
    * @param anno the annotation whose name to search for in c
    * @return AnnotationMirror with the same class as {@code anno} iff c contains anno, according
    *     to areSameByName; otherwise, {@code null}
    */
   public static @Nullable AnnotationMirror getSameByName(
      Collection<? extends AnnotationMirror> c, AnnotationMirror anno) {
      for (AnnotationMirror an : c) {
         if (AnnotationUtils.areSameByName(an, anno)) {
            return an;
         }
      }
      return null;
   }

   public static int compareAnnotationMirrors(AnnotationMirror a1, AnnotationMirror a2) {
      if (!AnnotationUtils.areSameByName(a1, a2)) {
         return annotationName(a1).compareTo(annotationName(a2));
      }

      // The annotations have the same name, but different values, so compare values.
      Map<? extends ExecutableElement, ? extends AnnotationValue> vals1 = a1.getElementValues();
      Map<? extends ExecutableElement, ? extends AnnotationValue> vals2 = a2.getElementValues();
      Set<ExecutableElement> sortedElements =
         new TreeSet<>(Comparator.comparing(ElementUtils::getSimpleName));
      sortedElements.addAll(
         ElementFilter.methodsIn(a1.getAnnotationType().asElement().getEnclosedElements()));

      for (ExecutableElement meth : sortedElements) {
         AnnotationValue aval1 = vals1.get(meth);
         AnnotationValue aval2 = vals2.get(meth);
         if (aval1 == null) {
            aval1 = meth.getDefaultValue();
         }
         if (aval2 == null) {
            aval2 = meth.getDefaultValue();
         }
         int result = compareAnnotationValue(aval1, aval2);
         if (result != 0) {
            return result;
         }
      }
      return 0;
   }

   private static int compareAnnotationValue(AnnotationValue av1, AnnotationValue av2) {
      if (av1 == av2) {
         return 0;
      } else if (av1 == null) {
         return -1;
      } else if (av2 == null) {
         return 1;
      }
      return compareAnnotationValueValue(av1.getValue(), av2.getValue());
   }

   private static int compareAnnotationValueValue(@Nullable Object val1, @Nullable Object val2) {
      if (val1 == val2) {
         return 0;
      } else if (val1 == null) {
         return -1;
      } else if (val2 == null) {
         return 1;
      }
      // Can't use deepEquals() to compare val1 and val2, because they might have mismatched
      // AnnotationValue vs. CheckerFrameworkAnnotationValue, and AnnotationValue doesn't override
      // equals().  So, write my own version of deepEquals().
      if ((val1 instanceof List<?>) && (val2 instanceof List<?>)) {
         List<?> list1 = (List<?>) val1;
         List<?> list2 = (List<?>) val2;
         if (list1.size() != list2.size()) {
            return list1.size() - list2.size();
         }
         // Don't compare setwise, because order can matter. These mean different things:
         //   @LTLengthOf(value={"a1","a2"}, offest={"0", "1"})
         //   @LTLengthOf(value={"a2","a1"}, offest={"0", "1"})
         for (int i = 0; i < list1.size(); i++) {
            Object v1 = list1.get(i);
            Object v2 = list2.get(i);
            int result = compareAnnotationValueValue(v1, v2);
            if (result != 0) {
               return result;
            }
         }
         return 0;
      } else if ((val1 instanceof AnnotationMirror) && (val2 instanceof AnnotationMirror)) {
         return compareAnnotationMirrors((AnnotationMirror) val1, (AnnotationMirror) val2);
      } else if ((val1 instanceof AnnotationValue) && (val2 instanceof AnnotationValue)) {
         // This case occurs because of the recursive call when comparing arrays of
         // annotation values.
         return compareAnnotationValue((AnnotationValue) val1, (AnnotationValue) val2);
      }

      if ((val1 instanceof Type.ClassType) && (val2 instanceof Type.ClassType)) {
         // Type.ClassType does not override equals
         if (TypesUtils.areSameDeclaredTypes((Type.ClassType) val1, (Type.ClassType) val2)) {
            return 0;
         }
      }
      if (Objects.equals(val1, val2)) {
         return 0;
      }
      int result = val1.toString().compareTo(val2.toString());
      if (result == 0) {
         result = -1;
      }
      return result;
   }

   public static <V> Map<AnnotationMirror, V> createAnnotationMap() {
      return new TreeMap<>(AnnotationUtils::compareAnnotationMirrors);
   }

   public static SortedSet<AnnotationMirror> createAnnotationSet() {
      return new TreeSet<>(AnnotationUtils::compareAnnotationMirrors);
   }

   public static boolean hasInheritedMeta(AnnotationMirror anno) {
      return anno.getAnnotationType().asElement().getAnnotation(Inherited.class) != null;
   }

   public static EnumSet<ElementKind> getElementKindsForTarget(@Nullable Target target) {
      if (target == null) {
         // A missing @Target implies that the annotation can be written everywhere.
         return EnumSet.allOf(ElementKind.class);
      }
      EnumSet<ElementKind> eleKinds = EnumSet.noneOf(ElementKind.class);
      for (ElementType elementType : target.value()) {
         eleKinds.addAll(getElementKindsForElementType(elementType));
      }
      return eleKinds;
   }

   public static EnumSet<ElementKind> getElementKindsForElementType(ElementType elementType) {
      switch (elementType) {
         case TYPE:
            return EnumSet.copyOf(ElementUtils.classElementKinds());
         case FIELD:
            return EnumSet.of(ElementKind.FIELD, ElementKind.ENUM_CONSTANT);
         case METHOD:
            return EnumSet.of(ElementKind.METHOD);
         case PARAMETER:
            return EnumSet.of(ElementKind.PARAMETER);
         case CONSTRUCTOR:
            return EnumSet.of(ElementKind.CONSTRUCTOR);
         case LOCAL_VARIABLE:
            return EnumSet.of(
               ElementKind.LOCAL_VARIABLE,
               ElementKind.RESOURCE_VARIABLE,
               ElementKind.EXCEPTION_PARAMETER);
         case ANNOTATION_TYPE:
            return EnumSet.of(ElementKind.ANNOTATION_TYPE);
         case PACKAGE:
            return EnumSet.of(ElementKind.PACKAGE);
         case TYPE_PARAMETER:
            return EnumSet.of(ElementKind.TYPE_PARAMETER);
         case TYPE_USE:
            return EnumSet.noneOf(ElementKind.class);
         default:
            // TODO: Use MODULE enum constants directly instead of looking them up by name.
            // (Java 11)
            if (elementType.name().equals("MODULE")) {
               return EnumSet.of(ElementKind.valueOf("MODULE"));
            }
            if (elementType.name().equals("RECORD_COMPONENT")) {
               return EnumSet.of(ElementKind.valueOf("RECORD_COMPONENT"));
            }
            throw new RuntimeException("Unrecognized ElementType: " + elementType);
      }
   }

   public static Map<? extends ExecutableElement, ? extends AnnotationValue>
      getElementValuesWithDefaults(AnnotationMirror ad) {
      Map<ExecutableElement, AnnotationValue> valMap = new HashMap<>();
      if (ad.getElementValues() != null) {
         valMap.putAll(ad.getElementValues());
      }
      for (ExecutableElement meth :
         ElementFilter.methodsIn(ad.getAnnotationType().asElement().getEnclosedElements())) {
         AnnotationValue defaultValue = meth.getDefaultValue();
         if (defaultValue != null) {
            valMap.putIfAbsent(meth, defaultValue);
         }
      }
      return valMap;
   }

   public static boolean sameElementValues(AnnotationMirror am1, AnnotationMirror am2) {
      if (am1 == am2) {
         return true;
      }

      Map<? extends ExecutableElement, ? extends AnnotationValue> vals1 = am1.getElementValues();
      Map<? extends ExecutableElement, ? extends AnnotationValue> vals2 = am2.getElementValues();
      for (ExecutableElement meth :
         ElementFilter.methodsIn(
            am1.getAnnotationType().asElement().getEnclosedElements())) {
         AnnotationValue aval1 = vals1.get(meth);
         AnnotationValue aval2 = vals2.get(meth);
         if (aval1 == null) {
            aval1 = meth.getDefaultValue();
         }
         if (aval2 == null) {
            aval2 = meth.getDefaultValue();
         }
         if (!sameAnnotationValue(aval1, aval2)) {
            return false;
         }
      }
      return true;
   }

   public static boolean sameAnnotationValue(AnnotationValue av1, AnnotationValue av2) {
      return compareAnnotationValue(av1, av2) == 0;
   }

   public static boolean hasElementValue(AnnotationMirror anno, CharSequence elementName) {
      Map<? extends ExecutableElement, ? extends AnnotationValue> valmap =
         anno.getElementValues();
      for (ExecutableElement elem : valmap.keySet()) {
         if (elem.getSimpleName().contentEquals(elementName)) {
            return true;
         }
      }
      return false;
   }

   public static <T> T getElementValue(
      AnnotationMirror anno,
      CharSequence elementName,
      Class<T> expectedType,
      boolean useDefaults) {
      Map<? extends ExecutableElement, ? extends AnnotationValue> valmap;
      if (useDefaults) {
         valmap = getElementValuesWithDefaults(anno);
      } else {
         valmap = anno.getElementValues();
      }
      for (ExecutableElement elem : valmap.keySet()) {
         if (elem.getSimpleName().contentEquals(elementName)) {
            AnnotationValue val = valmap.get(elem);
            return expectedType.cast(val.getValue());
         }
      }
      throw new NoSuchElementException(
         String.format(
            "No element with name \'%s\' in annotation %s; useDefaults=%s, valmap.keySet()=%s",
            elementName, anno, useDefaults, valmap.keySet()));
   }

   @SuppressWarnings("serial")
   private static class NoSuchElementException extends RuntimeException {
      /**
       * Constructs a new NoSuchElementException.
       *
       * @param message the detail message
       */
      public NoSuchElementException(String message) {
         super(message);
      }
   }

   public static <T> @Nullable T getElementValueOrNull(
      AnnotationMirror anno,
      CharSequence elementName,
      Class<T> expectedType,
      boolean useDefaults) {
      // This implementation permits getElementValue a more detailed error message than if
      // getElementValue called getElementValueOrNull and threw an error if the result was null.
      try {
         return getElementValue(anno, elementName, expectedType, useDefaults);
      } catch (NoSuchElementException e) {
         return null;
      }
   }

   public static <T extends Enum<T>> T getElementValueEnum(
      AnnotationMirror anno,
      CharSequence elementName,
      Class<T> expectedType,
      boolean useDefaults) {
      Symbol.VarSymbol vs = getElementValue(anno, elementName, Symbol.VarSymbol.class, useDefaults);
      T value = Enum.valueOf(expectedType, vs.getSimpleName().toString());
      return value;
   }

   public static <T> List<T> getElementValueArray(
      AnnotationMirror anno,
      CharSequence elementName,
      Class<T> expectedType,
      boolean useDefaults) {
      @SuppressWarnings("unchecked")
      List<AnnotationValue> la = getElementValue(anno, elementName, List.class, useDefaults);
      List<T> result = new ArrayList<>(la.size());
      for (AnnotationValue a : la) {
         result.add(expectedType.cast(a.getValue()));
      }
      return result;
   }

   public static <T extends Enum<T>> List<T> getElementValueEnumArray(
      AnnotationMirror anno,
      CharSequence elementName,
      Class<T> expectedType,
      boolean useDefaults) {
      @SuppressWarnings("unchecked")
      List<AnnotationValue> la = getElementValue(anno, elementName, List.class, useDefaults);
      List<T> result = new ArrayList<>(la.size());
      for (AnnotationValue a : la) {
         T value = Enum.valueOf(expectedType, a.getValue().toString());
         result.add(value);
      }
      return result;
   }

   @SuppressWarnings("signature") // https://tinyurl.com/cfissue/658 for getQualifiedName
   public static @DotSeparatedIdentifiers Name getElementValueClassName(
      AnnotationMirror anno, CharSequence elementName, boolean useDefaults) {
      Type.ClassType ct = getElementValue(anno, elementName, Type.ClassType.class, useDefaults);
      // TODO:  Is it a problem that this returns the type parameters too?  Should I cut them off?
      return ct.asElement().getQualifiedName();
   }

   public static List<Name> getElementValueClassNames(
      AnnotationMirror anno, CharSequence annoElement, boolean useDefaults) {
      List<Type.ClassType> la =
         getElementValueArray(anno, annoElement, Type.ClassType.class, useDefaults);
      List<Name> names = new ArrayList<>();
      for (Type.ClassType classType : la) {
         names.add(classType.asElement().getQualifiedName());
      }
      return names;
   }

   public static <T> void updateMappingToImmutableSet(
      Map<T, Set<AnnotationMirror>> map, T key, Set<AnnotationMirror> newQual) {

      Set<AnnotationMirror> result = AnnotationUtils.createAnnotationSet();
      // TODO: if T is also an AnnotationMirror, should we use areSame?
      if (!map.containsKey(key)) {
         result.addAll(newQual);
      } else {
         result.addAll(map.get(key));
         result.addAll(newQual);
      }
      map.put(key, Collections.unmodifiableSet(result));
   }

   public static Set<AnnotationMirror> getExplicitAnnotationsOnConstructorResult(
      MethodTree constructorDeclaration) {
      Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
      ModifiersTree modifiersTree = constructorDeclaration.getModifiers();
      if (modifiersTree != null) {
         List<? extends AnnotationTree> annotationTrees = modifiersTree.getAnnotations();
         annotationSet.addAll(SourceTreeUtils.annotationsFromTypeAnnotationTrees(annotationTrees));
      }
      return annotationSet;
   }

   public static boolean isDeclarationAnnotation(AnnotationMirror anno) {
      TypeElement elem = (TypeElement) anno.getAnnotationType().asElement();
      Target t = elem.getAnnotation(Target.class);
      if (t == null) {
         return true;
      }

      for (ElementType elementType : t.value()) {
         if (elementType == ElementType.TYPE_USE) {
            return false;
         }
      }
      return true;
   }

   public static boolean hasTypeQualifierElementTypes(ElementType[] elements, Class<?> cls) {
      // True if the array contains TYPE_USE
      boolean hasTypeUse = false;
      // Non-null if the array contains an element other than TYPE_USE or TYPE_PARAMETER
      ElementType otherElementType = null;

      for (ElementType element : elements) {
         if (element == ElementType.TYPE_USE) {
            hasTypeUse = true;
         } else if (element != ElementType.TYPE_PARAMETER) {
            otherElementType = element;
         }
         if (hasTypeUse && otherElementType != null) {
            throw new RuntimeException(
               "@Target meta-annotation should not contain both TYPE_USE and "
                  + otherElementType
                  + ", for annotation "
                  + cls.getName());
         }
      }

      return hasTypeUse;
   }
}
