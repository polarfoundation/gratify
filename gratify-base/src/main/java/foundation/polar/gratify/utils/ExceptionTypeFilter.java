package foundation.polar.gratify.utils;

import java.util.Collection;

/**
 * An {@link InstanceFilter} implementation that handles exception types. A type
 * will match against a given candidate if it is assignable to that candidate.
 *
 * @author Stephane Nicoll
 */
public class ExceptionTypeFilter extends InstanceFilter<Class<? extends Throwable>> {
   public ExceptionTypeFilter(Collection<? extends Class<? extends Throwable>> includes,
                              Collection<? extends Class<? extends Throwable>> excludes, boolean matchIfEmpty) {

      super(includes, excludes, matchIfEmpty);
   }

   @Override
   protected boolean match(Class<? extends Throwable> instance, Class<? extends Throwable> candidate) {
      return candidate.isAssignableFrom(instance);
   }
}
