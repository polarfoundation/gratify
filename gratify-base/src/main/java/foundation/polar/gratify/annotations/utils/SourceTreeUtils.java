package foundation.polar.gratify.annotations.utils;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Pair;
import foundation.polar.gratify.annotations.checker.Pure;
import foundation.polar.gratify.lang.EnsuresNonNullIf;
import foundation.polar.gratify.lang.NonNull;
import foundation.polar.gratify.lang.Nullable;
import foundation.polar.gratify.utils.compiler.ElementUtils;
import foundation.polar.gratify.utils.compiler.TypeAnnotationUtils;
import foundation.polar.gratify.utils.compiler.TypesUtils;
import foundation.polar.gratify.utils.compiler.UserError;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;

public final class SourceTreeUtils {
   private SourceTreeUtils() {
      throw new RuntimeException("Class TreeUtils cannot be instantiated.");
   }

   public static boolean isConstructor(final MethodTree tree) {
      return tree.getName().contentEquals("<init>");
   }

   public static boolean isSuperConstructorCall(MethodInvocationTree tree) {
      return isNamedMethodCall("super", tree);
   }

   public static boolean isThisConstructorCall(MethodInvocationTree tree) {
      return isNamedMethodCall("this", tree);
   }

   private static boolean isNamedMethodCall(String name, MethodInvocationTree tree) {
      return getMethodName(tree.getMethodSelect()).equals(name);
   }

   public static boolean isSelfAccess(final ExpressionTree tree) {
      ExpressionTree tr = SourceTreeUtils.withoutParens(tree);
      // If method invocation check the method select
      if (tr.getKind() == Tree.Kind.ARRAY_ACCESS) {
         return false;
      }

      if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
         tr = ((MethodInvocationTree) tree).getMethodSelect();
      }
      tr = SourceTreeUtils.withoutParens(tr);
      if (tr.getKind() == Tree.Kind.TYPE_CAST) {
         tr = ((TypeCastTree) tr).getExpression();
      }
      tr = SourceTreeUtils.withoutParens(tr);

      if (tr.getKind() == Tree.Kind.IDENTIFIER) {
         return true;
      }

      if (tr.getKind() == Tree.Kind.MEMBER_SELECT) {
         tr = ((MemberSelectTree) tr).getExpression();
         if (tr.getKind() == Tree.Kind.IDENTIFIER) {
            Name ident = ((IdentifierTree) tr).getName();
            return ident.contentEquals("this") || ident.contentEquals("super");
         }
      }

      return false;
   }

   public static @Nullable Tree enclosingOfKind(final TreePath path, final Tree.Kind kind) {
      return enclosingOfKind(path, EnumSet.of(kind));
   }

   public static @Nullable Tree enclosingOfKind(final TreePath path, final Set<Tree.Kind> kinds) {
      TreePath p = path;
      while (p != null) {
         Tree leaf = p.getLeaf();
         assert leaf != null; /*nninvariant*/
         if (kinds.contains(leaf.getKind())) {
            return leaf;
         }
         p = p.getParentPath();
      }
      return null;
   }

   public static @Nullable TreePath pathTillClass(final TreePath path) {
      return pathTillOfKind(path, classTreeKinds());
   }

   public static @Nullable TreePath pathTillOfKind(final TreePath path, final Tree.Kind kind) {
      return pathTillOfKind(path, EnumSet.of(kind));
   }

   public static @Nullable TreePath pathTillOfKind(
      final TreePath path, final Set<Tree.Kind> kinds) {
      TreePath p = path;

      while (p != null) {
         Tree leaf = p.getLeaf();
         assert leaf != null; /*nninvariant*/
         if (kinds.contains(leaf.getKind())) {
            return p;
         }
         p = p.getParentPath();
      }

      return null;
   }

   public static <T extends Tree> @Nullable T enclosingOfClass(
      final TreePath path, final Class<T> treeClass) {
      TreePath p = path;

      while (p != null) {
         Tree leaf = p.getLeaf();
         if (treeClass.isInstance(leaf)) {
            return treeClass.cast(leaf);
         }
         p = p.getParentPath();
      }

      return null;
   }

   public static @Nullable ClassTree enclosingClass(final TreePath path) {
      return (ClassTree) enclosingOfKind(path, classTreeKinds());
   }

   public static @Nullable VariableTree enclosingVariable(final TreePath path) {
      return (VariableTree) enclosingOfKind(path, Tree.Kind.VARIABLE);
   }

   public static @Nullable MethodTree enclosingMethod(final TreePath path) {
      return (MethodTree) enclosingOfKind(path, Tree.Kind.METHOD);
   }

   public static @Nullable Tree enclosingMethodOrLambda(final TreePath path) {
      return enclosingOfKind(path, EnumSet.of(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION));
   }

   public static @Nullable BlockTree enclosingTopLevelBlock(TreePath path) {
      TreePath parpath = path.getParentPath();
      while (parpath != null && !classTreeKinds.contains(parpath.getLeaf().getKind())) {
         path = parpath;
         parpath = parpath.getParentPath();
      }
      if (path.getLeaf().getKind() == Tree.Kind.BLOCK) {
         return (BlockTree) path.getLeaf();
      }
      return null;
   }

   public static ExpressionTree withoutParens(final ExpressionTree tree) {
      ExpressionTree t = tree;
      while (t.getKind() == Tree.Kind.PARENTHESIZED) {
         t = ((ParenthesizedTree) t).getExpression();
      }
      return t;
   }

   public static Pair<Tree, Tree> enclosingNonParen(final TreePath path) {
      TreePath parentPath = path.getParentPath();
      Tree enclosing = parentPath.getLeaf();
      Tree enclosingChild = path.getLeaf();
      while (enclosing.getKind() == Tree.Kind.PARENTHESIZED) {
         parentPath = parentPath.getParentPath();
         enclosingChild = enclosing;
         enclosing = parentPath.getLeaf();
      }
      return Pair.of(enclosing, enclosingChild);
   }

   public static @Nullable Tree getAssignmentContext(final TreePath treePath) {
      TreePath parentPath = treePath.getParentPath();

      if (parentPath == null) {
         return null;
      }

      Tree parent = parentPath.getLeaf();
      switch (parent.getKind()) {
         case PARENTHESIZED:
            return getAssignmentContext(parentPath);
         case CONDITIONAL_EXPRESSION:
            ConditionalExpressionTree cet = (ConditionalExpressionTree) parent;
            if (cet.getCondition() == treePath.getLeaf()) {
               // The assignment context for the condition is simply boolean.
               // No point in going on.
               return null;
            }
            // Otherwise use the context of the ConditionalExpressionTree.
            return getAssignmentContext(parentPath);
         case ASSIGNMENT:
         case METHOD_INVOCATION:
         case NEW_ARRAY:
         case NEW_CLASS:
         case RETURN:
         case VARIABLE:
            return parent;
         default:
            // 11 Tree.Kinds are CompoundAssignmentTrees,
            // so use instanceof rather than listing all 11.
            if (parent instanceof CompoundAssignmentTree) {
               return parent;
            }
            return null;
      }
   }

   @Pure
   public static @Nullable Element elementFromTree(Tree tree) {
      if (tree == null) {
         throw new RuntimeException("InternalUtils.symbol: tree is null");
      }

      if (!(tree instanceof JCTree)) {
         throw new RuntimeException("InternalUtils.symbol: tree is not a valid Javac tree");
      }

      if (isExpressionTree(tree)) {
         tree = withoutParens((ExpressionTree) tree);
      }

      switch (tree.getKind()) {
         // symbol() only works on MethodSelects, so we need to get it manually
         // for method invocations.
         case METHOD_INVOCATION:
            return TreeInfo.symbol(((JCTree.JCMethodInvocation) tree).getMethodSelect());

         case ASSIGNMENT:
            return TreeInfo.symbol((JCTree) ((AssignmentTree) tree).getVariable());

         case ARRAY_ACCESS:
            return elementFromTree(((ArrayAccessTree) tree).getExpression());

         case NEW_CLASS:
            return ((JCTree.JCNewClass) tree).constructor;

         case MEMBER_REFERENCE:
            // TreeInfo.symbol, which is used in the default case, didn't handle
            // member references until JDK8u20. So handle it here.
            return ((JCTree.JCMemberReference) tree).sym;

         default:
            if (isTypeDeclaration(tree)
               || tree.getKind() == Tree.Kind.VARIABLE
               || tree.getKind() == Tree.Kind.METHOD) {
               return TreeInfo.symbolFor((JCTree) tree);
            }
            return TreeInfo.symbol((JCTree) tree);
      }
   }

   public static TypeElement elementFromDeclaration(ClassTree node) {
      TypeElement elt = (TypeElement) SourceTreeUtils.elementFromTree(node);
      assert elt != null : "@AssumeAssertion(nullness): tree kind";
      return elt;
   }

   public static ExecutableElement elementFromDeclaration(MethodTree node) {
      ExecutableElement elt = (ExecutableElement) SourceTreeUtils.elementFromTree(node);
      assert elt != null : "@AssumeAssertion(nullness): tree kind";
      return elt;
   }

   public static VariableElement elementFromDeclaration(VariableTree node) {
      VariableElement elt = (VariableElement) SourceTreeUtils.elementFromTree(node);
      assert elt != null : "@AssumeAssertion(nullness): tree kind";
      return elt;
   }

   @Pure
   public static @Nullable Element elementFromUse(ExpressionTree node) {
      return SourceTreeUtils.elementFromTree(node);
   }

   public static @Nullable ExecutableElement elementFromUse(MethodInvocationTree node) {
      Element el = SourceTreeUtils.elementFromTree(node);
      if (el instanceof ExecutableElement) {
         return (ExecutableElement) el;
      } else {
         return null;
      }
   }

   public static @Nullable ExecutableElement elementFromUse(NewClassTree node) {
      Element el = SourceTreeUtils.elementFromTree(node);
      if (el instanceof ExecutableElement) {
         return (ExecutableElement) el;
      } else {
         return null;
      }
   }

   public static ExecutableElement constructor(NewClassTree tree) {

      if (!(tree instanceof JCTree.JCNewClass)) {
         throw new RuntimeException("InternalUtils.constructor: not a javac internal tree");
      }

      JCTree.JCNewClass newClassTree = (JCTree.JCNewClass) tree;

      if (tree.getClassBody() != null) {
         // anonymous constructor bodies should contain exactly one statement
         // in the form:
         //    super(arg1, ...)
         // or
         //    o.super(arg1, ...)
         //
         // which is a method invocation (!) to the actual constructor

         // the method call is guaranteed to return nonnull
         JCTree.JCMethodDecl anonConstructor =
            (JCTree.JCMethodDecl) TreeInfo.declarationFor(newClassTree.constructor, newClassTree);
         assert anonConstructor != null;
         assert anonConstructor.body.stats.size() == 1;
         JCTree.JCExpressionStatement stmt = (JCTree.JCExpressionStatement) anonConstructor.body.stats.head;
         JCTree.JCMethodInvocation superInvok = (JCTree.JCMethodInvocation) stmt.expr;
         return (ExecutableElement) TreeInfo.symbol(superInvok.meth);
      } else {
         Element e = newClassTree.constructor;
         return (ExecutableElement) e;
      }
   }

   @EnsuresNonNullIf(result = true, expression = "elementFromUse(#1)")
   @Pure
   public static boolean isUseOfElement(ExpressionTree node) {
      ExpressionTree realnode = SourceTreeUtils.withoutParens(node);
      switch (realnode.getKind()) {
         case IDENTIFIER:
         case MEMBER_SELECT:
         case METHOD_INVOCATION:
         case NEW_CLASS:
            assert elementFromUse(node) != null : "@AssumeAssertion(nullness): inspection";
            return true;
         default:
            return false;
      }
   }

   public static Name methodName(MethodInvocationTree node) {
      ExpressionTree expr = node.getMethodSelect();
      if (expr.getKind() == Tree.Kind.IDENTIFIER) {
         return ((IdentifierTree) expr).getName();
      } else if (expr.getKind() == Tree.Kind.MEMBER_SELECT) {
         return ((MemberSelectTree) expr).getIdentifier();
      }
      throw new RuntimeException("TreeUtils.methodName: cannot be here: " + node);
   }

   public static boolean containsThisConstructorInvocation(MethodTree node) {
      if (!SourceTreeUtils.isConstructor(node) || node.getBody().getStatements().isEmpty()) {
         return false;
      }

      StatementTree st = node.getBody().getStatements().get(0);
      if (!(st instanceof ExpressionStatementTree)
         || !(((ExpressionStatementTree) st).getExpression()
         instanceof MethodInvocationTree)) {
         return false;
      }

      MethodInvocationTree invocation =
         (MethodInvocationTree) ((ExpressionStatementTree) st).getExpression();

      return "this".contentEquals(SourceTreeUtils.methodName(invocation));
   }

   public static Tree firstStatement(Tree tree) {
      Tree first;
      if (tree.getKind() == Tree.Kind.BLOCK) {
         BlockTree block = (BlockTree) tree;
         if (block.getStatements().isEmpty()) {
            first = block;
         } else {
            first = block.getStatements().iterator().next();
         }
      } else {
         first = tree;
      }
      return first;
   }

   public static boolean hasExplicitConstructor(ClassTree node) {
      TypeElement elem = SourceTreeUtils.elementFromDeclaration(node);

      for (ExecutableElement ee : ElementFilter.constructorsIn(elem.getEnclosedElements())) {
         Symbol.MethodSymbol ms = (Symbol.MethodSymbol) ee;
         long mod = ms.flags();

         if ((mod & Flags.SYNTHETIC) == 0) {
            return true;
         }
      }
      return false;
   }

   public static boolean isDiamondTree(Tree tree) {
      switch (tree.getKind()) {
         case ANNOTATED_TYPE:
            return isDiamondTree(((AnnotatedTypeTree) tree).getUnderlyingType());
         case PARAMETERIZED_TYPE:
            return ((ParameterizedTypeTree) tree).getTypeArguments().isEmpty();
         case NEW_CLASS:
            return isDiamondTree(((NewClassTree) tree).getIdentifier());
         default:
            return false;
      }
   }

   public static boolean isStringConcatenation(Tree tree) {
      return (tree.getKind() == Tree.Kind.PLUS && TypesUtils.isString(SourceTreeUtils.typeOf(tree)));
   }

   public static boolean isStringCompoundConcatenation(CompoundAssignmentTree tree) {
      return (tree.getKind() == Tree.Kind.PLUS_ASSIGNMENT
         && TypesUtils.isString(SourceTreeUtils.typeOf(tree)));
   }

   public static boolean isCompileTimeString(ExpressionTree node) {
      ExpressionTree tree = SourceTreeUtils.withoutParens(node);
      if (tree instanceof LiteralTree) {
         return true;
      }

      if (SourceTreeUtils.isUseOfElement(tree)) {
         Element elt = SourceTreeUtils.elementFromUse(tree);
         return ElementUtils.isCompileTimeConstant(elt);
      } else if (SourceTreeUtils.isStringConcatenation(tree)) {
         BinaryTree binOp = (BinaryTree) tree;
         return isCompileTimeString(binOp.getLeftOperand())
            && isCompileTimeString(binOp.getRightOperand());
      } else {
         return false;
      }
   }

   /** Returns the receiver tree of a field access or a method invocation. */
   public static @Nullable ExpressionTree getReceiverTree(ExpressionTree expression) {
      ExpressionTree receiver;
      switch (expression.getKind()) {
         case METHOD_INVOCATION:
            // Trying to handle receiver calls to trees of the form
            //     ((m).getArray())
            // returns the type of 'm' in this case
            receiver = ((MethodInvocationTree) expression).getMethodSelect();

            if (receiver.getKind() == Tree.Kind.MEMBER_SELECT) {
               receiver = ((MemberSelectTree) receiver).getExpression();
            } else {
               // It's a method call "m(foo)" without an explicit receiver
               return null;
            }
            break;
         case NEW_CLASS:
            receiver = ((NewClassTree) expression).getEnclosingExpression();
            break;
         case ARRAY_ACCESS:
            receiver = ((ArrayAccessTree) expression).getExpression();
            break;
         case MEMBER_SELECT:
            receiver = ((MemberSelectTree) expression).getExpression();
            // Avoid int.class
            if (receiver instanceof PrimitiveTypeTree) {
               return null;
            }
            break;
         case IDENTIFIER:
            // It's a field access on implicit this or a local variable/parameter.
            return null;
         default:
            return null;
      }
      if (receiver == null) {
         return null;
      }

      return SourceTreeUtils.withoutParens(receiver);
   }

   // TODO: What about anonymous classes?
   // Adding Tree.Kind.NEW_CLASS here doesn't work, because then a
   // tree gets cast to ClassTree when it is actually a NewClassTree,
   // for example in enclosingClass above.
   /** The set of kinds that represent classes. */
   private static final Set<Tree.Kind> classTreeKinds;

   static {
      classTreeKinds = EnumSet.noneOf(Tree.Kind.class);
      for (Tree.Kind kind : Tree.Kind.values()) {
         if (kind.asInterface() == ClassTree.class) {
            classTreeKinds.add(kind);
         }
      }
   }

   public static Set<Tree.Kind> classTreeKinds() {
      return classTreeKinds;
   }

   public static boolean isClassTree(Tree tree) {
      return classTreeKinds().contains(tree.getKind());
   }

   private static final Set<Tree.Kind> typeTreeKinds =
      EnumSet.of(
         Tree.Kind.PRIMITIVE_TYPE,
         Tree.Kind.PARAMETERIZED_TYPE,
         Tree.Kind.TYPE_PARAMETER,
         Tree.Kind.ARRAY_TYPE,
         Tree.Kind.UNBOUNDED_WILDCARD,
         Tree.Kind.EXTENDS_WILDCARD,
         Tree.Kind.SUPER_WILDCARD,
         Tree.Kind.ANNOTATED_TYPE);

   public static Set<Tree.Kind> typeTreeKinds() {
      return typeTreeKinds;
   }

   public static boolean isTypeTree(Tree tree) {
      return typeTreeKinds().contains(tree.getKind());
   }

   public static boolean isMethodInvocation(
      Tree tree, ExecutableElement method, ProcessingEnvironment env) {
      if (!(tree instanceof MethodInvocationTree)) {
         return false;
      }
      MethodInvocationTree methInvok = (MethodInvocationTree) tree;
      ExecutableElement invoked = SourceTreeUtils.elementFromUse(methInvok);
      assert invoked != null : "@AssumeAssertion(nullness): assumption";
      return ElementUtils.isMethod(invoked, method, env);
   }

   public static boolean isMethodInvocation(
      Tree methodTree, List<ExecutableElement> methods, ProcessingEnvironment processingEnv) {
      if (!(methodTree instanceof MethodInvocationTree)) {
         return false;
      }
      for (ExecutableElement Method : methods) {
         if (isMethodInvocation(methodTree, Method, processingEnv)) {
            return true;
         }
      }
      return false;
   }

   public static ExecutableElement getMethod(
      String typeName, String methodName, int params, ProcessingEnvironment env) {
      List<ExecutableElement> methods = getMethods(typeName, methodName, params, env);
      if (methods.size() == 1) {
         return methods.get(0);
      }
      throw new RuntimeException(
         String.format("TreeUtils.getMethod(%s, %s, %d): expected 1 match, found %d",
         typeName, methodName, params, methods.size()));
   }

   public static List<ExecutableElement> getMethods(
      String typeName, String methodName, int params, ProcessingEnvironment env) {
      List<ExecutableElement> methods = new ArrayList<>(1);
      TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
      if (typeElt == null) {
         throw new UserError("Configuration problem! Could not load type: " + typeName);
      }
      for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
         if (exec.getSimpleName().contentEquals(methodName)
            && exec.getParameters().size() == params) {
            methods.add(exec);
         }
      }
      return methods;
   }

   public static ExecutableElement getMethod(
      String typeName, String methodName, ProcessingEnvironment env, String... paramTypes) {
      TypeElement typeElt = env.getElementUtils().getTypeElement(typeName);
      for (ExecutableElement exec : ElementFilter.methodsIn(typeElt.getEnclosedElements())) {
         if (exec.getSimpleName().contentEquals(methodName)
            && exec.getParameters().size() == paramTypes.length) {
            boolean typesMatch = true;
            List<? extends VariableElement> params = exec.getParameters();
            for (int i = 0; i < paramTypes.length; i++) {
               VariableElement ve = params.get(i);
               TypeMirror tm = TypeAnnotationUtils.unannotatedType(ve.asType());
               if (!tm.toString().equals(paramTypes[i])) {
                  typesMatch = false;
                  break;
               }
            }
            if (typesMatch) {
               return exec;
            }
         }
      }
      throw new RuntimeException(
         "TreeUtils.getMethod: found no match for "
            + typeName
            + "."
            + methodName
            + "("
            + Arrays.toString(paramTypes)
            + ")");
   }

   public static boolean isExplicitThisDereference(ExpressionTree tree) {
      if (tree.getKind() == Tree.Kind.IDENTIFIER
         && ((IdentifierTree) tree).getName().contentEquals("this")) {
         // Explicit this reference "this"
         return true;
      }

      if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
         return false;
      }

      MemberSelectTree memSelTree = (MemberSelectTree) tree;
      if (memSelTree.getIdentifier().contentEquals("this")) {
         // Outer this reference "C.this"
         return true;
      }
      return false;
   }

   public static boolean isClassLiteral(Tree tree) {
      if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
         return false;
      }
      return "class".equals(((MemberSelectTree) tree).getIdentifier().toString());
   }

   public static boolean isFieldAccess(Tree tree) {
      if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
         // explicit field access
         MemberSelectTree memberSelect = (MemberSelectTree) tree;
         assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
         Element el = SourceTreeUtils.elementFromUse(memberSelect);
         return el.getKind().isField();
      } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
         // implicit field access
         IdentifierTree ident = (IdentifierTree) tree;
         assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
         Element el = SourceTreeUtils.elementFromUse(ident);
         return el.getKind().isField()
            && !ident.getName().contentEquals("this")
            && !ident.getName().contentEquals("super");
      }
      return false;
   }

   public static String getFieldName(Tree tree) {
      assert isFieldAccess(tree);
      if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
         MemberSelectTree mtree = (MemberSelectTree) tree;
         return mtree.getIdentifier().toString();
      } else {
         IdentifierTree itree = (IdentifierTree) tree;
         return itree.getName().toString();
      }
   }

   public static boolean isMethodAccess(Tree tree) {
      if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
         // explicit method access
         MemberSelectTree memberSelect = (MemberSelectTree) tree;
         assert isUseOfElement(memberSelect) : "@AssumeAssertion(nullness): tree kind";
         Element el = SourceTreeUtils.elementFromUse(memberSelect);
         return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
      } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
         // implicit method access
         IdentifierTree ident = (IdentifierTree) tree;
         // The field "super" and "this" are also legal methods
         if (ident.getName().contentEquals("super") || ident.getName().contentEquals("this")) {
            return true;
         }
         assert isUseOfElement(ident) : "@AssumeAssertion(nullness): tree kind";
         Element el = SourceTreeUtils.elementFromUse(ident);
         return el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR;
      }
      return false;
   }

   public static String getMethodName(Tree tree) {
      assert isMethodAccess(tree);
      if (tree.getKind() == Tree.Kind.MEMBER_SELECT) {
         MemberSelectTree mtree = (MemberSelectTree) tree;
         return mtree.getIdentifier().toString();
      } else {
         IdentifierTree itree = (IdentifierTree) tree;
         return itree.getName().toString();
      }
   }

   public static boolean canHaveTypeAnnotation(Tree tree) {
      return ((JCTree) tree).type != null;
   }

   public static boolean isSpecificFieldAccess(Tree tree, VariableElement var) {
      if (tree instanceof MemberSelectTree) {
         MemberSelectTree memSel = (MemberSelectTree) tree;
         assert isUseOfElement(memSel) : "@AssumeAssertion(nullness): tree kind";
         Element field = SourceTreeUtils.elementFromUse(memSel);
         return field.equals(var);
      } else if (tree instanceof IdentifierTree) {
         IdentifierTree idTree = (IdentifierTree) tree;
         assert isUseOfElement(idTree) : "@AssumeAssertion(nullness): tree kind";
         Element field = SourceTreeUtils.elementFromUse(idTree);
         return field.equals(var);
      } else {
         return false;
      }
   }

   public static VariableElement getField(
      String typeName, String fieldName, ProcessingEnvironment env) {
      TypeElement mapElt = env.getElementUtils().getTypeElement(typeName);
      for (VariableElement var : ElementFilter.fieldsIn(mapElt.getEnclosedElements())) {
         if (var.getSimpleName().contentEquals(fieldName)) {
            return var;
         }
      }
      throw new RuntimeException("TreeUtils.getField: shouldn't be here");
   }

   public static boolean isExpressionTree(Tree tree) {
      // TODO: is there a nicer way than an instanceof?
      return tree instanceof ExpressionTree;
   }

   public static boolean isEnumSuper(MethodInvocationTree node) {
      ExecutableElement ex = SourceTreeUtils.elementFromUse(node);
      assert ex != null : "@AssumeAssertion(nullness): tree kind";
      Name name = ElementUtils.getQualifiedClassName(ex);
      assert name != null : "@AssumeAssertion(nullness): assumption";
      boolean correctClass = "java.lang.Enum".contentEquals(name);
      boolean correctMethod = "<init>".contentEquals(ex.getSimpleName());
      return correctClass && correctMethod;
   }

   public static boolean isTypeDeclaration(Tree node) {
      return isClassTree(node) || node.getKind() == Tree.Kind.TYPE_PARAMETER;
   }

   public static boolean isTreeInStaticScope(TreePath path) {
      MethodTree enclosingMethod = SourceTreeUtils.enclosingMethod(path);

      if (enclosingMethod != null) {
         return enclosingMethod.getModifiers().getFlags().contains(Modifier.STATIC);
      }
      // no enclosing method, check for static or initializer block
      BlockTree block = enclosingTopLevelBlock(path);
      if (block != null) {
         return block.isStatic();
      }

      // check if its in a variable initializer
      Tree t = enclosingVariable(path);
      if (t != null) {
         return ((VariableTree) t).getModifiers().getFlags().contains(Modifier.STATIC);
      }
      ClassTree classTree = enclosingClass(path);
      if (classTree != null) {
         return classTree.getModifiers().getFlags().contains(Modifier.STATIC);
      }
      return false;
   }

   public static boolean isArrayLengthAccess(Tree tree) {
      if (tree.getKind() == Tree.Kind.MEMBER_SELECT
         && isFieldAccess(tree)
         && getFieldName(tree).equals("length")) {
         ExpressionTree expressionTree = ((MemberSelectTree) tree).getExpression();
         if (SourceTreeUtils.typeOf(expressionTree).getKind() == TypeKind.ARRAY) {
            return true;
         }
      }
      return false;
   }

   public static boolean isAnonymousConstructor(final MethodTree method) {
      @Nullable Element e = elementFromTree(method);
      if (!(e instanceof Symbol)) {
         return false;
      }

      if ((((@NonNull Symbol) e).flags() & Flags.ANONCONSTR) != 0) {
         return true;
      }

      return false;
   }

   public static List<AnnotationMirror> annotationsFromTypeAnnotationTrees(
      List<? extends AnnotationTree> annoTreess) {
      List<AnnotationMirror> annotations = new ArrayList<>(annoTreess.size());
      for (AnnotationTree anno : annoTreess) {
         annotations.add(SourceTreeUtils.annotationFromAnnotationTree(anno));
      }
      return annotations;
   }

   public static AnnotationMirror annotationFromAnnotationTree(AnnotationTree tree) {
      return ((JCTree.JCAnnotation) tree).attribute;
   }

   public static List<? extends AnnotationMirror> annotationsFromTree(AnnotatedTypeTree tree) {
      return annotationsFromTypeAnnotationTrees(((JCTree.JCAnnotatedType) tree).annotations);
   }

   public static List<? extends AnnotationMirror> annotationsFromTree(TypeParameterTree tree) {
      return annotationsFromTypeAnnotationTrees(((JCTree.JCTypeParameter) tree).annotations);
   }

   public static List<? extends AnnotationMirror> annotationsFromArrayCreation(
      NewArrayTree tree, int level) {

      assert tree instanceof JCTree.JCNewArray;
      final JCTree.JCNewArray newArray = ((JCTree.JCNewArray) tree);

      if (level == -1) {
         return annotationsFromTypeAnnotationTrees(newArray.annotations);
      }

      if (newArray.dimAnnotations.length() > 0
         && (level >= 0)
         && (level < newArray.dimAnnotations.size())) {
         return annotationsFromTypeAnnotationTrees(newArray.dimAnnotations.get(level));
      }

      return Collections.emptyList();
   }

   public static boolean isLocalVariable(Tree tree) {
      if (tree.getKind() == Tree.Kind.VARIABLE) {
         return elementFromDeclaration((VariableTree) tree).getKind()
            == ElementKind.LOCAL_VARIABLE;
      } else if (tree.getKind() == Tree.Kind.IDENTIFIER) {
         ExpressionTree etree = (ExpressionTree) tree;
         assert isUseOfElement(etree) : "@AssumeAssertion(nullness): tree kind";
         return elementFromUse(etree).getKind() == ElementKind.LOCAL_VARIABLE;
      }
      return false;
   }

   public static TypeMirror typeOf(Tree tree) {
      return ((JCTree) tree).type;
   }

   public static Symbol findFunction(Tree tree, ProcessingEnvironment env) {
      Context ctx = ((JavacProcessingEnvironment) env).getContext();
      Types javacTypes = Types.instance(ctx);
      return javacTypes.findDescriptorSymbol(((Type) typeOf(tree)).asElement());
   }

   public static boolean isImplicitlyTypedLambda(Tree tree) {
      return tree.getKind() == Tree.Kind.LAMBDA_EXPRESSION
         && ((JCTree.JCLambda) tree).paramKind == JCTree.JCLambda.ParameterKind.IMPLICIT;
   }
}
