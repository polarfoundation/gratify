package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactUtils;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactClassLoaderAware;
import foundation.polar.gratify.core.Ordered;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple {@link ArtifactFactoryPostProcessor} implementation that registers
 * custom {@link Scope Scope(s)} with the containing {@link ConfigurableArtifactFactory}.
 *
 * <p>Will register all of the supplied {@link #setScopes(java.util.Map) scopes}
 * with the {@link ConfigurableListableArtifactFactory} that is passed to the
 * {@link #postProcessArtifactFactory(ConfigurableListableArtifactFactory)} method.
 *
 * <p>This class allows for <i>declarative</i> registration of custom scopes.
 * Alternatively, consider implementing a custom {@link ArtifactFactoryPostProcessor}
 * that calls {@link ConfigurableArtifactFactory#registerScope} programmatically.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 *
 * @see ConfigurableArtifactFactory#registerScope
 */
public class CustomScopeConfigurer implements ArtifactFactoryPostProcessor, ArtifactClassLoaderAware, Ordered {
   @Nullable
   private Map<String, Object> scopes;

   private int order = Ordered.LOWEST_PRECEDENCE;

   @Nullable
   private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

   /**
    * Specify the custom scopes that are to be registered.
    * <p>The keys indicate the scope names (of type String); each value
    * is expected to be the corresponding custom {@link Scope} instance
    * or class name.
    */
   public void setScopes(Map<String, Object> scopes) {
      this.scopes = scopes;
   }

   /**
    * Add the given scope to this configurer's map of scopes.
    * @param scopeName the name of the scope
    * @param scope the scope implementation
    */
   public void addScope(String scopeName, Scope scope) {
      if (this.scopes == null) {
         this.scopes = new LinkedHashMap<>(1);
      }
      this.scopes.put(scopeName, scope);
   }

   public void setOrder(int order) {
      this.order = order;
   }

   @Override
   public int getOrder() {
      return this.order;
   }

   @Override
   public void setArtifactClassLoader(@Nullable ClassLoader beanClassLoader) {
      this.beanClassLoader = beanClassLoader;
   }
   
   @Override
   public void postProcessArtifactFactory(ConfigurableListableArtifactFactory beanFactory) throws ArtifactsException {
      if (this.scopes != null) {
         this.scopes.forEach((scopeKey, value) -> {
            if (value instanceof Scope) {
               beanFactory.registerScope(scopeKey, (Scope) value);
            }
            else if (value instanceof Class) {
               Class<?> scopeClass = (Class<?>) value;
               AssertUtils.isAssignable(Scope.class, scopeClass, "Invalid scope class");
               beanFactory.registerScope(scopeKey, (Scope) ArtifactUtils.instantiateClass(scopeClass));
            }
            else if (value instanceof String) {
               Class<?> scopeClass = ClassUtils.resolveClassName((String) value, this.beanClassLoader);
               AssertUtils.isAssignable(Scope.class, scopeClass, "Invalid scope class");
               beanFactory.registerScope(scopeKey, (Scope) ArtifactUtils.instantiateClass(scopeClass));
            }
            else {
               throw new IllegalArgumentException("Mapped value [" + value + "] for scope key [" +
                  scopeKey + "] is not an instance of required type [" + Scope.class.getName() +
                  "] or a corresponding Class or String value indicating a Scope implementation");
            }
         });
      }
   }
}
