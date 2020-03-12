package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Encoder for {@code byte} arrays.
 *
 * @author Arjen Poutsma
 */
public class ByteArrayEncoder extends AbstractEncoder<byte[]> {

   public ByteArrayEncoder() {
      super(MimeTypeUtils.ALL);
   }


   @Override
   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
      Class<?> clazz = elementType.toClass();
      return super.canEncode(elementType, mimeType) && byte[].class.isAssignableFrom(clazz);
   }

   @Override
   public Flux<DataBuffer> encode(Publisher<? extends byte[]> inputStream,
                                  DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
                                  @Nullable Map<String, Object> hints) {

      // Use (byte[] bytes) for Eclipse
      return Flux.from(inputStream).map((byte[] bytes) ->
         encodeValue(bytes, bufferFactory, elementType, mimeType, hints));
   }

   @Override
   public DataBuffer encodeValue(byte[] bytes, DataBufferFactory bufferFactory,
                                 ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      DataBuffer dataBuffer = bufferFactory.wrap(bytes);
      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
         String logPrefix = Hints.getLogPrefix(hints);
         logger.debug(logPrefix + "Writing " + dataBuffer.readableByteCount() + " bytes");
      }
      return dataBuffer;
   }

}
