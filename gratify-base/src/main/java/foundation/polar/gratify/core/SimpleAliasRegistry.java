package foundation.polar.gratify.core;

import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import foundation.polar.gratify.utils.StringValueResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of the {@link AliasRegistry} interface.
 * <p>Serves as base class for
 * {@link foundation.polar.gratify.artifacts.factory.support.ArtifactDefinitionRegistry}
 * implementations.
 *
 * @author Juergen Hoeller
 * @author Qimiao Chen
 */
public class SimpleAliasRegistry implements AliasRegistry {
   /** Logger available to subclasses. */
   protected final Log logger = LogFactory.getLog(getClass());

   /** Map from alias to canonical name. */
   private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);

   @Override
   public void registerAlias(String name, String alias) {
      AssertUtils.hasText(name, "'name' must not be empty");
      AssertUtils.hasText(alias, "'alias' must not be empty");
      synchronized (this.aliasMap) {
         if (alias.equals(name)) {
            this.aliasMap.remove(alias);
            if (logger.isDebugEnabled()) {
               logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
            }
         }
         else {
            String registeredName = this.aliasMap.get(alias);
            if (registeredName != null) {
               if (registeredName.equals(name)) {
                  // An existing alias - no need to re-register
                  return;
               }
               if (!allowAliasOverriding()) {
                  throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
                     name + "': It is already registered for name '" + registeredName + "'.");
               }
               if (logger.isDebugEnabled()) {
                  logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
                     registeredName + "' with new target name '" + name + "'");
               }
            }
            checkForAliasCircle(name, alias);
            this.aliasMap.put(alias, name);
            if (logger.isTraceEnabled()) {
               logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
            }
         }
      }
   }

   /**
    * Determine whether alias overriding is allowed.
    * <p>Default is {@code true}.
    */
   protected boolean allowAliasOverriding() {
      return true;
   }

   /**
    * Determine whether the given name has the given alias registered.
    * @param name the name to check
    * @param alias the alias to look for
    */
   public boolean hasAlias(String name, String alias) {
      String registeredName = this.aliasMap.get(alias);
      return ObjectUtils.nullSafeEquals(registeredName, name) || (registeredName != null
         && hasAlias(name, registeredName));
   }

   @Override
   public void removeAlias(String alias) {
      synchronized (this.aliasMap) {
         String name = this.aliasMap.remove(alias);
         if (name == null) {
            throw new IllegalStateException("No alias '" + alias + "' registered");
         }
      }
   }

   @Override
   public boolean isAlias(String name) {
      return this.aliasMap.containsKey(name);
   }

   @Override
   public String[] getAliases(String name) {
      List<String> result = new ArrayList<>();
      synchronized (this.aliasMap) {
         retrieveAliases(name, result);
      }
      return StringUtils.toStringArray(result);
   }

   /**
    * Transitively retrieve all aliases for the given name.
    * @param name the target name to find aliases for
    * @param result the resulting aliases list
    */
   private void retrieveAliases(String name, List<String> result) {
      this.aliasMap.forEach((alias, registeredName) -> {
         if (registeredName.equals(name)) {
            result.add(alias);
            retrieveAliases(alias, result);
         }
      });
   }

   /**
    * Resolve all alias target names and aliases registered in this
    * registry, applying the given {@link StringValueResolver} to them.
    * <p>The value resolver may for example resolve placeholders
    * in target bean names and even in alias names.
    * @param valueResolver the StringValueResolver to apply
    */
   public void resolveAliases(StringValueResolver valueResolver) {
      AssertUtils.notNull(valueResolver, "StringValueResolver must not be null");
      synchronized (this.aliasMap) {
         Map<String, String> aliasCopy = new HashMap<>(this.aliasMap);
         aliasCopy.forEach((alias, registeredName) -> {
            String resolvedAlias = valueResolver.resolveStringValue(alias);
            String resolvedName = valueResolver.resolveStringValue(registeredName);
            if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
               this.aliasMap.remove(alias);
            }
            else if (!resolvedAlias.equals(alias)) {
               String existingName = this.aliasMap.get(resolvedAlias);
               if (existingName != null) {
                  if (existingName.equals(resolvedName)) {
                     // Pointing to existing alias - just remove placeholder
                     this.aliasMap.remove(alias);
                     return;
                  }
                  throw new IllegalStateException(
                     "Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias +
                        "') for name '" + resolvedName + "': It is already registered for name '" +
                        registeredName + "'.");
               }
               checkForAliasCircle(resolvedName, resolvedAlias);
               this.aliasMap.remove(alias);
               this.aliasMap.put(resolvedAlias, resolvedName);
            }
            else if (!registeredName.equals(resolvedName)) {
               this.aliasMap.put(alias, resolvedName);
            }
         });
      }
   }

   /**
    * Check whether the given name points back to the given alias as an alias
    * in the other direction already, catching a circular reference upfront
    * and throwing a corresponding IllegalStateException.
    * @param name the candidate name
    * @param alias the candidate alias
    * @see #registerAlias
    * @see #hasAlias
    */
   protected void checkForAliasCircle(String name, String alias) {
      if (hasAlias(alias, name)) {
         throw new IllegalStateException("Cannot register alias '" + alias +
            "' for name '" + name + "': Circular reference - '" +
            name + "' is a direct or indirect alias for '" + alias + "' already");
      }
   }

   /**
    * Determine the raw name, resolving aliases to canonical names.
    * @param name the user-specified name
    * @return the transformed name
    */
   public String canonicalName(String name) {
      String canonicalName = name;
      // Handle aliasing...
      String resolvedName;
      do {
         resolvedName = this.aliasMap.get(canonicalName);
         if (resolvedName != null) {
            canonicalName = resolvedName;
         }
      }
      while (resolvedName != null);
      return canonicalName;
   }
}
