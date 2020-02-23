package foundation.polar.gratify.utils.logging;

import static java.util.Objects.requireNonNull;

public abstract class LoggerFactory {
   private static volatile LoggerFactory defaultFactory;

   @SuppressWarnings("UnusedCatchParameter")
   private static LoggerFactory createDefaultFactory(String name) {
      LoggerFactory factory = null;
      return factory;
   }

   public static LoggerFactory getDefaultFactory() {
      if (defaultFactory == null) {
         defaultFactory = createDefaultFactory(LoggerFactory.class.getName());
      }
      return defaultFactory;
   }

   public static void setDefaultFactory(LoggerFactory defaultFactory) {
      requireNonNull(defaultFactory, "defaultFactory");
      LoggerFactory.defaultFactory = defaultFactory;
   }

   public static Logger getInstance(Class<?> clazz) {
      return getInstance(clazz.getName());
   }

   public static Logger getInstance(String name) {
      return getDefaultFactory().newInstance(name);
   }

   protected abstract Logger newInstance(String name);
}
