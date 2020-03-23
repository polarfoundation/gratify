package foundation.polar.gratify.core;

import foundation.polar.gratify.utils.AssertUtils;

/**
 * {@link InheritableThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 *
 * @author Juergen Hoeller
 *
 * @param <T> the value type
 * @see NamedThreadLocal
 */
public class NamedInheritableThreadLocal<T> extends InheritableThreadLocal<T> {
   private final String name;

   /**
    * Create a new NamedInheritableThreadLocal with the given name.
    * @param name a descriptive name for this ThreadLocal
    */
   public NamedInheritableThreadLocal(String name) {
      AssertUtils.hasText(name, "Name must not be empty");
      this.name = name;
   }

   @Override
   public String toString() {
      return this.name;
   }

}
