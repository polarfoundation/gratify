package foundation.polar.gratify.artifacts.factory.parsing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Simple {@link ProblemReporter} implementation that exhibits fail-fast
 * behavior when errors are encountered.
 *
 * <p>The first error encountered results in a {@link ArtifactDefinitionParsingException}
 * being thrown.
 *
 * <p>Warnings are written to
 * {@link #setLogger(org.apache.commons.logging.Log) the log} for this class.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class FailFastProblemReporter implements ProblemReporter {

   private Log logger = LogFactory.getLog(getClass());

   /**
    * Set the {@link Log logger} that is to be used to report warnings.
    * <p>If set to {@code null} then a default {@link Log logger} set to
    * the name of the instance class will be used.
    * @param logger the {@link Log logger} that is to be used to report warnings
    */
   public void setLogger(@Nullable Log logger) {
      this.logger = (logger != null ? logger : LogFactory.getLog(getClass()));
   }

   /**
    * Throws a {@link ArtifactDefinitionParsingException} detailing the error
    * that has occurred.
    * @param problem the source of the error
    */
   @Override
   public void fatal(Problem problem) {
      throw new ArtifactDefinitionParsingException(problem);
   }

   /**
    * Throws a {@link ArtifactDefinitionParsingException} detailing the error
    * that has occurred.
    * @param problem the source of the error
    */
   @Override
   public void error(Problem problem) {
      throw new ArtifactDefinitionParsingException(problem);
   }

   /**
    * Writes the supplied {@link Problem} to the {@link Log} at {@code WARN} level.
    * @param problem the source of the warning
    */
   @Override
   public void warning(Problem problem) {
      logger.warn(problem, problem.getRootCause());
   }

}
