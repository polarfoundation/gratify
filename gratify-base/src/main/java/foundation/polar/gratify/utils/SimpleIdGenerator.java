package foundation.polar.gratify.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple {@link IdGenerator} that starts at 1 and increments by 1 with each call.
 *
 * @author Rossen Stoyanchev
 */
public class SimpleIdGenerator implements IdGenerator {
   private final AtomicLong mostSigBits = new AtomicLong(0);

   private final AtomicLong leastSigBits = new AtomicLong(0);

   @Override
   public UUID generateId() {
      long leastSigBits = this.leastSigBits.incrementAndGet();
      if (leastSigBits == 0) {
         this.mostSigBits.incrementAndGet();
      }
      return new UUID(this.mostSigBits.get(), leastSigBits);
   }
}
