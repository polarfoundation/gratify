package foundation.polar.gratify.utils.compiler;

@SuppressWarnings("serial")
public class UserError extends RuntimeException {
   public UserError(String message) {
      super(message);
      if (message == null) {
         throw new Error("Must have a detail message.");
      }
   }

   public UserError(String fmt, Object... args) {
      this(String.format(fmt, args));
   }
}
