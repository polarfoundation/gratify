package foundation.polar.gratify.core.serializer.support;

import foundation.polar.gratify.core.serializer.DefaultDeserializer;
import foundation.polar.gratify.core.serializer.DefaultSerializer;
import foundation.polar.gratify.core.serializer.Deserializer;
import foundation.polar.gratify.core.serializer.Serializer;
import foundation.polar.gratify.utils.AssertUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A convenient delegate with pre-arranged configuration state for common
 * serialization needs. Implements {@link Serializer} and {@link Deserializer}
 * itself, so can also be passed into such more specific callback methods.
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public class SerializationDelegate implements Serializer<Object>, Deserializer<Object> {

   private final Serializer<Object> serializer;
   private final Deserializer<Object> deserializer;

   /**
    * Create a {@code SerializationDelegate} with a default serializer/deserializer
    * for the given {@code ClassLoader}.
    * @see DefaultDeserializer
    * @see DefaultDeserializer#DefaultDeserializer(ClassLoader)
    */
   public SerializationDelegate(ClassLoader classLoader) {
      this.serializer = new DefaultSerializer();
      this.deserializer = new DefaultDeserializer(classLoader);
   }

   /**
    * Create a {@code SerializationDelegate} with the given serializer/deserializer.
    * @param serializer the {@link Serializer} to use (never {@code null)}
    * @param deserializer the {@link Deserializer} to use (never {@code null)}
    */
   public SerializationDelegate(Serializer<Object> serializer, Deserializer<Object> deserializer) {
      AssertUtils.notNull(serializer, "Serializer must not be null");
      AssertUtils.notNull(deserializer, "Deserializer must not be null");
      this.serializer = serializer;
      this.deserializer = deserializer;
   }

   @Override
   public void serialize(Object object, OutputStream outputStream) throws IOException {
      this.serializer.serialize(object, outputStream);
   }

   @Override
   public Object deserialize(InputStream inputStream) throws IOException {
      return this.deserializer.deserialize(inputStream);
   }
}