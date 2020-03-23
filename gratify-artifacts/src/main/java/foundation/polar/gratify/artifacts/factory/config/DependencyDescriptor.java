package foundation.polar.gratify.artifacts.factory.config;

import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.ArtifactFactory;
import foundation.polar.gratify.artifacts.factory.InjectionPoint;
import foundation.polar.gratify.artifacts.factory.NoUniqueArtifactDefinitionException;
import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.core.ParameterNameDiscoverer;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 * Descriptor for a specific dependency that is about to be injected.
 * Wraps a constructor parameter, a method parameter or a field,
 * allowing unified access to their metadata.
 *
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class DependencyDescriptor  extends InjectionPoint implements Serializable  {
   private final Class<?> declaringClass;

   @Nullable
   private String methodName;

   @Nullable
   private Class<?>[] parameterTypes;

   private int parameterIndex;

   @Nullable
   private String fieldName;

   private final boolean required;

   private final boolean eager;

   private int nestingLevel = 1;

   @Nullable
   private Class<?> containingClass;

   @Nullable
   private transient volatile ResolvableType resolvableType;

   @Nullable
   private transient volatile TypeDescriptor typeDescriptor;

   /**
    * Create a new descriptor for a method or constructor parameter.
    * Considers the dependency as 'eager'.
    * @param methodParameter the MethodParameter to wrap
    * @param required whether the dependency is required
    */
   public DependencyDescriptor(MethodParameter methodParameter, boolean required) {
      this(methodParameter, required, true);
   }

   /**
    * Create a new descriptor for a method or constructor parameter.
    * @param methodParameter the MethodParameter to wrap
    * @param required whether the dependency is required
    * @param eager whether this dependency is 'eager' in the sense of
    * eagerly resolving potential target beans for type matching
    */
   public DependencyDescriptor(MethodParameter methodParameter, boolean required, boolean eager) {
      super(methodParameter);

      this.declaringClass = methodParameter.getDeclaringClass();
      if (methodParameter.getMethod() != null) {
         this.methodName = methodParameter.getMethod().getName();
      }
      this.parameterTypes = methodParameter.getExecutable().getParameterTypes();
      this.parameterIndex = methodParameter.getParameterIndex();
      this.containingClass = methodParameter.getContainingClass();
      this.required = required;
      this.eager = eager;
   }

   /**
    * Create a new descriptor for a field.
    * Considers the dependency as 'eager'.
    * @param field the field to wrap
    * @param required whether the dependency is required
    */
   public DependencyDescriptor(Field field, boolean required) {
      this(field, required, true);
   }

   /**
    * Create a new descriptor for a field.
    * @param field the field to wrap
    * @param required whether the dependency is required
    * @param eager whether this dependency is 'eager' in the sense of
    * eagerly resolving potential target beans for type matching
    */
   public DependencyDescriptor(Field field, boolean required, boolean eager) {
      super(field);

      this.declaringClass = field.getDeclaringClass();
      this.fieldName = field.getName();
      this.required = required;
      this.eager = eager;
   }

   /**
    * Copy constructor.
    * @param original the original descriptor to create a copy from
    */
   public DependencyDescriptor(DependencyDescriptor original) {
      super(original);

      this.declaringClass = original.declaringClass;
      this.methodName = original.methodName;
      this.parameterTypes = original.parameterTypes;
      this.parameterIndex = original.parameterIndex;
      this.fieldName = original.fieldName;
      this.containingClass = original.containingClass;
      this.required = original.required;
      this.eager = original.eager;
      this.nestingLevel = original.nestingLevel;
   }

   /**
    * Return whether this dependency is required.
    * <p>Optional semantics are derived from Java 8's {@link java.util.Optional},
    * any variant of a parameter-level {@code Nullable} annotation (such as from
    * JSR-305 or the FindBugs set of annotations), or a language-level nullable
    * type declaration in Kotlin.
    */
   public boolean isRequired() {
      if (!this.required) {
         return false;
      }

      if (this.field != null) {
         return !(this.field.getType() == Optional.class || hasNullableAnnotation());
      }
      else {
         return !obtainMethodParameter().isOptional();
      }
   }

   /**
    * Check whether the underlying field is annotated with any variant of a
    * {@code Nullable} annotation, e.g. {@code javax.annotation.Nullable} or
    * {@code edu.umd.cs.findbugs.annotations.Nullable}.
    */
   private boolean hasNullableAnnotation() {
      for (Annotation ann : getAnnotations()) {
         if ("Nullable".equals(ann.annotationType().getSimpleName())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Return whether this dependency is 'eager' in the sense of
    * eagerly resolving potential target beans for type matching.
    */
   public boolean isEager() {
      return this.eager;
   }

   /**
    * Resolve the specified not-unique scenario: by default,
    * throwing a {@link NoUniqueArtifactDefinitionException}.
    * <p>Subclasses may override this to select one of the instances or
    * to opt out with no result at all through returning {@code null}.
    * @param type the requested bean type
    * @param matchingArtifacts a map of bean names and corresponding bean
    * instances which have been pre-selected for the given type
    * (qualifiers etc already applied)
    * @return a bean instance to proceed with, or {@code null} for none
    * @throws ArtifactsException in case of the not-unique scenario being fatal
    */
   @Nullable
   public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingArtifacts) throws ArtifactsException {
      throw new NoUniqueArtifactDefinitionException(type, matchingArtifacts.keySet());
   }

   /**
    * Resolve a shortcut for this dependency against the given factory, for example
    * taking some pre-resolved information into account.
    * <p>The resolution algorithm will first attempt to resolve a shortcut through this
    * method before going into the regular type matching algorithm across all beans.
    * Subclasses may override this method to improve resolution performance based on
    * pre-cached information while still receiving {@link InjectionPoint} exposure etc.
    * @param beanFactory the associated factory
    * @return the shortcut result if any, or {@code null} if none
    * @throws ArtifactsException if the shortcut could not be obtained
    */
   @Nullable
   public Object resolveShortcut(ArtifactFactory beanFactory) throws ArtifactsException {
      return null;
   }

   /**
    * Resolve the specified bean name, as a candidate result of the matching
    * algorithm for this dependency, to a bean instance from the given factory.
    * <p>The default implementation calls {@link ArtifactFactory#getArtifact(String)}.
    * Subclasses may provide additional arguments or other customizations.
    * @param beanName the bean name, as a candidate result for this dependency
    * @param requiredType the expected type of the bean (as an assertion)
    * @param beanFactory the associated factory
    * @return the bean instance (never {@code null})
    * @throws ArtifactsException if the bean could not be obtained
    *
    * @see ArtifactFactory#getArtifact(String)
    */
   public Object resolveCandidate(String beanName, Class<?> requiredType, ArtifactFactory beanFactory)
      throws ArtifactsException {

      return beanFactory.getArtifact(beanName);
   }

   /**
    * Increase this descriptor's nesting level.
    */
   public void increaseNestingLevel() {
      this.nestingLevel++;
      this.resolvableType = null;
      if (this.methodParameter != null) {
         this.methodParameter = this.methodParameter.nested();
      }
   }

   /**
    * Optionally set the concrete class that contains this dependency.
    * This may differ from the class that declares the parameter/field in that
    * it may be a subclass thereof, potentially substituting type variables.
    */
   public void setContainingClass(Class<?> containingClass) {
      this.containingClass = containingClass;
      this.resolvableType = null;
      if (this.methodParameter != null) {
         this.methodParameter = this.methodParameter.withContainingClass(containingClass);
      }
   }

   /**
    * Build a {@link ResolvableType} object for the wrapped parameter/field.
    */
   public ResolvableType getResolvableType() {
      ResolvableType resolvableType = this.resolvableType;
      if (resolvableType == null) {
         resolvableType = (this.field != null ?
            ResolvableType.forField(this.field, this.nestingLevel, this.containingClass) :
            ResolvableType.forMethodParameter(obtainMethodParameter()));
         this.resolvableType = resolvableType;
      }
      return resolvableType;
   }

   /**
    * Build a {@link TypeDescriptor} object for the wrapped parameter/field.
    */
   public TypeDescriptor getTypeDescriptor() {
      TypeDescriptor typeDescriptor = this.typeDescriptor;
      if (typeDescriptor == null) {
         typeDescriptor = (this.field != null ?
            new TypeDescriptor(getResolvableType(), getDependencyType(), getAnnotations()) :
            new TypeDescriptor(obtainMethodParameter()));
         this.typeDescriptor = typeDescriptor;
      }
      return typeDescriptor;
   }

   /**
    * Return whether a fallback match is allowed.
    * <p>This is {@code false} by default but may be overridden to return {@code true} in order
    * to suggest to an {@link foundation.polar.gratify.artifacts.factory.support.AutowireCandidateResolver}
    * that a fallback match is acceptable as well.
    */
   public boolean fallbackMatchAllowed() {
      return false;
   }

   /**
    * Return a variant of this descriptor that is intended for a fallback match.
    * @see #fallbackMatchAllowed()
    */
   public DependencyDescriptor forFallbackMatch() {
      return new DependencyDescriptor(this) {
         @Override
         public boolean fallbackMatchAllowed() {
            return true;
         }
      };
   }

   /**
    * Initialize parameter name discovery for the underlying method parameter, if any.
    * <p>This method does not actually try to retrieve the parameter name at
    * this point; it just allows discovery to happen when the application calls
    * {@link #getDependencyName()} (if ever).
    */
   public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
      if (this.methodParameter != null) {
         this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
      }
   }

   /**
    * Determine the name of the wrapped parameter/field.
    * @return the declared name (may be {@code null} if unresolvable)
    */
   @Nullable
   public String getDependencyName() {
      return (this.field != null ? this.field.getName() : obtainMethodParameter().getParameterName());
   }

   /**
    * Determine the declared (non-generic) type of the wrapped parameter/field.
    * @return the declared type (never {@code null})
    */
   public Class<?> getDependencyType() {
      if (this.field != null) {
         if (this.nestingLevel > 1) {
            Type type = this.field.getGenericType();
            for (int i = 2; i <= this.nestingLevel; i++) {
               if (type instanceof ParameterizedType) {
                  Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                  type = args[args.length - 1];
               }
            }
            if (type instanceof Class) {
               return (Class<?>) type;
            }
            else if (type instanceof ParameterizedType) {
               Type arg = ((ParameterizedType) type).getRawType();
               if (arg instanceof Class) {
                  return (Class<?>) arg;
               }
            }
            return Object.class;
         }
         else {
            return this.field.getType();
         }
      }
      else {
         return obtainMethodParameter().getNestedParameterType();
      }
   }

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!super.equals(other)) {
         return false;
      }
      DependencyDescriptor otherDesc = (DependencyDescriptor) other;
      return (this.required == otherDesc.required && this.eager == otherDesc.eager &&
         this.nestingLevel == otherDesc.nestingLevel && this.containingClass == otherDesc.containingClass);
   }

   @Override
   public int hashCode() {
      return (31 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.containingClass));
   }

   //---------------------------------------------------------------------
   // Serialization support
   //---------------------------------------------------------------------

   private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      // Rely on default serialization; just initialize state after deserialization.
      ois.defaultReadObject();

      // Restore reflective handles (which are unfortunately not serializable)
      try {
         if (this.fieldName != null) {
            this.field = this.declaringClass.getDeclaredField(this.fieldName);
         }
         else {
            if (this.methodName != null) {
               this.methodParameter = new MethodParameter(
                  this.declaringClass.getDeclaredMethod(this.methodName, this.parameterTypes), this.parameterIndex);
            }
            else {
               this.methodParameter = new MethodParameter(
                  this.declaringClass.getDeclaredConstructor(this.parameterTypes), this.parameterIndex);
            }
            for (int i = 1; i < this.nestingLevel; i++) {
               this.methodParameter = this.methodParameter.nested();
            }
         }
      }
      catch (Throwable ex) {
         throw new IllegalStateException("Could not find original class structure", ex);
      }
   }
}
