//package foundation.polar.gratify.core.codec;
//
//import foundation.polar.gratify.core.ResolvableType;
//import foundation.polar.gratify.core.io.buffer.DataBuffer;
//import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
//import foundation.polar.gratify.utils.MimeType;
//import foundation.polar.gratify.utils.MimeTypeUtils;
//import reactor.util.annotation.Nullable;
//
//import java.nio.ByteBuffer;
//import java.util.Map;
//
///**
// * Decoder for {@link ByteBuffer ByteBuffers}.
// *
// * @author Sebastien Deleuze
// * @author Arjen Poutsma
// * @author Rossen Stoyanchev
// */
//public class ByteBufferDecoder extends AbstractDataBufferDecoder<ByteBuffer> {
//
//   public ByteBufferDecoder() {
//      super(MimeTypeUtils.ALL);
//   }
//
//
//   @Override
//   public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
//      return (ByteBuffer.class.isAssignableFrom(elementType.toClass()) &&
//         super.canDecode(elementType, mimeType));
//   }
//
//   @Override
//   public ByteBuffer decode(DataBuffer dataBuffer, ResolvableType elementType,
//                            @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
//
//      int byteCount = dataBuffer.readableByteCount();
//      ByteBuffer copy = ByteBuffer.allocate(byteCount);
//      copy.put(dataBuffer.asByteBuffer());
//      copy.flip();
//      DataBufferUtils.release(dataBuffer);
//      if (logger.isDebugEnabled()) {
//         logger.debug(Hints.getLogPrefix(hints) + "Read " + byteCount + " bytes");
//      }
//      return copy;
//   }
//
//}
