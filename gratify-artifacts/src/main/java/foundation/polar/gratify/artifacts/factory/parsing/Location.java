package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class that models an arbitrary location in a {@link Resource resource}.
 *
 * <p>Typically used to track the location of problematic or erroneous
 * metadata in XML configuration files. For example, a
 * {@link #getSource() source} location might be 'The bean defined on
 * line 76 of beans.properties has an invalid Class'; another source might
 * be the actual DOM Element from a parsed XML {@link org.w3c.dom.Document};
 * or the source object might simply be {@code null}.
 *
 * @author Rob Harrop
 */
public class Location {

   private final Resource resource;

   @Nullable
   private final Object source;

   /**
    * Create a new instance of the {@link Location} class.
    * @param resource the resource with which this location is associated
    */
   public Location(Resource resource) {
      this(resource, null);
   }

   /**
    * Create a new instance of the {@link Location} class.
    * @param resource the resource with which this location is associated
    * @param source the actual location within the associated resource
    * (may be {@code null})
    */
   public Location(Resource resource, @Nullable Object source) {
      AssertUtils.notNull(resource, "Resource must not be null");
      this.resource = resource;
      this.source = source;
   }

   /**
    * Get the resource with which this location is associated.
    */
   public Resource getResource() {
      return this.resource;
   }

   /**
    * Get the actual location within the associated {@link #getResource() resource}
    * (may be {@code null}).
    * <p>See the {@link Location class level javadoc for this class} for examples
    * of what the actual type of the returned object may be.
    */
   @Nullable
   public Object getSource() {
      return this.source;
   }
}

