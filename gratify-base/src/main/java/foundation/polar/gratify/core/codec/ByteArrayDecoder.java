package foundation.polar.gratify.core.codec;


import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

/**
 * Decoder for {@code byte} arrays.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ByteArrayDecoder extends AbstractDataBufferDecoder<byte[]> {

   public ByteArrayDecoder() {
      super(MimeTypeUtils.ALL);
   }

   @Override
   public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
      return (elementType.resolve() == byte[].class && super.canDecode(elementType, mimeType));
   }

   @Override
   public byte[] decode(DataBuffer dataBuffer, ResolvableType elementType,
                        @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      byte[] result = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(result);
      DataBufferUtils.release(dataBuffer);
      if (logger.isDebugEnabled()) {
         logger.debug(Hints.getLogPrefix(hints) + "Read " + result.length + " bytes");
      }
      return result;
   }

}
