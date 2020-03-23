package foundation.polar.gratify.artifacts.factory.parsing;

import foundation.polar.gratify.utils.StringUtils;

/**
 * {@link ParseState} entry representing an autowire candidate qualifier.
 *
 * @author Mark Fisher
 */
public class QualifierEntry implements ParseState.Entry {

   private String typeName;

   public QualifierEntry(String typeName) {
      if (!StringUtils.hasText(typeName)) {
         throw new IllegalArgumentException("Invalid qualifier type '" + typeName + "'.");
      }
      this.typeName = typeName;
   }

   @Override
   public String toString() {
      return "Qualifier '" + this.typeName + "'";
   }

}
