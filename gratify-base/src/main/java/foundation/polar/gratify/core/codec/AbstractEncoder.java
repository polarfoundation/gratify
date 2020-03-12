package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.MimeType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract base class for {@link Decoder} implementations.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @param <T> the element type
 */
public abstract class AbstractEncoder<T> implements Encoder<T> {

   private final List<MimeType> encodableMimeTypes;

   protected Log logger = LogFactory.getLog(getClass());

   protected AbstractEncoder(MimeType... supportedMimeTypes) {
      this.encodableMimeTypes = Arrays.asList(supportedMimeTypes);
   }

   /**
    * Set an alternative logger to use than the one based on the class name.
    * @param logger the logger to use
    */
   public void setLogger(Log logger) {
      this.logger = logger;
   }

   /**
    * Return the currently configured Logger.
    */
   public Log getLogger() {
      return logger;
   }

   @Override
   public List<MimeType> getEncodableMimeTypes() {
      return this.encodableMimeTypes;
   }

   @Override
   public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
      if (mimeType == null) {
         return true;
      }
      for (MimeType candidate : this.encodableMimeTypes) {
         if (candidate.isCompatibleWith(mimeType)) {
            return true;
         }
      }
      return false;
   }

}
