//package foundation.polar.gratify.core.codec;
//
//import foundation.polar.gratify.core.ResolvableType;
//import foundation.polar.gratify.core.io.buffer.DataBuffer;
//import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
//import foundation.polar.gratify.utils.MimeType;
//import foundation.polar.gratify.utils.MimeTypeUtils;
//import org.reactivestreams.Publisher;
//import reactor.core.publisher.Flux;
//import reactor.util.annotation.Nullable;
//
//import java.nio.ByteBuffer;
//import java.util.Map;
//
///**
// * Encoder for {@link ByteBuffer ByteBuffers}.
// *
// * @author Sebastien Deleuze
// */
//public class ByteBufferEncoder extends AbstractEncoder<ByteBuffer> {
//
//   public ByteBufferEncoder() {
//      super(MimeTypeUtils.ALL);
//   }
//
//   @Override
//   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
//      Class<?> clazz = elementType.toClass();
//      return super.canEncode(elementType, mimeType) && ByteBuffer.class.isAssignableFrom(clazz);
//   }
//
//   @Override
//   public Flux<DataBuffer> encode(Publisher<? extends ByteBuffer> inputStream,
//                                  DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
//                                  @Nullable Map<String, Object> hints) {
//
//      return Flux.from(inputStream).map(byteBuffer ->
//         encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
//   }
//
//   @Override
//   public DataBuffer encodeValue(ByteBuffer byteBuffer, DataBufferFactory bufferFactory,
//                                 ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
//
//      DataBuffer dataBuffer = bufferFactory.wrap(byteBuffer);
//      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
//         String logPrefix = Hints.getLogPrefix(hints);
//         logger.debug(logPrefix + "Writing " + dataBuffer.readableByteCount() + " bytes");
//      }
//      return dataBuffer;
//   }
//
//}
