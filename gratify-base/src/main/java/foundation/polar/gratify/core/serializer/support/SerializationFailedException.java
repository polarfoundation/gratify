package foundation.polar.gratify.core.serializer.support;

import foundation.polar.gratify.core.NestedRuntimeException;

/**
 * Wrapper for the native IOException (or similar) when a
 * {@link foundation.polar.gratify.core.serializer.Serializer} or
 * {@link foundation.polar.gratify.core.serializer.Deserializer} failed.
 * Thrown by {@link SerializingConverter} and {@link DeserializingConverter}.
 *
 * @author Gary Russell
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class SerializationFailedException extends NestedRuntimeException {
   /**
    * Construct a {@code SerializationException} with the specified detail message.
    * @param message the detail message
    */
   public SerializationFailedException(String message) {
      super(message);
   }

   /**
    * Construct a {@code SerializationException} with the specified detail message
    * and nested exception.
    * @param message the detail message
    * @param cause the nested exception
    */
   public SerializationFailedException(String message, Throwable cause) {
      super(message, cause);
   }
}
