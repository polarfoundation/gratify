//package foundation.polar.gratify.core.codec;
//
//import foundation.polar.gratify.core.ResolvableType;
//import foundation.polar.gratify.core.io.buffer.DataBuffer;
//import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
//import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
//import foundation.polar.gratify.utils.MimeType;
//import foundation.polar.gratify.utils.MimeTypeUtils;
//import foundation.polar.gratify.utils.logging.LogFormatUtils;
//import org.reactivestreams.Publisher;
//import reactor.core.publisher.Flux;
//import reactor.util.annotation.Nullable;
//
//import java.nio.charset.Charset;
//import java.nio.charset.CoderMalfunctionError;
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
///**
// * Encode from a {@code CharSequence} stream to a bytes stream.
// *
// * @author Sebastien Deleuze
// * @author Arjen Poutsma
// * @author Rossen Stoyanchev
// * @see StringDecoder
// */
//public final class CharSequenceEncoder extends AbstractEncoder<CharSequence> {
//
//   /**
//    * The default charset used by the encoder.
//    */
//   public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
//
//   private final ConcurrentMap<Charset, Float> charsetToMaxBytesPerChar =
//      new ConcurrentHashMap<>(3);
//
//
//   private CharSequenceEncoder(MimeType... mimeTypes) {
//      super(mimeTypes);
//   }
//
//
//   @Override
//   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
//      Class<?> clazz = elementType.toClass();
//      return super.canEncode(elementType, mimeType) && CharSequence.class.isAssignableFrom(clazz);
//   }
//
//   @Override
//   public Flux<DataBuffer> encode(Publisher<? extends CharSequence> inputStream,
//                                  DataBufferFactory bufferFactory, ResolvableType elementType,
//                                  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
//
//      return Flux.from(inputStream).map(charSequence ->
//         encodeValue(charSequence, bufferFactory, elementType, mimeType, hints));
//   }
//
//   @Override
//   public DataBuffer encodeValue(CharSequence charSequence, DataBufferFactory bufferFactory,
//                                 ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
//
//      if (!Hints.isLoggingSuppressed(hints)) {
//         LogFormatUtils.traceDebug(logger, traceOn -> {
//            String formatted = LogFormatUtils.formatValue(charSequence, !traceOn);
//            return Hints.getLogPrefix(hints) + "Writing " + formatted;
//         });
//      }
//      boolean release = true;
//      Charset charset = getCharset(mimeType);
//      int capacity = calculateCapacity(charSequence, charset);
//      DataBuffer dataBuffer = bufferFactory.allocateBuffer(capacity);
//      try {
//         dataBuffer.write(charSequence, charset);
//         release = false;
//      }
//      catch (CoderMalfunctionError ex) {
//         throw new EncodingException("String encoding error: " + ex.getMessage(), ex);
//      }
//      finally {
//         if (release) {
//            DataBufferUtils.release(dataBuffer);
//         }
//      }
//      return dataBuffer;
//   }
//
//   int calculateCapacity(CharSequence sequence, Charset charset) {
//      float maxBytesPerChar = this.charsetToMaxBytesPerChar
//         .computeIfAbsent(charset, cs -> cs.newEncoder().maxBytesPerChar());
//      float maxBytesForSequence = sequence.length() * maxBytesPerChar;
//      return (int) Math.ceil(maxBytesForSequence);
//   }
//
//   private Charset getCharset(@Nullable MimeType mimeType) {
//      if (mimeType != null && mimeType.getCharset() != null) {
//         return mimeType.getCharset();
//      }
//      else {
//         return DEFAULT_CHARSET;
//      }
//   }
//
//
//   /**
//    * Create a {@code CharSequenceEncoder} that supports only "text/plain".
//    */
//   public static CharSequenceEncoder textPlainOnly() {
//      return new CharSequenceEncoder(new MimeType("text", "plain", DEFAULT_CHARSET));
//   }
//
//   /**
//    * Create a {@code CharSequenceEncoder} that supports all MIME types.
//    */
//   public static CharSequenceEncoder allMimeTypes() {
//      return new CharSequenceEncoder(new MimeType("text", "plain", DEFAULT_CHARSET), MimeTypeUtils.ALL);
//   }
//
//}
