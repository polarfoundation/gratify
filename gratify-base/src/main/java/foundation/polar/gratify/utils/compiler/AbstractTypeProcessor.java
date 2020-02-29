package foundation.polar.gratify.utils.compiler;

import com.sun.source.util.*;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.comp.CompileStates.CompileState;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractTypeProcessor extends AbstractProcessor {
   private final Set<Name> elements = new HashSet<>();
   private boolean hasInvokedTypeProcessingStart = false;
   private static boolean hasInvokedTypeProcessingOver = false;
   private final AttributionTaskListener listener = new AttributionTaskListener();

   protected AbstractTypeProcessor() {}

   @Override
   public synchronized void init(ProcessingEnvironment env) {
      super.init(env);
      JavacTask.instance(env).addTaskListener(listener);
      Context ctx = ((JavacProcessingEnvironment)processingEnv).getContext();
      JavaCompiler compiler = JavaCompiler.instance(ctx);
      compiler.shouldStopPolicyIfNoError = CompileState.max(compiler.shouldStopPolicyIfNoError, CompileState.FLOW);
      compiler.shouldStopPolicyIfError = CompileState.max(compiler.shouldStopPolicyIfError, CompileState.FLOW);
   }

   @Override
   public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement element : ElementFilter.typesIn(roundEnv.getRootElements())) {
         elements.add(element.getQualifiedName());
      }
      return false;
   }

   public void typeProcessingStart() {}
   public void typeProcessingOver() {}
   public abstract void typeProcess(TypeElement element, TreePath tree);

   private final class AttributionTaskListener implements TaskListener {
      @Override
      public void finished(TaskEvent event) {
         if (event.getKind() != TaskEvent.Kind.ANALYZE) {
            return;
         }
         if (!hasInvokedTypeProcessingStart) {
            typeProcessingStart();
            hasInvokedTypeProcessingStart = true;
         }

         if (event.getTypeElement() == null) {
            throw new RuntimeException("event task without a type element", new Throwable());
         }
         if (event.getCompilationUnit() == null) {
            throw new RuntimeException("event task without compilation unit", new Throwable());
         }
         if (!elements.remove(event.getTypeElement().getQualifiedName())) {
            return;
         }
         TypeElement element = event.getTypeElement();
         TreePath treePath = Trees.instance(processingEnv).getPath(element);
         typeProcess(element, treePath);

         if (!hasInvokedTypeProcessingOver) {
            typeProcessingOver();
            hasInvokedTypeProcessingOver = true;
         }
      }

      @Override
      public void started(TaskEvent event) {}
   }
}
