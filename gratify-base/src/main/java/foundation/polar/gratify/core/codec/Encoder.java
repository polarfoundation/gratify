package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.utils.MimeType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Strategy to encode a stream of Objects of type {@code <T>} into an output
 * stream of bytes.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @param <T> the type of elements in the input stream
 */
public interface Encoder<T> {
   /**
    * Whether the encoder supports the given source element type and the MIME
    * type for the output stream.
    * @param elementType the type of elements in the source stream
    * @param mimeType the MIME type for the output stream
    * (can be {@code null} if not specified)
    * @return {@code true} if supported, {@code false} otherwise
    */
   boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType);

   /**
    * Encode a stream of Objects of type {@code T} into a {@link DataBuffer}
    * output stream.
    * @param inputStream the input stream of Objects to encode. If the input should be
    * encoded as a single value rather than as a stream of elements, an instance of
    * {@link Mono} should be used.
    * @param bufferFactory for creating output stream {@code DataBuffer}'s
    * @param elementType the expected type of elements in the input stream;
    * this type must have been previously passed to the {@link #canEncode}
    * method and it must have returned {@code true}.
    * @param mimeType the MIME type for the output content (optional)
    * @param hints additional information about how to encode
    * @return the output stream
    */
   Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
                           ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

   /**
    * Encode an Object of type T to a data buffer. This is useful for scenarios,
    * that distinct messages (or events) are encoded and handled individually,
    * in fully aggregated form.
    * <p>By default this method raises {@link UnsupportedOperationException}
    * and it is expected that some encoders cannot produce a single buffer or
    * cannot do so synchronously (e.g. encoding a {@code Resource}).
    * @param value the value to be encoded
    * @param bufferFactory for creating the output {@code DataBuffer}
    * @param valueType the type for the value being encoded
    * @param mimeType the MIME type for the output content (optional)
    * @param hints additional information about how to encode
    * @return the encoded content
    */
   default DataBuffer encodeValue(T value, DataBufferFactory bufferFactory,
                                  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

      // It may not be possible to produce a single DataBuffer synchronously
      throw new UnsupportedOperationException();
   }

   /**
    * Return the list of mime types this encoder supports.
    */
   List<MimeType> getEncodableMimeTypes();
}
