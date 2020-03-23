package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * Extension of MethodOverride that represents an arbitrary
 * override of a method by the IoC container.
 *
 * <p>Any non-final method can be overridden, irrespective of its
 * parameters and return types.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class ReplaceOverride extends MethodOverride {

   private final String methodReplacerArtifactName;

   private List<String> typeIdentifiers = new LinkedList<>();

   /**
    * Construct a new ReplaceOverride.
    * @param methodName the name of the method to override
    * @param methodReplacerArtifactName the bean name of the MethodReplacer
    */
   public ReplaceOverride(String methodName, String methodReplacerArtifactName) {
      super(methodName);
      AssertUtils.notNull(methodName, "Method replacer bean name must not be null");
      this.methodReplacerArtifactName = methodReplacerArtifactName;
   }
   
   /**
    * Return the name of the bean implementing MethodReplacer.
    */
   public String getMethodReplacerArtifactName() {
      return this.methodReplacerArtifactName;
   }

   /**
    * Add a fragment of a class string, like "Exception"
    * or "java.lang.Exc", to identify a parameter type.
    * @param identifier a substring of the fully qualified class name
    */
   public void addTypeIdentifier(String identifier) {
      this.typeIdentifiers.add(identifier);
   }

   @Override
   public boolean matches(Method method) {
      if (!method.getName().equals(getMethodName())) {
         return false;
      }
      if (!isOverloaded()) {
         // Not overloaded: don't worry about arg type matching...
         return true;
      }
      // If we get here, we need to insist on precise argument matching...
      if (this.typeIdentifiers.size() != method.getParameterCount()) {
         return false;
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      for (int i = 0; i < this.typeIdentifiers.size(); i++) {
         String identifier = this.typeIdentifiers.get(i);
         if (!parameterTypes[i].getName().contains(identifier)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (!(other instanceof ReplaceOverride) || !super.equals(other)) {
         return false;
      }
      ReplaceOverride that = (ReplaceOverride) other;
      return (ObjectUtils.nullSafeEquals(this.methodReplacerArtifactName, that.methodReplacerArtifactName) &&
         ObjectUtils.nullSafeEquals(this.typeIdentifiers, that.typeIdentifiers));
   }

   @Override
   public int hashCode() {
      int hashCode = super.hashCode();
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.methodReplacerArtifactName);
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.typeIdentifiers);
      return hashCode;
   }

   @Override
   public String toString() {
      return "Replace override for method '" + getMethodName() + "'";
   }
}
