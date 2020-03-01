package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Converts a String to a Properties by calling Properties#load(java.io.InputStream).
 * Uses ISO-8559-1 encoding required by Properties.
 *
 * @author Keith Donald
 */
final class StringToPropertiesConverter implements Converter<String, Properties> {

   @Override
   public Properties convert(String source) {
      try {
         Properties props = new Properties();
         // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
         props.load(new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));
         return props;
      }
      catch (Exception ex) {
         // Should never happen.
         throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
      }
   }

}