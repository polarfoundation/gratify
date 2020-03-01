package foundation.polar.gratify.utils.logging;

import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractLogger implements Logger, Serializable {
   private static final long serialVersionUID = -6382972526573193470L;
   static final String EXCEPTION_MESSAGE = "Unexpected exception:";
   private final String name;

   protected AbstractLogger(String name) {
      Objects.requireNonNull(name);
      this.name = name;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public boolean isEnabled(LogLevel level) {
      switch (level) {
         case TRACE:
            return isTraceEnabled();
         case DEBUG:
            return isDebugEnabled();
         case INFO:
            return isInfoEnabled();
         case WARN:
            return isWarnEnabled();
         case ERROR:
            return isErrorEnabled();
         default:
            throw new Error();
      }
   }

   @Override
   public void trace(Throwable t) {
      trace(EXCEPTION_MESSAGE, t);
   }

   @Override
   public void debug(Throwable t) {
      debug(EXCEPTION_MESSAGE, t);
   }

   @Override
   public void info(Throwable t) {
      info(EXCEPTION_MESSAGE, t);
   }

   @Override
   public void warn(Throwable t) {
      warn(EXCEPTION_MESSAGE, t);
   }

   @Override
   public void error(Throwable t) {
      error(EXCEPTION_MESSAGE, t);
   }

   @Override
   public void log(LogLevel level, String msg, Throwable cause) {
      switch (level) {
         case TRACE:
            trace(msg, cause);
            break;
         case DEBUG:
            debug(msg, cause);
            break;
         case INFO:
            info(msg, cause);
            break;
         case WARN:
            warn(msg, cause);
            break;
         case ERROR:
            error(msg, cause);
            break;
         default:
            throw new Error();
      }
   }

   @Override
   public void log(LogLevel level, Throwable cause) {
      switch (level) {
         case TRACE:
            trace(cause);
            break;
         case DEBUG:
            debug(cause);
            break;
         case INFO:
            info(cause);
            break;
         case WARN:
            warn(cause);
            break;
         case ERROR:
            error(cause);
            break;
         default:
            throw new Error();
      }
   }

   @Override
   public void log(LogLevel level, String msg) {
      switch (level) {
         case TRACE:
            trace(msg);
            break;
         case DEBUG:
            debug(msg);
            break;
         case INFO:
            info(msg);
            break;
         case WARN:
            warn(msg);
            break;
         case ERROR:
            error(msg);
            break;
         default:
            throw new Error();
      }
   }

   @Override
   public void log(LogLevel level, String format, Object arg) {
      switch (level) {
         case TRACE:
            trace(format, arg);
            break;
         case DEBUG:
            debug(format, arg);
            break;
         case INFO:
            info(format, arg);
            break;
         case WARN:
            warn(format, arg);
            break;
         case ERROR:
            error(format, arg);
            break;
         default:
            throw new Error();
      }
   }

   @Override
   public void log(LogLevel level, String format, Object argA, Object argB) {
      switch (level) {
         case TRACE:
            trace(format, argA, argB);
            break;
         case DEBUG:
            debug(format, argA, argB);
            break;
         case INFO:
            info(format, argA, argB);
            break;
         case WARN:
            warn(format, argA, argB);
            break;
         case ERROR:
            error(format, argA, argB);
            break;
         default:
            throw new Error();
      }
   }

   @Override
   public void log(LogLevel level, String format, Object... arguments) {
      switch (level) {
         case TRACE:
            trace(format, arguments);
            break;
         case DEBUG:
            debug(format, arguments);
            break;
         case INFO:
            info(format, arguments);
            break;
         case WARN:
            warn(format, arguments);
            break;
         case ERROR:
            error(format, arguments);
            break;
         default:
            throw new Error();
      }
   }

   protected @Nullable Object readResolve() throws ObjectStreamException {
//      return LoggerFactory.getInstance(getName());
      return null;
   }

   @Override
   public String toString() {
      return StringUtils.simpleClassName(this) + '(' + getName() + ')';
   }
}
