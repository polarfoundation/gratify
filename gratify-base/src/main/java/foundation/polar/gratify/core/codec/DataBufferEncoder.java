package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.Map;

/**
 * Simple pass-through encoder for {@link DataBuffer DataBuffers}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class DataBufferEncoder extends AbstractEncoder<DataBuffer> {

   public DataBufferEncoder() {
      super(MimeTypeUtils.ALL);
   }


   @Override
   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
      Class<?> clazz = elementType.toClass();
      return super.canEncode(elementType, mimeType) && DataBuffer.class.isAssignableFrom(clazz);
   }

   @Override
   public Flux<DataBuffer> encode(Publisher<? extends DataBuffer> inputStream,
                                  DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
                                  @Nullable Map<String, Object> hints) {

      Flux<DataBuffer> flux = Flux.from(inputStream);
      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
         flux = flux.doOnNext(buffer -> logValue(buffer, hints));
      }
      return flux;
   }

   @Override
   public DataBuffer encodeValue(DataBuffer buffer, DataBufferFactory bufferFactory,
                                 ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
         logValue(buffer, hints);
      }
      return buffer;
   }

   private void logValue(DataBuffer buffer, @Nullable Map<String, Object> hints) {
      String logPrefix = Hints.getLogPrefix(hints);
      logger.debug(logPrefix + "Writing " + buffer.readableByteCount() + " bytes");
   }

}
