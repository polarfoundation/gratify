package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.utils.AssertUtils;

/**
 * {@link ParseState} entry representing a (possibly indexed)
 * constructor argument.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ConstructorArgumentEntry implements ParseState.Entry {
   private final int index;

   /**
    * Creates a new instance of the {@link ConstructorArgumentEntry} class
    * representing a constructor argument with a (currently) unknown index.
    */
   public ConstructorArgumentEntry() {
      this.index = -1;
   }

   /**
    * Creates a new instance of the {@link ConstructorArgumentEntry} class
    * representing a constructor argument at the supplied {@code index}.
    * @param index the index of the constructor argument
    * @throws IllegalArgumentException if the supplied {@code index}
    * is less than zero
    */
   public ConstructorArgumentEntry(int index) {
      AssertUtils.isTrue(index >= 0, "Constructor argument index must be greater than or equal to zero");
      this.index = index;
   }

   @Override
   public String toString() {
      return "Constructor-arg" + (this.index >= 0 ? " #" + this.index : "");
   }
}
