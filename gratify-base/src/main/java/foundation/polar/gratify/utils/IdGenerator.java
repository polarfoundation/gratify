package foundation.polar.gratify.utils;

import java.util.UUID;

/**
 * Contract for generating universally unique identifiers {@link UUID (UUIDs)}.
 *
 * @author Rossen Stoyanchev
 */
public interface IdGenerator {
   /**
    * Generate a new identifier.
    * @return the generated identifier
    */
   UUID generateId();
}
