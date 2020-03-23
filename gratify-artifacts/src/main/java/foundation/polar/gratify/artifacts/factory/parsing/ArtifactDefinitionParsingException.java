package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.artifacts.factory.ArtifactDefinitionStoreException;

/**
 * Exception thrown when a bean definition reader encounters an error
 * during the parsing process.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 */
@SuppressWarnings("serial")
public class ArtifactDefinitionParsingException extends ArtifactDefinitionStoreException {
   /**
    * Create a new ArtifactDefinitionParsingException.
    * @param problem the configuration problem that was detected during the parsing process
    */
   public ArtifactDefinitionParsingException(Problem problem) {
      super(problem.getResourceDescription(), problem.toString(), problem.getRootCause());
   }
}

