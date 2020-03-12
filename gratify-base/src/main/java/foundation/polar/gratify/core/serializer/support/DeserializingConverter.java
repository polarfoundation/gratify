package foundation.polar.gratify.core.serializer.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.serializer.DefaultDeserializer;
import foundation.polar.gratify.core.serializer.Deserializer;
import foundation.polar.gratify.utils.AssertUtils;

import java.io.ByteArrayInputStream;

/**
 * A {@link Converter} that delegates to a
 * {@link foundation.polar.gratify.core.serializer.Deserializer}
 * to convert data in a byte array to an object.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 */
public class DeserializingConverter implements Converter<byte[], Object> {
   private final Deserializer<Object> deserializer;

   /**
    * Create a {@code DeserializingConverter} with default {@link java.io.ObjectInputStream}
    * configuration, using the "latest user-defined ClassLoader".
    * @see DefaultDeserializer#DefaultDeserializer()
    */
   public DeserializingConverter() {
      this.deserializer = new DefaultDeserializer();
   }

   /**
    * Create a {@code DeserializingConverter} for using an {@link java.io.ObjectInputStream}
    * with the given {@code ClassLoader}.
    * @since 4.2.1
    * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
    */
   public DeserializingConverter(ClassLoader classLoader) {
      this.deserializer = new DefaultDeserializer(classLoader);
   }

   /**
    * Create a {@code DeserializingConverter} that delegates to the provided {@link Deserializer}.
    */
   public DeserializingConverter(Deserializer<Object> deserializer) {
      AssertUtils.notNull(deserializer, "Deserializer must not be null");
      this.deserializer = deserializer;
   }

   @Override
   public Object convert(byte[] source) {
      ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
      try {
         return this.deserializer.deserialize(byteStream);
      }
      catch (Throwable ex) {
         throw new SerializationFailedException("Failed to deserialize payload. " +
            "Is the byte array a result of corresponding serialization for " +
            this.deserializer.getClass().getSimpleName() + "?", ex);
      }
   }
}
