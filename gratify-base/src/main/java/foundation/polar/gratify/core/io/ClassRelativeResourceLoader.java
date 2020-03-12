package foundation.polar.gratify.core.io;


import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.StringUtils;

/**
 * {@link ResourceLoader} implementation that interprets plain resource paths
 * as relative to a given {@code java.lang.Class}.
 *
 * @author Juergen Hoeller
 * @see Class#getResource(String)
 * @see ClassPathResource#ClassPathResource(String, Class)
 */
public class ClassRelativeResourceLoader extends DefaultResourceLoader {
   private final Class<?> clazz;

   /**
    * Create a new ClassRelativeResourceLoader for the given class.
    * @param clazz the class to load resources through
    */
   public ClassRelativeResourceLoader(Class<?> clazz) {
      AssertUtils.notNull(clazz, "Class must not be null");
      this.clazz = clazz;
      setClassLoader(clazz.getClassLoader());
   }

   @Override
   protected Resource getResourceByPath(String path) {
      return new ClassRelativeContextResource(path, this.clazz);
   }

   /**
    * ClassPathResource that explicitly expresses a context-relative path
    * through implementing the ContextResource interface.
    */
   private static class ClassRelativeContextResource extends ClassPathResource implements ContextResource {

      private final Class<?> clazz;

      public ClassRelativeContextResource(String path, Class<?> clazz) {
         super(path, clazz);
         this.clazz = clazz;
      }

      @Override
      public String getPathWithinContext() {
         return getPath();
      }

      @Override
      public Resource createRelative(String relativePath) {
         String pathToUse = StringUtils.applyRelativePath(getPath(), relativePath);
         return new ClassRelativeContextResource(pathToUse, this.clazz);
      }
   }
}
