package foundation.polar.gratify.core.io.support;

import foundation.polar.gratify.annotation.AnnotationAwareOrderComparator;
import foundation.polar.gratify.core.io.UrlResource;
import foundation.polar.gratify.ds.ConcurrentReferenceHashMap;
import foundation.polar.gratify.ds.LinkedMultiValueMap;
import foundation.polar.gratify.ds.MultiValueMap;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ReflectionUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * General purpose factory loading mechanism for internal use within the framework.
 *
 * <p>{@code SpringFactoriesLoader} {@linkplain #loadFactories loads} and instantiates
 * factories of a given type from {@value #FACTORIES_RESOURCE_LOCATION} files which
 * may be present in multiple JAR files in the classpath. The {@code spring.factories}
 * file must be in {@link Properties} format, where the key is the fully qualified
 * name of the interface or abstract class, and the value is a comma-separated list of
 * implementation class names. For example:
 *
 * <pre class="code">example.MyService=example.MyServiceImpl1,example.MyServiceImpl2</pre>
 *
 * where {@code example.MyService} is the name of the interface, and {@code MyServiceImpl1}
 * and {@code MyServiceImpl2} are two implementations.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class GratifyFactoriesLoader {
   /**
    * The location to look for factories.
    * <p>Can be present in multiple JAR files.
    */
   public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/gratify.factories";

   private static final Log logger = LogFactory.getLog(GratifyFactoriesLoader.class);

   private static final Map<ClassLoader, MultiValueMap<String, String>> cache = new ConcurrentReferenceHashMap<>();

   private GratifyFactoriesLoader() {
   }

   /**
    * Load and instantiate the factory implementations of the given type from
    * {@value #FACTORIES_RESOURCE_LOCATION}, using the given class loader.
    * <p>The returned factories are sorted through {@link AnnotationAwareOrderComparator}.
    * <p>If a custom instantiation strategy is required, use {@link #loadFactoryNames}
    * to obtain all registered factory names.
    * @param factoryType the interface or abstract class representing the factory
    * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
    * @throws IllegalArgumentException if any factory implementation class cannot
    * be loaded or if an error occurs while instantiating any factory
    * @see #loadFactoryNames
    */
   public static <T> List<T> loadFactories(Class<T> factoryType, @Nullable ClassLoader classLoader) {
      AssertUtils.notNull(factoryType, "'factoryType' must not be null");
      ClassLoader classLoaderToUse = classLoader;
      if (classLoaderToUse == null) {
         classLoaderToUse = GratifyFactoriesLoader.class.getClassLoader();
      }
      List<String> factoryImplementationNames = loadFactoryNames(factoryType, classLoaderToUse);
      if (logger.isTraceEnabled()) {
         logger.trace("Loaded [" + factoryType.getName() + "] names: " + factoryImplementationNames);
      }
      List<T> result = new ArrayList<>(factoryImplementationNames.size());
      for (String factoryImplementationName : factoryImplementationNames) {
         result.add(instantiateFactory(factoryImplementationName, factoryType, classLoaderToUse));
      }
      AnnotationAwareOrderComparator.sort(result);
      return result;
   }

   /**
    * Load the fully qualified class names of factory implementations of the
    * given type from {@value #FACTORIES_RESOURCE_LOCATION}, using the given
    * class loader.
    * @param factoryType the interface or abstract class representing the factory
    * @param classLoader the ClassLoader to use for loading resources; can be
    * {@code null} to use the default
    * @throws IllegalArgumentException if an error occurs while loading factory names
    * @see #loadFactories
    */
   public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
      String factoryTypeName = factoryType.getName();
      return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
   }

   private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
      MultiValueMap<String, String> result = cache.get(classLoader);
      if (result != null) {
         return result;
      }

      try {
         Enumeration<URL> urls = (classLoader != null ?
            classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
            ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
         result = new LinkedMultiValueMap<>();
         while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            UrlResource resource = new UrlResource(url);
            Properties properties = PropertiesLoaderUtils.loadProperties(resource);
            for (Map.Entry<?, ?> entry : properties.entrySet()) {
               String factoryTypeName = ((String) entry.getKey()).trim();
               for (String factoryImplementationName : StringUtils.commaDelimitedListToStringArray((String) entry.getValue())) {
                  result.add(factoryTypeName, factoryImplementationName.trim());
               }
            }
         }
         cache.put(classLoader, result);
         return result;
      }
      catch (IOException ex) {
         throw new IllegalArgumentException("Unable to load factories from location [" +
            FACTORIES_RESOURCE_LOCATION + "]", ex);
      }
   }

   @SuppressWarnings("unchecked")
   private static <T> T instantiateFactory(String factoryImplementationName, Class<T> factoryType, ClassLoader classLoader) {
      try {
         Class<?> factoryImplementationClass = ClassUtils.forName(factoryImplementationName, classLoader);
         if (!factoryType.isAssignableFrom(factoryImplementationClass)) {
            throw new IllegalArgumentException(
               "Class [" + factoryImplementationName + "] is not assignable to factory type [" + factoryType.getName() + "]");
         }
         return (T) ReflectionUtils.accessibleConstructor(factoryImplementationClass).newInstance();
      }
      catch (Throwable ex) {
         throw new IllegalArgumentException(
            "Unable to instantiate factory class [" + factoryImplementationName + "] for factory type [" + factoryType.getName() + "]",
            ex);
      }
   }
}
