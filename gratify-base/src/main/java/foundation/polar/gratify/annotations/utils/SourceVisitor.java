package foundation.polar.gratify.annotations.utils;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public abstract class SourceVisitor<R, P> extends TreePathScanner<R, P> {
   protected final Trees trees;
   protected final Elements elements;
   protected final Types types;
   protected CompilationUnitTree root;
   public final List<Tree> treesWithSuppressWarnings;
   private final boolean warnUnneededSuppressions;

   Tree lastVisited;

   public SourceVisitor(SourceChecker checker) {
      ProcessingEnvironment env = checker.getProcessingEnvironment();
      this.trees = Trees.instance(env);
      this.elements = env.getElementUtils();
      this.types = env.getTypeUtils();
      this.treesWithSuppressWarnings = new ArrayList<>();
      this.warnUnneededSuppressions = checker.hasOption("warnUnneededSuppressions");
   }

   public void setRoot(CompilationUnitTree root) {
      this.root = root;
   }

   public void visit(TreePath path) {
      lastVisited = path.getLeaf();
      this.scan(path, null);
   }

   @Override
   public R scan(Tree tree, P p) {
      lastVisited = tree;
      return super.scan(tree, p);
   }

   @Override
   public R visitClass(ClassTree classTree, P p) {
      storeSuppressWarningsAnno(classTree);
      return super.visitClass(classTree, p);
   }

   @Override
   public R visitVariable(VariableTree variableTree, P p) {
      storeSuppressWarningsAnno(variableTree);
      return super.visitVariable(variableTree, p);
   }

   @Override
   public R visitMethod(MethodTree node, P p) {
      storeSuppressWarningsAnno(node);
      return super.visitMethod(node, p);
   }

   private void storeSuppressWarningsAnno(Tree tree) {
      if (!warnUnneededSuppressions) {
         return;
      }
      Element elt = SourceTreeUtils.elementFromTree(tree);
      if (elt.getAnnotation(SuppressWarnings.class) != null) {
         treesWithSuppressWarnings.add(tree);
      }
   }
}
