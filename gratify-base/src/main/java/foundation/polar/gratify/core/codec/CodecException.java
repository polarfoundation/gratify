package foundation.polar.gratify.core.codec;

import foundation.polar.gratify.core.NestedRuntimeException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * General error that indicates a problem while encoding and decoding to and
 * from an Object stream.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("serial")
public class CodecException extends NestedRuntimeException {

   /**
    * Create a new CodecException.
    * @param msg the detail message
    */
   public CodecException(String msg) {
      super(msg);
   }

   /**
    * Create a new CodecException.
    * @param msg the detail message
    * @param cause root cause for the exception, if any
    */
   public CodecException(String msg, @Nullable Throwable cause) {
      super(msg, cause);
   }

}
