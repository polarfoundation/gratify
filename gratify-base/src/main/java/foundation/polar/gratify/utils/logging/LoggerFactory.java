package foundation.polar.gratify.utils.logging;

import org.apache.commons.logging.Log;

public abstract class LoggerFactory {

   /**
    * Convenience method to return a named logger.
    * @param clazz containing Class from which a log name will be derived
    */
   public static Log getLog(Class<?> clazz) {
      return getLog(clazz.getName());
   }

   /**
    * Convenience method to return a named logger.
    * @param name logical name of the <code>Log</code> instance to be returned
    */
   public static Log getLog(String name) {
      return LogAdapter.createLog(name);
   }
}
