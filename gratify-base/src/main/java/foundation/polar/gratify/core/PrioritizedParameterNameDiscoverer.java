package foundation.polar.gratify.core;


import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link ParameterNameDiscoverer} implementation that tries several discoverer
 * delegates in succession. Those added first in the {@code addDiscoverer} method
 * have highest priority. If one returns {@code null}, the next will be tried.
 *
 * <p>The default behavior is to return {@code null} if no discoverer matches.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PrioritizedParameterNameDiscoverer implements ParameterNameDiscoverer {

   private final List<ParameterNameDiscoverer> parameterNameDiscoverers = new LinkedList<>();

   /**
    * Add a further {@link ParameterNameDiscoverer} delegate to the list of
    * discoverers that this {@code PrioritizedParameterNameDiscoverer} checks.
    */
   public void addDiscoverer(ParameterNameDiscoverer pnd) {
      this.parameterNameDiscoverers.add(pnd);
   }


   @Override
   @Nullable
   public String[] getParameterNames(Method method) {
      for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
         String[] result = pnd.getParameterNames(method);
         if (result != null) {
            return result;
         }
      }
      return null;
   }

   @Override
   @Nullable
   public String[] getParameterNames(Constructor<?> ctor) {
      for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
         String[] result = pnd.getParameterNames(ctor);
         if (result != null) {
            return result;
         }
      }
      return null;
   }

}
