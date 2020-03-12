package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.ByteArrayResource;
import foundation.polar.gratify.core.io.InputStreamResource;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Decoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ResourceDecoder extends AbstractDataBufferDecoder<Resource> {

   /** Name of hint with a filename for the resource(e.g. from "Content-Disposition" HTTP header). */
   public static String FILENAME_HINT = ResourceDecoder.class.getName() + ".filename";

   public ResourceDecoder() {
      super(MimeTypeUtils.ALL);
   }

   @Override
   public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
      return (Resource.class.isAssignableFrom(elementType.toClass()) &&
         super.canDecode(elementType, mimeType));
   }

   @Override
   public Flux<Resource> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
                                @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      return Flux.from(decodeToMono(inputStream, elementType, mimeType, hints));
   }

   @Override
   public Resource decode(DataBuffer dataBuffer, ResolvableType elementType,
                          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      byte[] bytes = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(bytes);
      DataBufferUtils.release(dataBuffer);

      if (logger.isDebugEnabled()) {
         logger.debug(Hints.getLogPrefix(hints) + "Read " + bytes.length + " bytes");
      }

      Class<?> clazz = elementType.toClass();
      String filename = hints != null ? (String) hints.get(FILENAME_HINT) : null;
      if (clazz == InputStreamResource.class) {
         return new InputStreamResource(new ByteArrayInputStream(bytes)) {
            @Override
            public String getFilename() {
               return filename;
            }
            @Override
            public long contentLength() {
               return bytes.length;
            }
         };
      }
      else if (Resource.class.isAssignableFrom(clazz)) {
         return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
               return filename;
            }
         };
      }
      else {
         throw new IllegalStateException("Unsupported resource class: " + clazz);
      }
   }


}
