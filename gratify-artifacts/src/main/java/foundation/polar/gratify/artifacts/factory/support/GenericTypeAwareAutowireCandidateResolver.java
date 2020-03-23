package foundation.polar.gratify.artifacts.factory.support;


import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.ArtifactFactoryAware;
import foundation.polar.gratify.artifacts.factory.FactoryArtifact;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.ConfigurableListableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.config.DependencyDescriptor;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Basic {@link AutowireCandidateResolver} that performs a full generic type
 * match with the candidate's type if the dependency is declared as a generic type
 * (e.g. Repository&lt;Customer&gt;).
 *
 * <p>This is the base class for
 * {@link foundation.polar.gratify.artifacts.factory.annotation.QualifierAnnotationAutowireCandidateResolver},
 * providing an implementation all non-annotation-based resolution steps at this level.
 *
 * @author Juergen Hoeller
 */
public class GenericTypeAwareAutowireCandidateResolver extends SimpleAutowireCandidateResolver
   implements ArtifactFactoryAware {

   @Nullable
   private ArtifactFactory artifactFactory;

   @Override
   public void setArtifactFactory(ArtifactFactory artifactFactory) {
      this.artifactFactory = artifactFactory;
   }

   @Nullable
   protected final ArtifactFactory getArtifactFactory() {
      return this.artifactFactory;
   }


   @Override
   public boolean isAutowireCandidate(ArtifactDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      if (!super.isAutowireCandidate(bdHolder, descriptor)) {
         // If explicitly false, do not proceed with any other checks...
         return false;
      }
      return checkGenericTypeMatch(bdHolder, descriptor);
   }

   /**
    * Match the given dependency type with its generic type information against the given
    * candidate bean definition.
    */
   protected boolean checkGenericTypeMatch(ArtifactDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      ResolvableType dependencyType = descriptor.getResolvableType();
      if (dependencyType.getType() instanceof Class) {
         // No generic type -> we know it's a Class type-match, so no need to check again.
         return true;
      }

      ResolvableType targetType = null;
      boolean cacheType = false;
      RootArtifactDefinition rbd = null;
      if (bdHolder.getArtifactDefinition() instanceof RootArtifactDefinition) {
         rbd = (RootArtifactDefinition) bdHolder.getArtifactDefinition();
      }
      if (rbd != null) {
         targetType = rbd.targetType;
         if (targetType == null) {
            cacheType = true;
            // First, check factory method return type, if applicable
            targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
            if (targetType == null) {
               RootArtifactDefinition dbd = getResolvedDecoratedDefinition(rbd);
               if (dbd != null) {
                  targetType = dbd.targetType;
                  if (targetType == null) {
                     targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
                  }
               }
            }
         }
      }

      if (targetType == null) {
         // Regular case: straight bean instance, with ArtifactFactory available.
         if (this.artifactFactory != null) {
            Class<?> beanType = this.artifactFactory.getType(bdHolder.getArtifactName());
            if (beanType != null) {
               targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
            }
         }
         // Fallback: no ArtifactFactory set, or no type resolvable through it
         // -> best-effort match against the target class if applicable.
         if (targetType == null && rbd != null && rbd.hasArtifactClass() && rbd.getFactoryMethodName() == null) {
            Class<?> beanClass = rbd.getArtifactClass();
            if (!FactoryArtifact.class.isAssignableFrom(beanClass)) {
               targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
            }
         }
      }

      if (targetType == null) {
         return true;
      }
      if (cacheType) {
         rbd.targetType = targetType;
      }
      if (descriptor.fallbackMatchAllowed() &&
         (targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
         // Fallback matches allow unresolvable generics, e.g. plain HashMap to Map<String,String>;
         // and pragmatically also java.util.Properties to any Map (since despite formally being a
         // Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
         return true;
      }
      // Full check for complex generic type match...
      return dependencyType.isAssignableFrom(targetType);
   }

   @Nullable
   protected RootArtifactDefinition getResolvedDecoratedDefinition(RootArtifactDefinition rbd) {
      ArtifactDefinitionHolder decDef = rbd.getDecoratedDefinition();
      if (decDef != null && this.artifactFactory instanceof ConfigurableListableArtifactFactory) {
         ConfigurableListableArtifactFactory clbf = (ConfigurableListableArtifactFactory) this.artifactFactory;
         if (clbf.containsArtifactDefinition(decDef.getArtifactName())) {
            ArtifactDefinition dbd = clbf.getMergedArtifactDefinition(decDef.getArtifactName());
            if (dbd instanceof RootArtifactDefinition) {
               return (RootArtifactDefinition) dbd;
            }
         }
      }
      return null;
   }

   @Nullable
   protected ResolvableType getReturnTypeForFactoryMethod(RootArtifactDefinition rbd, DependencyDescriptor descriptor) {
      // Should typically be set for any kind of factory method, since the ArtifactFactory
      // pre-resolves them before reaching out to the AutowireCandidateResolver...
      ResolvableType returnType = rbd.factoryMethodReturnType;
      if (returnType == null) {
         Method factoryMethod = rbd.getResolvedFactoryMethod();
         if (factoryMethod != null) {
            returnType = ResolvableType.forMethodReturnType(factoryMethod);
         }
      }
      if (returnType != null) {
         Class<?> resolvedClass = returnType.resolve();
         if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
            // Only use factory method metadata if the return type is actually expressive enough
            // for our dependency. Otherwise, the returned instance type may have matched instead
            // in case of a singleton instance having been registered with the container already.
            return returnType;
         }
      }
      return null;
   }

}
