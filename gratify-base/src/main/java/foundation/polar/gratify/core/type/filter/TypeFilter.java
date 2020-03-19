package foundation.polar.gratify.core.type.filter;

import foundation.polar.gratify.core.type.classreading.MetadataReader;
import foundation.polar.gratify.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

public interface TypeFilter {
   /**
    * Determine whether this filter matches for the class described by
    * the given metadata.
    * @param metadataReader the metadata reader for the target class
    * @param metadataReaderFactory a factory for obtaining metadata readers
    * for other classes (such as superclasses and interfaces)
    * @return whether this filter matches
    * @throws IOException in case of I/O failure when reading metadata
    */
   boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
      throws IOException;
}
