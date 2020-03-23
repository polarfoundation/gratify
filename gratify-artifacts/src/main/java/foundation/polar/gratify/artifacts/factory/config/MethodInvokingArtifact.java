package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.ArtifactFactoryAware;
import foundation.polar.gratify.artifacts.factory.InitializingArtifact;
import foundation.polar.gratify.artifacts.support.ArgumentConvertingMethodInvoker;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.InvocationTargetException;

public class MethodInvokingArtifact extends ArgumentConvertingMethodInvoker
   implements ArtifactClassLoaderAware, ArtifactFactoryAware, InitializingArtifact {
   @Nullable
   private ClassLoader artifactClassLoader = ClassUtils.getDefaultClassLoader();

   @Nullable
   private ConfigurableArtifactFactory artifactFactory;

   @Override
   public void setArtifactClassLoader(ClassLoader classLoader) {
      this.artifactClassLoader = classLoader;
   }

   @Override
   protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
      return ClassUtils.forName(className, this.artifactClassLoader);
   }

   @Override
   public void setArtifactFactory(ArtifactFactory artifactFactory) {
      if (artifactFactory instanceof ConfigurableArtifactFactory) {
         this.artifactFactory = (ConfigurableArtifactFactory) artifactFactory;
      }
   }

   /**
    * Obtain the TypeConverter from the ArtifactFactory that this artifact runs in,
    * if possible.
    * @see ConfigurableArtifactFactory#getTypeConverter()
    */
   @Override
   protected TypeConverter getDefaultTypeConverter() {
      if (this.artifactFactory != null) {
         return this.artifactFactory.getTypeConverter();
      }
      else {
         return super.getDefaultTypeConverter();
      }
   }

   @Override
   public void afterPropertiesSet() throws Exception {
      prepare();
      invokeWithTargetException();
   }

   /**
    * Perform the invocation and convert InvocationTargetException
    * into the underlying target exception.
    */
   @Nullable
   protected Object invokeWithTargetException() throws Exception {
      try {
         return invoke();
      }
      catch (InvocationTargetException ex) {
         if (ex.getTargetException() instanceof Exception) {
            throw (Exception) ex.getTargetException();
         }
         if (ex.getTargetException() instanceof Error) {
            throw (Error) ex.getTargetException();
         }
         throw ex;
      }
   }
}
