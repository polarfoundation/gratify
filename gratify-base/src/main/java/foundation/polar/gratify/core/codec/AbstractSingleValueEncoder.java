package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.core.io.buffer.PooledDataBuffer;
import foundation.polar.gratify.utils.MimeType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Abstract base class for {@link foundation.polar.gratify.core.codec.Encoder}
 * classes that can only deal with a single value.
 *
 * @author Arjen Poutsma
 * @param <T> the element type
 */
public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {
   public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
      super(supportedMimeTypes);
   }

   @Override
   public final Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
                                        ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      return Flux.from(inputStream)
         .take(1)
         .concatMap(value -> encode(value, bufferFactory, elementType, mimeType, hints))
         .doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release);
   }

   /**
    * Encode {@code T} to an output {@link DataBuffer} stream.
    * @param t the value to process
    * @param dataBufferFactory a buffer factory used to create the output
    * @param type the stream element type to process
    * @param mimeType the mime type to process
    * @param hints additional information about how to do decode, optional
    * @return the output stream
    */
   protected abstract Flux<DataBuffer> encode(T t, DataBufferFactory dataBufferFactory,
                                              ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}