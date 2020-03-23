package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Tag collection class used to hold managed array elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

   /** Resolved element type for runtime creation of the target array. */
   @Nullable
   volatile Class<?> resolvedElementType;

   /**
    * Create a new managed array placeholder.
    * @param elementTypeName the target element type as a class name
    * @param size the size of the array
    */
   public ManagedArray(String elementTypeName, int size) {
      super(size);
      AssertUtils.notNull(elementTypeName, "elementTypeName must not be null");
      setElementTypeName(elementTypeName);
   }
}
