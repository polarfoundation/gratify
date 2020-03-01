package foundation.polar.gratify.utils;

import org.apache.commons.logging.Log;
import java.io.Writer;

/**
 * {@code java.io.Writer} adapter for a Commons Logging {@code Log}.
 *
 * @author Juergen Hoeller
 */
public class CommonsLogWriter extends Writer {
   private final Log logger;

   private final StringBuilder buffer = new StringBuilder();

   /**
    * Create a new CommonsLogWriter for the given Commons Logging logger.
    * @param logger the Commons Logging logger to write to
    */
   public CommonsLogWriter(Log logger) {
      AssertUtils.notNull(logger, "Logger must not be null");
      this.logger = logger;
   }

   public void write(char ch) {
      if (ch == '\n' && this.buffer.length() > 0) {
         logger.debug(this.buffer.toString());
         this.buffer.setLength(0);
      }
      else {
         this.buffer.append(ch);
      }
   }

   @Override
   public void write(char[] buffer, int offset, int length) {
      for (int i = 0; i < length; i++) {
         write(buffer[offset + i]);
      }
   }

   @Override
   public void flush() {}

   @Override
   public void close() {}
}
