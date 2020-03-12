package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import foundation.polar.gratify.utils.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.Map;

/**
 * Encoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 */
public class ResourceEncoder extends AbstractSingleValueEncoder<Resource> {

   /**
    * The default buffer size used by the encoder.
    */
   public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

   private final int bufferSize;


   public ResourceEncoder() {
      this(DEFAULT_BUFFER_SIZE);
   }

   public ResourceEncoder(int bufferSize) {
      super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
      AssertUtils.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
      this.bufferSize = bufferSize;
   }

   @Override
   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
      Class<?> clazz = elementType.toClass();
      return (super.canEncode(elementType, mimeType) && Resource.class.isAssignableFrom(clazz));
   }

   @Override
   protected Flux<DataBuffer> encode(Resource resource, DataBufferFactory bufferFactory,
                                     ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
         String logPrefix = Hints.getLogPrefix(hints);
         logger.debug(logPrefix + "Writing [" + resource + "]");
      }
      return DataBufferUtils.read(resource, bufferFactory, this.bufferSize);
   }

}
