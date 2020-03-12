package foundation.polar.gratify.utils;

import java.io.*;
import java.util.Properties;

/**
 * Default implementation of the {@link PropertiesPersister} interface.
 * Follows the native parsing of {@code java.util.Properties}.
 *
 * <p>Allows for reading from any Reader and writing to any Writer, for example
 * to specify a charset for a properties file. This is a capability that standard
 * {@code java.util.Properties} unfortunately lacked up until JDK 5:
 * You were only able to load files using the ISO-8859-1 charset there.
 *
 * <p>Loading from and storing to a stream delegates to {@code Properties.load}
 * and {@code Properties.store}, respectively, to be fully compatible with
 * the Unicode conversion as implemented by the JDK Properties class. As of JDK 6,
 * {@code Properties.load/store} will also be used for readers/writers,
 * effectively turning this class into a plain backwards compatibility adapter.
 *
 * <p>The persistence code that works with Reader/Writer follows the JDK's parsing
 * strategy but does not implement Unicode conversion, because the Reader/Writer
 * should already apply proper decoding/encoding of characters. If you prefer
 * to escape unicode characters in your properties files, do <i>not</i> specify
 * an encoding for a Reader/Writer (like ReloadableResourceBundleMessageSource's
 * "defaultEncoding" and "fileEncodings" properties).
 *
 * @author Juergen Hoeller
 * @see java.util.Properties
 * @see java.util.Properties#load
 * @see java.util.Properties#store
 */
public class DefaultPropertiesPersister implements PropertiesPersister {
   @Override
   public void load(Properties props, InputStream is) throws IOException {
      props.load(is);
   }

   @Override
   public void load(Properties props, Reader reader) throws IOException {
      props.load(reader);
   }

   @Override
   public void store(Properties props, OutputStream os, String header) throws IOException {
      props.store(os, header);
   }

   @Override
   public void store(Properties props, Writer writer, String header) throws IOException {
      props.store(writer, header);
   }

   @Override
   public void loadFromXml(Properties props, InputStream is) throws IOException {
      props.loadFromXML(is);
   }

   @Override
   public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
      props.storeToXML(os, header);
   }

   @Override
   public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
      props.storeToXML(os, header, encoding);
   }
}
