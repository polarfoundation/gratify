package foundation.polar.gratify.utils;

import static java.util.Objects.requireNonNull;

public class StringUtils {
   public static String simpleClassName(Object object) {
      if (object == null) {
         return "null_object";
      } else {
         return simpleClassName(object.getClass());
      }
   }

   public static String simpleClassName(Class<?> clazz) {
      String className = requireNonNull(clazz, "clazz").getName();
      final int lastDotIdx = className.lastIndexOf('.');
      if (lastDotIdx > -1) {
         return className.substring(lastDotIdx + 1);
      }
      return className;
   }
}
