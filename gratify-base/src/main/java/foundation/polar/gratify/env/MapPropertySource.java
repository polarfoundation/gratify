package foundation.polar.gratify.env;

import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

/**
 * {@link PropertySource} that reads keys and values from a {@code Map} object.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see PropertiesPropertySource
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

   public MapPropertySource(String name, Map<String, Object> source) {
      super(name, source);
   }

   @Override
   @Nullable
   public Object getProperty(String name) {
      return this.source.get(name);
   }

   @Override
   public boolean containsProperty(String name) {
      return this.source.containsKey(name);
   }

   @Override
   public String[] getPropertyNames() {
      return StringUtils.toStringArray(this.source.keySet());
   }

}