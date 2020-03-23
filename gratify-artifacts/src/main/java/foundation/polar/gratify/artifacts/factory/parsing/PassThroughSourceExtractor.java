package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.core.io.Resource;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Simple {@link SourceExtractor} implementation that just passes
 * the candidate source metadata object through for attachment.
 *
 * <p>Using this implementation means that tools will get raw access to the
 * underlying configuration source metadata provided by the tool.
 *
 * <p>This implementation <strong>should not</strong> be used in a production
 * application since it is likely to keep too much metadata in memory
 * (unnecessarily).
 *
 * @author Rob Harrop
 */
public class PassThroughSourceExtractor implements SourceExtractor {

   /**
    * Simply returns the supplied {@code sourceCandidate} as-is.
    * @param sourceCandidate the source metadata
    * @return the supplied {@code sourceCandidate}
    */
   @Override
   public Object extractSource(Object sourceCandidate, @Nullable Resource definingResource) {
      return sourceCandidate;
   }

}