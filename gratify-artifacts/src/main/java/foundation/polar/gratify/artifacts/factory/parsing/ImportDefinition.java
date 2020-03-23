package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.ArtifactMetadataElement;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Representation of an import that has been processed during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#importProcessed(ImportDefinition)
 */
public class ImportDefinition implements ArtifactMetadataElement {
   private final String importedResource;

   @Nullable
   private final Resource[] actualResources;

   @Nullable
   private final Object source;

   /**
    * Create a new ImportDefinition.
    * @param importedResource the location of the imported resource
    */
   public ImportDefinition(String importedResource) {
      this(importedResource, null, null);
   }

   /**
    * Create a new ImportDefinition.
    * @param importedResource the location of the imported resource
    * @param source the source object (may be {@code null})
    */
   public ImportDefinition(String importedResource, @Nullable Object source) {
      this(importedResource, null, source);
   }

   /**
    * Create a new ImportDefinition.
    * @param importedResource the location of the imported resource
    * @param source the source object (may be {@code null})
    */
   public ImportDefinition(String importedResource, @Nullable Resource[] actualResources, @Nullable Object source) {
      AssertUtils.notNull(importedResource, "Imported resource must not be null");
      this.importedResource = importedResource;
      this.actualResources = actualResources;
      this.source = source;
   }

   /**
    * Return the location of the imported resource.
    */
   public final String getImportedResource() {
      return this.importedResource;
   }

   @Nullable
   public final Resource[] getActualResources() {
      return this.actualResources;
   }

   @Override
   @Nullable
   public final Object getSource() {
      return this.source;
   }
}
