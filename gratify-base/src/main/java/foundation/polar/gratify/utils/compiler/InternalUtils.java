package foundation.polar.gratify.utils.compiler;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;

import javax.annotation.processing.ProcessingEnvironment;

public class InternalUtils {
   private InternalUtils() {
      throw new AssertionError("Class InternalUtils cannot be instantiated.");
   }

   public static Context getJavacContext(ProcessingEnvironment env) {
      return ((JavacProcessingEnvironment) env).getContext();
   }

   public static ClassLoader getClassLoaderForClass(Class<? extends Object> clazz) {
      ClassLoader classLoader = clazz.getClassLoader();
      return classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
   }

   public static int compareDiagnosticPosition(Tree tree1, Tree tree2) {
      JCDiagnostic.DiagnosticPosition pos1 = (JCDiagnostic.DiagnosticPosition) tree1;
      JCDiagnostic.DiagnosticPosition pos2 = (JCDiagnostic.DiagnosticPosition) tree2;

      int preferred = Integer.compare(pos1.getPreferredPosition(), pos2.getPreferredPosition());
      if (preferred != 0) {
         return preferred;
      }

      return Integer.compare(pos1.getStartPosition(), pos2.getStartPosition());
   }
}
