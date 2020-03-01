package foundation.polar.gratify.utils.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory for common {@link Log} delegates with Spring's logging conventions.
 *
 * <p>Mainly for internal use within the framework with Apache Commons Logging,
 * typically in the form of the {@code spring-jcl} bridge but also compatible
 * with other Commons Logging bridges.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see org.apache.commons.logging.LogFactory
 */
public final class LogDelegateFactory {
   private LogDelegateFactory() {}

   /**
    * Create a composite logger that delegates to a primary or falls back on a
    * secondary logger if logging for the primary logger is not enabled.
    * <p>This may be used for fallback logging from lower-level packages that
    * logically should log together with some higher-level package but the two
    * don't happen to share a suitable parent package (e.g. logging for the web
    * and lower-level http and codec packages). For such cases the primary
    * (class-based) logger can be wrapped with a shared fallback logger.
    * @param primaryLogger primary logger to try first
    * @param secondaryLogger secondary logger
    * @param tertiaryLoggers optional vararg of further fallback loggers
    * @return the resulting composite logger for the related categories
    */
   public static Log getCompositeLog(Log primaryLogger, Log secondaryLogger, Log... tertiaryLoggers) {
      List<Log> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
      loggers.add(primaryLogger);
      loggers.add(secondaryLogger);
      Collections.addAll(loggers, tertiaryLoggers);
      return new CompositeLog(loggers);
   }

   /**
    * Create a "hidden" logger whose name is intentionally prefixed with "_"
    * because its output is either too verbose or otherwise deemed as optional
    * or unnecessary to see at any log level by default under the normal package
    * based log hierarchy.
    * @param clazz the class for which to create a logger
    * @return a logger for the hidden category ("_" + fully-qualified class name)
    */
   public static Log getHiddenLog(Class<?> clazz) {
      return LogFactory.getLog("_" + clazz.getName());
   }
}
