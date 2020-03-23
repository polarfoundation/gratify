package foundation.polar.gratify.artifacts.support;


/**
 * Definition for sorting bean instances by a property.
 *
 * @author Juergen Hoeller
 */
public interface SortDefinition {

   /**
    * Return the name of the bean property to compare.
    * Can also be a nested bean property path.
    */
   String getProperty();

   /**
    * Return whether upper and lower case in String values should be ignored.
    */
   boolean isIgnoreCase();

   /**
    * Return whether to sort ascending (true) or descending (false).
    */
   boolean isAscending();
}
