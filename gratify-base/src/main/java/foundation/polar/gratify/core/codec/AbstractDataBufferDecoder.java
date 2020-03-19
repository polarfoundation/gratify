package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
import foundation.polar.gratify.utils.MimeType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Abstract base class for {@code Decoder} implementations that can decode
 * a {@code DataBuffer} directly to the target element type.
 *
 * <p>Sub-classes must implement {@link #decodeDataBuffer} to provide a way to
 * transform a {@code DataBuffer} to the target data type. The default
 * {@link #decode} implementation transforms each individual data buffer while
 * {@link #decodeToMono} applies "reduce" and transforms the aggregated buffer.
 *
 * <p>Sub-classes can override {@link #decode} in order to split the input stream
 * along different boundaries (e.g. on new line characters for {@code String})
 * or always reduce to a single data buffer (e.g. {@code Resource}).
 *
 * @author Rossen Stoyanchev
 * @param <T> the element type
 */
@SuppressWarnings("deprecation")
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {

   private int maxInMemorySize = 256 * 1024;

   protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
      super(supportedMimeTypes);
   }

   /**
    * Configure a limit on the number of bytes that can be buffered whenever
    * the input stream needs to be aggregated. This can be a result of
    * decoding to a single {@code DataBuffer},
    * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
    * {@link foundation.polar.gratify.core.io.Resource Resource}, {@code String}, etc.
    * It can also occur when splitting the input stream, e.g. delimited text,
    * in which case the limit applies to data buffered between delimiters.
    * <p>By default this is set to 256K.
    * @param byteCount the max number of bytes to buffer, or -1 for unlimited
    */
   public void setMaxInMemorySize(int byteCount) {
      this.maxInMemorySize = byteCount;
   }

   /**
    * Return the {@link #setMaxInMemorySize configured} byte count limit.
    */
   public int getMaxInMemorySize() {
      return this.maxInMemorySize;
   }

   @Override
   public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
                         @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      return Flux.from(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
   }

   @Override
   public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
                               @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      return DataBufferUtils.join(input, this.maxInMemorySize)
         .map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
   }

}
