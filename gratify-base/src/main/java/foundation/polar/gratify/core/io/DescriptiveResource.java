package foundation.polar.gratify.core.io;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple {@link Resource} implementation that holds a resource description
 * but does not point to an actually readable resource.
 *
 * <p>To be used as placeholder if a {@code Resource} argument is
 * expected by an API but not necessarily used for actual reading.
 *
 * @author Juergen Hoeller
 */
public class DescriptiveResource extends AbstractResource {
   private final String description;

   /**
    * Create a new DescriptiveResource.
    * @param description the resource description
    */
   public DescriptiveResource(@Nullable String description) {
      this.description = (description != null ? description : "");
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
         getDescription() + " cannot be opened because it does not point to a readable resource");
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   /**
    * This implementation compares the underlying description String.
    */
   @Override
   public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof DescriptiveResource &&
         ((DescriptiveResource) other).description.equals(this.description)));
   }

   /**
    * This implementation returns the hash code of the underlying description String.
    */
   @Override
   public int hashCode() {
      return this.description.hashCode();
   }
}
