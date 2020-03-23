package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.core.io.AbstractResource;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Descriptive {@link foundation.polar.gratify.core.io.Resource} wrapper for
 * a {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition}.
 *
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.core.io.DescriptiveResource
 */
public class ArtifactDefinitionResource extends AbstractResource {

   private final ArtifactDefinition artifactDefinition;

   /**
    * Create a new ArtifactDefinitionResource.
    * @param artifactDefinition the ArtifactDefinition object to wrap
    */
   public ArtifactDefinitionResource(ArtifactDefinition artifactDefinition) {
      AssertUtils.notNull(artifactDefinition, "ArtifactDefinition must not be null");
      this.artifactDefinition = artifactDefinition;
   }

   /**
    * Return the wrapped ArtifactDefinition object.
    */
   public final ArtifactDefinition getArtifactDefinition() {
      return this.artifactDefinition;
   }

   @Override
   public boolean exists() {
      return false;
   }

   @Override
   public boolean isReadable() {
      return false;
   }

   @Override
   public InputStream getInputStream() throws IOException {
      throw new FileNotFoundException(
         "Resource cannot be opened because it points to " + getDescription());
   }

   @Override
   public String getDescription() {
      return "ArtifactDefinition defined in " + this.artifactDefinition.getResourceDescription();
   }

   /**
    * This implementation compares the underlying ArtifactDefinition.
    */
   @Override
   public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof ArtifactDefinitionResource &&
         ((ArtifactDefinitionResource) other).artifactDefinition.equals(this.artifactDefinition)));
   }

   /**
    * This implementation returns the hash code of the underlying ArtifactDefinition.
    */
   @Override
   public int hashCode() {
      return this.artifactDefinition.hashCode();
   }
}
