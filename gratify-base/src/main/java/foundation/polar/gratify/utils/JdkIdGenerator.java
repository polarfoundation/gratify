package foundation.polar.gratify.utils;

import java.util.UUID;

/**
 * An {@link IdGenerator} that calls {@link java.util.UUID#randomUUID()}.
 *
 * @author Rossen Stoyanchev
 */
public class JdkIdGenerator implements IdGenerator {
   @Override
   public UUID generateId() {
      return UUID.randomUUID();
   }
}
