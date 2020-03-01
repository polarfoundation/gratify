package foundation.polar.gratify.annotation;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Strategy API for extracting a value for an annotation attribute from a given
 * source object which is typically an {@link Annotation}, {@link Map}, or
 * {@link TypeMappedAnnotation}.
 *
 * @author Sam Brannen
 */
@FunctionalInterface
interface ValueExtractor {

   /**
    * Extract the annotation attribute represented by the supplied {@link Method}
    * from the supplied source {@link Object}.
    */
   @Nullable
   Object extract(Method attribute, @Nullable Object object);
}
