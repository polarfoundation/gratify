package foundation.polar.gratify.utils.compiler;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import foundation.polar.gratify.lang.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.*;

public class ElementUtils {
   private ElementUtils() {
      throw new AssertionError("Class ElementUtils cannot be instantiated.");
   }
   public static TypeElement enclosingClass(final Element elem) {
      Element result = elem;
      while (result != null && !isClassElement(result)) {
         @Nullable Element encl = result.getEnclosingElement();
         result = encl;
      }
      return (TypeElement) result;
   }

   public static PackageElement enclosingPackage(final Element elem) {
      Element result = elem;
      while (result != null && result.getKind() != ElementKind.PACKAGE) {
         @Nullable Element encl = result.getEnclosingElement();
         result = encl;
      }
      return (PackageElement) result;
   }

   public static @Nullable PackageElement parentPackage(
      final PackageElement elem, final Elements e) {
      // The following might do the same thing:
      //   ((Symbol) elt).owner;
      // TODO: verify and see whether the change is worth it.
      String fqnstart = elem.getQualifiedName().toString();
      String fqn = fqnstart;
      if (fqn != null && !fqn.isEmpty() && fqn.contains(".")) {
         fqn = fqn.substring(0, fqn.lastIndexOf('.'));
         return e.getPackageElement(fqn);
      }
      return null;
   }

   public static boolean isStatic(Element element) {
      return element.getModifiers().contains(Modifier.STATIC);
   }

   public static boolean isFinal(Element element) {
      return element.getModifiers().contains(Modifier.FINAL);
   }

   public static boolean isEffectivelyFinal(Element element) {
      Symbol sym = (Symbol) element;
      if (sym.getEnclosingElement().getKind() == ElementKind.METHOD
         && (sym.getEnclosingElement().flags() & Flags.ABSTRACT) != 0) {
         return true;
      }
      return (sym.flags() & (Flags.FINAL | Flags.EFFECTIVELY_FINAL)) != 0;
   }

   public static TypeMirror getType(Element element) {
      if (element.getKind() == ElementKind.METHOD) {
         return ((ExecutableElement) element).getReturnType();
      } else if (element.getKind() == ElementKind.CONSTRUCTOR) {
         return enclosingClass(element).asType();
      } else {
         return element.asType();
      }
   }

   public static @Nullable Name getQualifiedClassName(Element element) {
      if (element.getKind() == ElementKind.PACKAGE) {
         PackageElement elem = (PackageElement) element;
         return elem.getQualifiedName();
      }

      TypeElement elem = enclosingClass(element);
      if (elem == null) {
         return null;
      }

      return elem.getQualifiedName();
   }

   public static String getVerboseName(Element elt) {
      Name n = getQualifiedClassName(elt);
      if (n == null) {
         return "Unexpected element: " + elt;
      }
      if (elt.getKind() == ElementKind.PACKAGE || isClassElement(elt)) {
         return n.toString();
      } else {
         return n + "." + elt;
      }
   }

   public static String getSimpleName(ExecutableElement element) {
      StringBuilder sb = new StringBuilder();

      // note: constructor simple name is <init>
      sb.append(element.getSimpleName());
      sb.append("(");
      for (Iterator<? extends VariableElement> i = element.getParameters().iterator();
           i.hasNext(); ) {
         sb.append(simpleTypeName(i.next().asType()));
         if (i.hasNext()) {
            sb.append(",");
         }
      }
      sb.append(")");

      return sb.toString();
   }

   private static String simpleTypeName(TypeMirror type) {
      switch (type.getKind()) {
         case ARRAY:
            return simpleTypeName(((ArrayType) type).getComponentType()) + "[]";
         case TYPEVAR:
            return ((TypeVariable) type).asElement().getSimpleName().toString();
         case DECLARED:
            return ((DeclaredType) type).asElement().getSimpleName().toString();
         default:
            if (type.getKind().isPrimitive()) {
               return type.toString();
            }
      }
      throw new RuntimeException(String.format("ElementUtils: unhandled type kind: %s, type: %s", type.getKind(), type));
   }

   public static boolean isObject(TypeElement element) {
      return element.getQualifiedName().contentEquals("java.lang.Object");
   }

   public static boolean isCompileTimeConstant(Element elt) {
      return elt != null
         && (elt.getKind() == ElementKind.FIELD
         || elt.getKind() == ElementKind.LOCAL_VARIABLE)
         && ((VariableElement) elt).getConstantValue() != null;
   }

   public static boolean isElementFromByteCode(Element elt) {
      if (elt == null) {
         return false;
      }

      if (elt instanceof Symbol.ClassSymbol) {
         Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
         if (null != clss.classfile) {
            // The class file could be a .java file
            return clss.classfile.getName().endsWith(".class");
         } else {
            return false;
         }
      }
      return isElementFromByteCodeHelper(elt.getEnclosingElement());
   }

   private static boolean isElementFromByteCodeHelper(Element elt) {
      if (elt == null) {
         return false;
      }
      if (elt instanceof Symbol.ClassSymbol) {
         Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
         if (null != clss.classfile) {
            // The class file could be a .java file
            return (clss.classfile.getName().endsWith(".class")
               || clss.classfile.getName().endsWith(".class)")
               || clss.classfile.getName().endsWith(".class)]"));
         } else {
            return false;
         }
      }
      return isElementFromByteCodeHelper(elt.getEnclosingElement());
   }

   public static @Nullable VariableElement findFieldInType(TypeElement type, String name) {
      for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
         if (field.getSimpleName().contentEquals(name)) {
            return field;
         }
      }
      return null;
   }

   public static Set<VariableElement> findFieldsInType(
      TypeElement type, Collection<String> names) {
      Set<VariableElement> results = new HashSet<>();
      for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
         if (names.contains(field.getSimpleName().toString())) {
            results.add(field);
         }
      }
      return results;
   }

   public static Set<VariableElement> findFieldsInTypeOrSuperType(
      TypeMirror type, Collection<String> names) {
      int origCardinality = names.size();
      Set<VariableElement> elements = new HashSet<>();
      findFieldsInTypeOrSuperType(type, names, elements);
      // Since names may contain duplicates, I don't trust the claim in the documentation about
      // cardinality.  (Does any code depend on the invariant, though?)
      if (origCardinality != names.size() + elements.size()) {
         throw new RuntimeException(
            String.format("Bad sizes: %d != %d + %d", origCardinality, names.size(), elements.size()));
      }
      return elements;
   }

   private static void findFieldsInTypeOrSuperType(
      TypeMirror type, Collection<String> notFound, Set<VariableElement> foundFields) {
      if (TypesUtils.isObject(type)) {
         return;
      }
      TypeElement elt = TypesUtils.getTypeElement(type);
      assert elt != null : "@AssumeAssertion(nullness): assumption";
      Set<VariableElement> fieldElts = findFieldsInType(elt, notFound);
      for (VariableElement field : new HashSet<VariableElement>(fieldElts)) {
         if (!field.getModifiers().contains(Modifier.PRIVATE)) {
            notFound.remove(field.getSimpleName().toString());
         } else {
            fieldElts.remove(field);
         }
      }
      foundFields.addAll(fieldElts);

      if (!notFound.isEmpty()) {
         findFieldsInTypeOrSuperType(elt.getSuperclass(), notFound, foundFields);
      }
   }

   public static boolean isError(Element element) {
      return element.getClass().getName().equals("com.sun.tools.javac.comp.Resolve$SymbolNotFoundError"); // interned
   }

   public static boolean hasReceiver(Element element) {
      return (element.getKind().isField()
         || element.getKind() == ElementKind.METHOD
         || element.getKind() == ElementKind.CONSTRUCTOR)
         && !ElementUtils.isStatic(element);
   }

   public static List<TypeElement> getSuperTypes(TypeElement type, Elements elements) {

      List<TypeElement> superelems = new ArrayList<>();
      if (type == null) {
         return superelems;
      }

      // Set up a stack containing type, which is our starting point.
      Deque<TypeElement> stack = new ArrayDeque<>();
      stack.push(type);

      while (!stack.isEmpty()) {
         TypeElement current = stack.pop();

         // For each direct supertype of the current type element, if it
         // hasn't already been visited, push it onto the stack and
         // add it to our superelems set.
         TypeMirror supertypecls;
         try {
            supertypecls = current.getSuperclass();
         } catch (com.sun.tools.javac.code.Symbol.CompletionFailure cf) {
            // Looking up a supertype failed. This sometimes happens
            // when transitive dependencies are not on the classpath.
            // As javac didn't complain, let's also not complain.
            supertypecls = null;
         }

         if (supertypecls != null && supertypecls.getKind() != TypeKind.NONE) {
            TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
            if (!superelems.contains(supercls)) {
               stack.push(supercls);
               superelems.add(supercls);
            }
         }

         for (TypeMirror supertypeitf : current.getInterfaces()) {
            TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
            if (!superelems.contains(superitf)) {
               stack.push(superitf);
               superelems.add(superitf);
            }
         }
      }

      // Include java.lang.Object as implicit superclass for all classes and interfaces.
      TypeElement jlobject = elements.getTypeElement("java.lang.Object");
      if (!superelems.contains(jlobject)) {
         superelems.add(jlobject);
      }

      return Collections.unmodifiableList(superelems);
   }

   public static List<VariableElement> getAllFieldsIn(TypeElement type, Elements elements) {
      List<VariableElement> fields = new ArrayList<>();
      fields.addAll(ElementFilter.fieldsIn(type.getEnclosedElements()));
      List<TypeElement> alltypes = getSuperTypes(type, elements);
      for (TypeElement atype : alltypes) {
         fields.addAll(ElementFilter.fieldsIn(atype.getEnclosedElements()));
      }
      return Collections.unmodifiableList(fields);
   }

   public static List<ExecutableElement> getAllMethodsIn(TypeElement type, Elements elements) {
      List<ExecutableElement> meths = new ArrayList<>();
      meths.addAll(ElementFilter.methodsIn(type.getEnclosedElements()));

      List<TypeElement> alltypes = getSuperTypes(type, elements);
      for (TypeElement atype : alltypes) {
         meths.addAll(ElementFilter.methodsIn(atype.getEnclosedElements()));
      }
      return Collections.unmodifiableList(meths);
   }

   public static List<TypeElement> getAllTypeElementsIn(TypeElement type) {
      List<TypeElement> types = new ArrayList<>();
      types.addAll(ElementFilter.typesIn(type.getEnclosedElements()));
      return types;
   }

   private static final Set<ElementKind> classElementKinds;

   static {
      classElementKinds = EnumSet.noneOf(ElementKind.class);
      for (ElementKind kind : ElementKind.values()) {
         if (kind.isClass() || kind.isInterface()) {
            classElementKinds.add(kind);
         }
      }
   }

   public static Set<ElementKind> classElementKinds() {
      return classElementKinds;
   }

   public static boolean isClassElement(Element element) {
      return classElementKinds().contains(element.getKind());
   }

   public static boolean isTypeDeclaration(Element elt) {
      return isClassElement(elt) || elt.getKind() == ElementKind.TYPE_PARAMETER;
   }

   public static boolean matchesElement(
      ExecutableElement method, String methodName, Class<?>... parameters) {
      if (!method.getSimpleName().contentEquals(methodName)) {
         return false;
      }
      if (method.getParameters().size() != parameters.length) {
         return false;
      } else {
         for (int i = 0; i < method.getParameters().size(); i++) {
            if (!method.getParameters()
               .get(i)
               .asType()
               .toString()
               .equals(parameters[i].getName())) {

               return false;
            }
         }
      }
      return true;
   }

   public static boolean isMethod(
      ExecutableElement questioned, ExecutableElement method, ProcessingEnvironment env) {
      TypeElement enclosing = (TypeElement) questioned.getEnclosingElement();
      return questioned.equals(method)
         || env.getElementUtils().overrides(questioned, method, enclosing);
   }
}
