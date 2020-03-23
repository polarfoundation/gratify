package foundation.polar.gratify.artifacts.factory.parsing;


import foundation.polar.gratify.core.io.Resource;
import org.checkerframework.checker.nullness.qual.Nullable;


/**
 * Simple implementation of {@link SourceExtractor} that returns {@code null}
 * as the source metadata.
 *
 * <p>This is the default implementation and prevents too much metadata from being
 * held in memory during normal (non-tooled) runtime usage.
 *
 * @author Rob Harrop
 */
public class NullSourceExtractor implements SourceExtractor {

   /**
    * This implementation simply returns {@code null} for any input.
    */
   @Override
   @Nullable
   public Object extractSource(Object sourceCandidate, @Nullable Resource definitionResource) {
      return null;
   }

}
