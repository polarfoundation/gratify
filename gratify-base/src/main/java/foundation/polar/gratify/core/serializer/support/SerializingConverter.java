package foundation.polar.gratify.core.serializer.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.serializer.DefaultSerializer;
import foundation.polar.gratify.core.serializer.Serializer;
import foundation.polar.gratify.utils.AssertUtils;

import java.io.ByteArrayOutputStream;

/**
 * A {@link Converter} that delegates to a
 * {@link foundation.polar.gratify.core.serializer.Serializer}
 * to convert an object to a byte array.
 *
 * @author Gary Russell
 * @author Mark Fisher
 */
public class SerializingConverter implements Converter<Object, byte[]> {
   private final Serializer<Object> serializer;
   /**
    * Create a default {@code SerializingConverter} that uses standard Java serialization.
    */
   public SerializingConverter() {
      this.serializer = new DefaultSerializer();
   }

   /**
    * Create a {@code SerializingConverter} that delegates to the provided {@link Serializer}.
    */
   public SerializingConverter(Serializer<Object> serializer) {
      AssertUtils.notNull(serializer, "Serializer must not be null");
      this.serializer = serializer;
   }

   /**
    * Serializes the source object and returns the byte array result.
    */
   @Override
   public byte[] convert(Object source) {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1024);
      try  {
         this.serializer.serialize(source, byteStream);
         return byteStream.toByteArray();
      } catch (Throwable ex) {
         throw new SerializationFailedException("Failed to serialize object using " +
            this.serializer.getClass().getSimpleName(), ex);
      }
   }
}
