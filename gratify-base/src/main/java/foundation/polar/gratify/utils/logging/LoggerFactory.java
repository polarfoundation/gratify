//package foundation.polar.gratify.utils.logging;
//
//import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
//import org.checkerframework.checker.nullness.qual.Nullable;
//
//import static java.util.Objects.requireNonNull;
//
//public abstract class LoggerFactory {
//
//   @Nullable
//   private static volatile LoggerFactory defaultFactory;
//
//   @SuppressWarnings("UnusedCatchParameter")
//   @Nullable
//   private static LoggerFactory createDefaultFactory(String name) {
//      return null;
//   }
//
//   @Nullable
//   public static LoggerFactory getDefaultFactory() {
//      if (defaultFactory == null) {
//         defaultFactory = createDefaultFactory(LoggerFactory.class.getName());
//      }
//      return defaultFactory;
//   }
//
//   public static void setDefaultFactory(LoggerFactory defaultFactory) {
//      requireNonNull(defaultFactory, "defaultFactory");
//      LoggerFactory.defaultFactory = defaultFactory;
//   }
//
//   @Nullable
//   public static Logger getInstance(Class<?> clazz) {
//      return getInstance(clazz.getName());
//   }
//
//   @Nullable
//   public static Logger getInstance(String name) {
//      return getDefaultFactory().newInstance(name);
//   }
//
//   protected abstract Logger newInstance(String name);
//}
