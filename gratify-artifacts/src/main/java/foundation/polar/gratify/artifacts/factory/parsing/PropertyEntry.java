package foundation.polar.gratify.artifacts.factory.parsing;


import foundation.polar.gratify.utils.StringUtils;

/**
 * {@link ParseState} entry representing a JavaBean property.
 *
 * @author Rob Harrop
 */
public class PropertyEntry implements ParseState.Entry {

   private final String name;

   /**
    * Creates a new instance of the {@link PropertyEntry} class.
    * @param name the name of the JavaBean property represented by this instance
    * @throws IllegalArgumentException if the supplied {@code name} is {@code null}
    * or consists wholly of whitespace
    */
   public PropertyEntry(String name) {
      if (!StringUtils.hasText(name)) {
         throw new IllegalArgumentException("Invalid property name '" + name + "'.");
      }
      this.name = name;
   }

   @Override
   public String toString() {
      return "Property '" + this.name + "'";
   }

}
