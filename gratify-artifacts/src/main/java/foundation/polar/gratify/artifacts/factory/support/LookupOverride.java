package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents an override of a method that looks up an object in the same IoC context.
 *
 * <p>Methods eligible for lookup override must not have arguments.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class LookupOverride extends MethodOverride {

   @Nullable
   private final String artifactName;

   @Nullable
   private Method method;

   /**
    * Construct a new LookupOverride.
    * @param methodName the name of the method to override
    * @param artifactName the name of the bean in the current {@code ArtifactFactory}
    * that the overridden method should return (may be {@code null})
    */
   public LookupOverride(String methodName, @Nullable String artifactName) {
      super(methodName);
      this.artifactName = artifactName;
   }

   /**
    * Construct a new LookupOverride.
    * @param method the method to override
    * @param artifactName the name of the bean in the current {@code ArtifactFactory}
    * that the overridden method should return (may be {@code null})
    */
   public LookupOverride(Method method, @Nullable String artifactName) {
      super(method.getName());
      this.method = method;
      this.artifactName = artifactName;
   }
   
   /**
    * Return the name of the bean that should be returned by this method.
    */
   @Nullable
   public String getArtifactName() {
      return this.artifactName;
   }

   /**
    * Match the specified method by {@link Method} reference or method name.
    * <p>For backwards compatibility reasons, in a scenario with overloaded
    * non-abstract methods of the given name, only the no-arg variant of a
    * method will be turned into a container-driven lookup method.
    * <p>In case of a provided {@link Method}, only straight matches will
    * be considered, usually demarcated by the {@code @Lookup} annotation.
    */
   @Override
   public boolean matches(Method method) {
      if (this.method != null) {
         return method.equals(this.method);
      }
      else {
         return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
            Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
      }
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (!(other instanceof LookupOverride) || !super.equals(other)) {
         return false;
      }
      LookupOverride that = (LookupOverride) other;
      return (ObjectUtils.nullSafeEquals(this.method, that.method) &&
         ObjectUtils.nullSafeEquals(this.artifactName, that.artifactName));
   }

   @Override
   public int hashCode() {
      return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.artifactName));
   }

   @Override
   public String toString() {
      return "LookupOverride for method '" + getMethodName() + "'";
   }

}
