package foundation.polar.gratify.javamodule.utils;

import java.util.Arrays;
import java.util.stream.Stream;

public final class StreamHelper {
   @SafeVarargs
   public static <T> Stream<T> concat(Stream<T>... streams) {
      return Arrays.stream(streams).reduce(Stream::concat).orElseThrow(IllegalAccessError::new);
   }
}
