package foundation.polar.gratify.core.io.support;

import foundation.polar.gratify.core.io.DefaultResourceLoader;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.env.PropertiesPropertySource;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Subclass of {@link PropertiesPropertySource} that loads a {@link Properties} object
 * from a given {@link foundation.polar.gratify.core.io.Resource} or resource location such as
 * {@code "classpath:/com/myco/foo.properties"} or {@code "file:/path/to/file.xml"}.
 *
 * <p>Both traditional and XML-based properties file formats are supported; however, in
 * order for XML processing to take effect, the underlying {@code Resource}'s
 * {@link foundation.polar.gratify.core.io.Resource#getFilename() getFilename()} method must
 * return a non-{@code null} value that ends in {@code ".xml"}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 *
 * @see foundation.polar.gratify.core.io.Resource
 * @see foundation.polar.gratify.core.io.support.EncodedResource
 */
public class ResourcePropertySource extends PropertiesPropertySource {
   /** The original resource name, if different from the given name. */
   @Nullable
   private final String resourceName;

   /**
    * Create a PropertySource having the given name based on Properties
    * loaded from the given encoded resource.
    */
   public ResourcePropertySource(String name, EncodedResource resource) throws IOException {
      super(name, PropertiesLoaderUtils.loadProperties(resource));
      this.resourceName = getNameForResource(resource.getResource());
   }

   /**
    * Create a PropertySource based on Properties loaded from the given resource.
    * The name of the PropertySource will be generated based on the
    * {@link Resource#getDescription() description} of the given resource.
    */
   public ResourcePropertySource(EncodedResource resource) throws IOException {
      super(getNameForResource(resource.getResource()), PropertiesLoaderUtils.loadProperties(resource));
      this.resourceName = null;
   }

   /**
    * Create a PropertySource having the given name based on Properties
    * loaded from the given encoded resource.
    */
   public ResourcePropertySource(String name, Resource resource) throws IOException {
      super(name, PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
      this.resourceName = getNameForResource(resource);
   }

   /**
    * Create a PropertySource based on Properties loaded from the given resource.
    * The name of the PropertySource will be generated based on the
    * {@link Resource#getDescription() description} of the given resource.
    */
   public ResourcePropertySource(Resource resource) throws IOException {
      super(getNameForResource(resource), PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
      this.resourceName = null;
   }

   /**
    * Create a PropertySource having the given name based on Properties loaded from
    * the given resource location and using the given class loader to load the
    * resource (assuming it is prefixed with {@code classpath:}).
    */
   public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
      this(name, new DefaultResourceLoader(classLoader).getResource(location));
   }

   /**
    * Create a PropertySource based on Properties loaded from the given resource
    * location and use the given class loader to load the resource, assuming it is
    * prefixed with {@code classpath:}. The name of the PropertySource will be
    * generated based on the {@link Resource#getDescription() description} of the
    * resource.
    */
   public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
      this(new DefaultResourceLoader(classLoader).getResource(location));
   }

   /**
    * Create a PropertySource having the given name based on Properties loaded from
    * the given resource location. The default thread context class loader will be
    * used to load the resource (assuming the location string is prefixed with
    * {@code classpath:}.
    */
   public ResourcePropertySource(String name, String location) throws IOException {
      this(name, new DefaultResourceLoader().getResource(location));
   }

   /**
    * Create a PropertySource based on Properties loaded from the given resource
    * location. The name of the PropertySource will be generated based on the
    * {@link Resource#getDescription() description} of the resource.
    */
   public ResourcePropertySource(String location) throws IOException {
      this(new DefaultResourceLoader().getResource(location));
   }

   private ResourcePropertySource(String name, @Nullable String resourceName, Map<String, Object> source) {
      super(name, source);
      this.resourceName = resourceName;
   }

   /**
    * Return a potentially adapted variant of this {@link ResourcePropertySource},
    * overriding the previously given (or derived) name with the specified name.
    */
   public ResourcePropertySource withName(String name) {
      if (this.name.equals(name)) {
         return this;
      }
      // Store the original resource name if necessary...
      if (this.resourceName != null) {
         if (this.resourceName.equals(name)) {
            return new ResourcePropertySource(this.resourceName, null, this.source);
         }
         else {
            return new ResourcePropertySource(name, this.resourceName, this.source);
         }
      }
      else {
         // Current name is resource name -> preserve it in the extra field...
         return new ResourcePropertySource(name, this.name, this.source);
      }
   }

   /**
    * Return a potentially adapted variant of this {@link ResourcePropertySource},
    * overriding the previously given name (if any) with the original resource name
    * (equivalent to the name generated by the name-less constructor variants).
    */
   public ResourcePropertySource withResourceName() {
      if (this.resourceName == null) {
         return this;
      }
      return new ResourcePropertySource(this.resourceName, null, this.source);
   }

   /**
    * Return the description for the given Resource; if the description is
    * empty, return the class name of the resource plus its identity hash code.
    * @see foundation.polar.gratify.core.io.Resource#getDescription()
    */
   private static String getNameForResource(Resource resource) {
      String name = resource.getDescription();
      if (!StringUtils.hasText(name)) {
         name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
      }
      return name;
   }
}
