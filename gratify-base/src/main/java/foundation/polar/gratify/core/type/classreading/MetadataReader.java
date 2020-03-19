package foundation.polar.gratify.core.type.classreading;

import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.type.AnnotationMetadata;
import foundation.polar.gratify.core.type.ClassMetadata;

/**
 * Simple facade for accessing class metadata,
 * as read by an ASM {@link org.objectweb.asm.ClassReader}.
 *
 * @author Juergen Hoeller
 */
public interface MetadataReader {
   /**
    * Return the resource reference for the class file.
    */
   Resource getResource();

   /**
    * Read basic class metadata for the underlying class.
    */
   ClassMetadata getClassMetadata();

   /**
    * Read full annotation metadata for the underlying class,
    * including metadata for annotated methods.
    */
   AnnotationMetadata getAnnotationMetadata();
}
