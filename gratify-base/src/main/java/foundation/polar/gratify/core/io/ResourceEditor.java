package foundation.polar.gratify.core.io;

import foundation.polar.gratify.env.PropertyResolver;
import foundation.polar.gratify.env.StandardEnvironment;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link Resource}
 * descriptors, to automatically convert {@code String} locations
 * e.g. {@code file:C:/myfile.txt} or {@code classpath:myfile.txt} to
 * {@code Resource} properties instead of using a {@code String} location property.
 *
 * <p>The path may contain {@code ${...}} placeholders, to be
 * resolved as {@link foundation.polar.gratify.env.Environment} properties:
 * e.g. {@code ${user.dir}}. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourceLoader} to do the heavy lifting,
 * by default using a {@link DefaultResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 *
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 */
public class ResourceEditor extends PropertyEditorSupport {
   private final ResourceLoader resourceLoader;

   @Nullable
   private PropertyResolver propertyResolver;

   private final boolean ignoreUnresolvablePlaceholders;

   /**
    * Create a new instance of the {@link ResourceEditor} class
    * using a {@link DefaultResourceLoader} and {@link StandardEnvironment}.
    */
   public ResourceEditor() {
      this(new DefaultResourceLoader(), null);
   }

   /**
    * Create a new instance of the {@link ResourceEditor} class
    * using the given {@link ResourceLoader} and {@link PropertyResolver}.
    * @param resourceLoader the {@code ResourceLoader} to use
    * @param propertyResolver the {@code PropertyResolver} to use
    */
   public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
      this(resourceLoader, propertyResolver, true);
   }

   /**
    * Create a new instance of the {@link ResourceEditor} class
    * using the given {@link ResourceLoader}.
    * @param resourceLoader the {@code ResourceLoader} to use
    * @param propertyResolver the {@code PropertyResolver} to use
    * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
    * if no corresponding property could be found in the given {@code propertyResolver}
    */
   public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver,
                         boolean ignoreUnresolvablePlaceholders) {

      AssertUtils.notNull(resourceLoader, "ResourceLoader must not be null");
      this.resourceLoader = resourceLoader;
      this.propertyResolver = propertyResolver;
      this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
   }

   @Override
   public void setAsText(String text) {
      if (StringUtils.hasText(text)) {
         String locationToUse = resolvePath(text).trim();
         setValue(this.resourceLoader.getResource(locationToUse));
      }
      else {
         setValue(null);
      }
   }

   /**
    * Resolve the given path, replacing placeholders with corresponding
    * property values from the {@code environment} if necessary.
    * @param path the original file path
    * @return the resolved file path
    * @see PropertyResolver#resolvePlaceholders
    * @see PropertyResolver#resolveRequiredPlaceholders
    */
   protected String resolvePath(String path) {
      if (this.propertyResolver == null) {
         this.propertyResolver = new StandardEnvironment();
      }
      return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
         this.propertyResolver.resolveRequiredPlaceholders(path));
   }

   @Override
   @Nullable
   public String getAsText() {
      Resource value = (Resource) getValue();
      try {
         // Try to determine URL for resource.
         return (value != null ? value.getURL().toExternalForm() : "");
      }
      catch (IOException ex) {
         // Couldn't determine resource URL - return null to indicate
         // that there is no appropriate text representation.
         return null;
      }
   }
}
