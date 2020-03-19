package foundation.polar.gratify.core.type.classreading;

import foundation.polar.gratify.core.NestedIOException;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.type.AnnotationMetadata;
import foundation.polar.gratify.core.type.ClassMetadata;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link MetadataReader} implementation based on an ASM
 * {@link org.objectweb.asm.ClassReader}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 */
public class SimpleMetadataReader implements MetadataReader {
   private static final int PARSING_OPTIONS = ClassReader.SKIP_DEBUG
      | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES;

   private final Resource resource;

   private final AnnotationMetadata annotationMetadata;

   SimpleMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
      SimpleAnnotationMetadataReadingVisitor visitor = new SimpleAnnotationMetadataReadingVisitor(classLoader);
      getClassReader(resource).accept(visitor, PARSING_OPTIONS);
      this.resource = resource;
      this.annotationMetadata = visitor.getMetadata();
   }

   private static ClassReader getClassReader(Resource resource) throws IOException {
      try (InputStream is = new BufferedInputStream(resource.getInputStream())) {
         try {
            return new ClassReader(is);
         }
         catch (IllegalArgumentException ex) {
            throw new NestedIOException("ASM ClassReader failed to parse class file - " +
               "probably due to a new Java class file version that isn't supported yet: " + resource, ex);
         }
      }
   }

   @Override
   public Resource getResource() {
      return this.resource;
   }

   @Override
   public ClassMetadata getClassMetadata() {
      return this.annotationMetadata;
   }

   @Override
   public AnnotationMetadata getAnnotationMetadata() {
      return this.annotationMetadata;
   }
}
