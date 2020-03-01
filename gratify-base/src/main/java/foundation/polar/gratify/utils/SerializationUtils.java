package foundation.polar.gratify.utils;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.*;

/**
 * Static utilities for serialization and deserialization.
 *
 * @author Dave Syer
 */
public abstract class SerializationUtils {
   /**
    * Serialize the given object to a byte array.
    * @param object the object to serialize
    * @return an array of bytes representing the object in a portable fashion
    */
   @Nullable
   public static byte[] serialize(@Nullable Object object) {
      if (object == null) {
         return null;
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
         oos.writeObject(object);
         oos.flush();
      }
      catch (IOException ex) {
         throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), ex);
      }
      return baos.toByteArray();
   }

   /**
    * Deserialize the byte array into an object.
    * @param bytes a serialized object
    * @return the result of deserializing the bytes
    */
   @Nullable
   public static Object deserialize(@Nullable byte[] bytes) {
      if (bytes == null) {
         return null;
      }
      try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
         return ois.readObject();
      }
      catch (IOException ex) {
         throw new IllegalArgumentException("Failed to deserialize object", ex);
      }
      catch (ClassNotFoundException ex) {
         throw new IllegalStateException("Failed to deserialize object type", ex);
      }
   }
}
