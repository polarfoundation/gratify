package foundation.polar.gratify.artifacts;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Interface representing an object whose value set can be merged with
 * that of a parent object.
 *
 * @author Rob Harrop
 * @see foundation.polar.gratify.artifacts.factory.support.ManagedSet
 * @see foundation.polar.gratify.artifacts.factory.support.ManagedList
 * @see foundation.polar.gratify.artifacts.factory.support.ManagedMap
 * @see foundation.polar.gratify.artifacts.support.ManagedProperties
 */
public interface Mergeable {

   /**
    * Is merging enabled for this particular instance?
    */
   boolean isMergeEnabled();

   /**
    * Merge the current value set with that of the supplied object.
    * <p>The supplied object is considered the parent, and values in
    * the callee's value set must override those of the supplied object.
    * @param parent the object to merge with
    * @return the result of the merge operation
    * @throws IllegalArgumentException if the supplied parent is {@code null}
    * @throws IllegalStateException if merging is not enabled for this instance
    * (i.e. {@code mergeEnabled} equals {@code false}).
    */
   Object merge(@Nullable Object parent);

}
