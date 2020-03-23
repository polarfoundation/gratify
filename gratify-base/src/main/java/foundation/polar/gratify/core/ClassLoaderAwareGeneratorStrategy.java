package foundation.polar.gratify.core;

import net.sf.cglib.core.ClassGenerator;
import net.sf.cglib.core.DefaultGeneratorStrategy;

/**
 * CGLIB GeneratorStrategy variant which exposes the application ClassLoader
 * as current thread context ClassLoader for the time of class generation.
 * The ASM ClassWriter in Gratify's ASM variant will pick it up when doing
 * common superclass resolution.
 *
 * @author Juergen Hoeller
 */
public class ClassLoaderAwareGeneratorStrategy extends DefaultGeneratorStrategy {

   private final ClassLoader classLoader;

   public ClassLoaderAwareGeneratorStrategy(ClassLoader classLoader) {
      this.classLoader = classLoader;
   }

   @Override
   public byte[] generate(ClassGenerator cg) throws Exception {
      if (this.classLoader == null) {
         return super.generate(cg);
      }

      Thread currentThread = Thread.currentThread();
      ClassLoader threadContextClassLoader;
      try {
         threadContextClassLoader = currentThread.getContextClassLoader();
      }
      catch (Throwable ex) {
         // Cannot access thread context ClassLoader - falling back...
         return super.generate(cg);
      }

      boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
      if (overrideClassLoader) {
         currentThread.setContextClassLoader(this.classLoader);
      }
      try {
         return super.generate(cg);
      }
      finally {
         if (overrideClassLoader) {
            // Reset original thread context ClassLoader.
            currentThread.setContextClassLoader(threadContextClassLoader);
         }
      }
   }
}
