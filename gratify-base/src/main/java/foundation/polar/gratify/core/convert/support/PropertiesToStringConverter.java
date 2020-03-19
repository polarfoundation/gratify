package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Converts from a Properties to a String by calling {@link Properties#store(java.io.OutputStream, String)}.
 * Decodes with the ISO-8859-1 charset before returning the String.
 *
 * @author Keith Donald
 */
final class PropertiesToStringConverter implements Converter<Properties, String> {

   @Override
   public String convert(Properties source) {
      try {
         ByteArrayOutputStream os = new ByteArrayOutputStream(256);
         source.store(os, null);
         return os.toString("ISO-8859-1");
      }
      catch (IOException ex) {
         // Should never happen.
         throw new IllegalArgumentException("Failed to store [" + source + "] into String", ex);
      }
   }
}
