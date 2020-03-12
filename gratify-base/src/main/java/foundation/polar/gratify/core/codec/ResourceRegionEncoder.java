package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.InputStreamResource;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.core.io.buffer.DataBuffer;
import foundation.polar.gratify.core.io.buffer.DataBufferFactory;
import foundation.polar.gratify.core.io.buffer.DataBufferUtils;
import foundation.polar.gratify.core.io.support.ResourceRegion;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.MimeType;
import foundation.polar.gratify.utils.MimeTypeUtils;
import foundation.polar.gratify.utils.StreamUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Encoder for {@link ResourceRegion ResourceRegions}.
 *
 * @author Brian Clozel
 */
public class ResourceRegionEncoder extends AbstractEncoder<ResourceRegion> {

   /**
    * The default buffer size used by the encoder.
    */
   public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

   /**
    * The hint key that contains the boundary string.
    */
   public static final String BOUNDARY_STRING_HINT = ResourceRegionEncoder.class.getName() + ".boundaryString";

   private final int bufferSize;


   public ResourceRegionEncoder() {
      this(DEFAULT_BUFFER_SIZE);
   }

   public ResourceRegionEncoder(int bufferSize) {
      super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
      AssertUtils.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
      this.bufferSize = bufferSize;
   }

   @Override
   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
      return super.canEncode(elementType, mimeType)
         && ResourceRegion.class.isAssignableFrom(elementType.toClass());
   }

   @Override
   public Flux<DataBuffer> encode(Publisher<? extends ResourceRegion> input,
                                  DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
                                  @Nullable Map<String, Object> hints) {

      AssertUtils.notNull(input, "'inputStream' must not be null");
      AssertUtils.notNull(bufferFactory, "'bufferFactory' must not be null");
      AssertUtils.notNull(elementType, "'elementType' must not be null");

      if (input instanceof Mono) {
         return Mono.from(input)
            .flatMapMany(region -> {
               if (!region.getResource().isReadable()) {
                  return Flux.error(new EncodingException(
                     "Resource " + region.getResource() + " is not readable"));
               }
               return writeResourceRegion(region, bufferFactory, hints);
            });
      }
      else {
         final String boundaryString = Hints.getRequiredHint(hints, BOUNDARY_STRING_HINT);
         byte[] startBoundary = toAsciiBytes("\r\n--" + boundaryString + "\r\n");
         byte[] contentType = mimeType != null ? toAsciiBytes("Content-Type: " + mimeType + "\r\n") : new byte[0];

         return Flux.from(input)
            .concatMap(region -> {
               if (!region.getResource().isReadable()) {
                  return Flux.error(new EncodingException(
                     "Resource " + region.getResource() + " is not readable"));
               }
               Flux<DataBuffer> prefix = Flux.just(
                  bufferFactory.wrap(startBoundary),
                  bufferFactory.wrap(contentType),
                  bufferFactory.wrap(getContentRangeHeader(region))); // only wrapping, no allocation

               return prefix.concatWith(writeResourceRegion(region, bufferFactory, hints));
            })
            .concatWithValues(getRegionSuffix(bufferFactory, boundaryString));
      }
      // No doOnDiscard (no caching after DataBufferUtils#read)
   }

   private Flux<DataBuffer> writeResourceRegion(
      ResourceRegion region, DataBufferFactory bufferFactory, @Nullable Map<String, Object> hints) {

      Resource resource = region.getResource();
      long position = region.getPosition();
      long count = region.getCount();

      if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
         logger.debug(Hints.getLogPrefix(hints) +
            "Writing region " + position + "-" + (position + count) + " of [" + resource + "]");
      }

      Flux<DataBuffer> in = DataBufferUtils.read(resource, position, bufferFactory, this.bufferSize);
      return DataBufferUtils.takeUntilByteCount(in, count);
   }

   private DataBuffer getRegionSuffix(DataBufferFactory bufferFactory, String boundaryString) {
      byte[] endBoundary = toAsciiBytes("\r\n--" + boundaryString + "--");
      return bufferFactory.wrap(endBoundary);
   }

   private byte[] toAsciiBytes(String in) {
      return in.getBytes(StandardCharsets.US_ASCII);
   }

   private byte[] getContentRangeHeader(ResourceRegion region) {
      long start = region.getPosition();
      long end = start + region.getCount() - 1;
      OptionalLong contentLength = contentLength(region.getResource());
      if (contentLength.isPresent()) {
         long length = contentLength.getAsLong();
         return toAsciiBytes("Content-Range: bytes " + start + '-' + end + '/' + length + "\r\n\r\n");
      }
      else {
         return toAsciiBytes("Content-Range: bytes " + start + '-' + end + "\r\n\r\n");
      }
   }

   /**
    * Determine, if possible, the contentLength of the given resource without reading it.
    * @param resource the resource instance
    * @return the contentLength of the resource
    */
   private OptionalLong contentLength(Resource resource) {
      // Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
      // Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
      if (InputStreamResource.class != resource.getClass()) {
         try {
            return OptionalLong.of(resource.contentLength());
         }
         catch (IOException ignored) {
         }
      }
      return OptionalLong.empty();
   }

}

