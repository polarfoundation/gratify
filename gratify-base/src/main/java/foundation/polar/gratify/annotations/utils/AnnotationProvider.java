package foundation.polar.gratify.annotations.utils;

import com.sun.source.tree.Tree;
import foundation.polar.gratify.lang.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;

public interface AnnotationProvider {
   @Nullable AnnotationMirror getDeclAnnotation(Element element, Class<? extends Annotation> annotation);
   @Nullable AnnotationMirror getAnnotationMirror(Tree tree, Class<? extends Annotation> target);
}
