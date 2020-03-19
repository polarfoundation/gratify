package foundation.polar.gratify.core.type.filter;

import foundation.polar.gratify.core.type.ClassMetadata;
import foundation.polar.gratify.core.type.classreading.MetadataReader;
import foundation.polar.gratify.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

/**
 * Type filter that exposes a
 * {@link foundation.polar.gratify.core.type.ClassMetadata} object
 * to subclasses, for class testing purposes.
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 *
 * @see #match(foundation.polar.gratify.core.type.ClassMetadata)
 */
public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

   @Override
   public final boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
      throws IOException {
      return match(metadataReader.getClassMetadata());
   }

   /**
    * Determine a match based on the given ClassMetadata object.
    * @param metadata the ClassMetadata object
    * @return whether this filter matches on the specified type
    */
   protected abstract boolean match(ClassMetadata metadata);

}