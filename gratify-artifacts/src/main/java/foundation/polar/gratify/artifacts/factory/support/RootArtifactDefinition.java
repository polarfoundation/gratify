package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.MutablePropertyValues;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.ConstructorArgumentValues;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A root artifact definition represents the merged artifact definition that backs
 * a specific artifact in a ArtifactFactory at runtime. It might have been created
 * from multiple original artifact definitions that inherit from each other,
 * typically registered as {@link GenericArtifactDefinition GenericArtifactDefinitions}.
 * A root artifact definition is essentially the 'unified' artifact definition view at runtime.
 *
 * <p>Root artifact definitions may also be used for registering individual artifact definitions
 * in the configuration phase. However, the preferred way to register
 * artifact definitions programmatically is the {@link GenericArtifactDefinition} class.
 * GenericArtifactDefinition has the advantage that it allows to dynamically define
 * parent dependencies, not 'hard-coding' the role as a root artifact definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see GenericArtifactDefinition
 * @see ChildArtifactDefinition
 */
@SuppressWarnings("serial")
public class RootArtifactDefinition extends AbstractArtifactDefinition {
   @Nullable
   private ArtifactDefinitionHolder decoratedDefinition;

   @Nullable
   private AnnotatedElement qualifiedElement;

   /** Determines if the definition needs to be re-merged. */
   volatile boolean stale;

   boolean allowCaching = true;

   boolean isFactoryMethodUnique = false;

   @Nullable
   volatile ResolvableType targetType;

   /** Package-visible field for caching the determined Class of a given artifact definition. */
   @Nullable
   volatile Class<?> resolvedTargetType;

   /** Package-visible field for caching if the artifact is a factory artifact. */
   @Nullable
   volatile Boolean isFactoryArtifact;

   /** Package-visible field for caching the return type of a generically typed factory method. */
   @Nullable
   volatile ResolvableType factoryMethodReturnType;

   /** Package-visible field for caching a unique factory method candidate for introspection. */
   @Nullable
   volatile Method factoryMethodToIntrospect;

   /** Common lock for the four constructor fields below. */
   final Object constructorArgumentLock = new Object();

   /** Package-visible field for caching the resolved constructor or factory method. */
   @Nullable
   Executable resolvedConstructorOrFactoryMethod;

   /** Package-visible field that marks the constructor arguments as resolved. */
   boolean constructorArgumentsResolved = false;

   /** Package-visible field for caching fully resolved constructor arguments. */
   @Nullable
   Object[] resolvedConstructorArguments;

   /** Package-visible field for caching partly prepared constructor arguments. */
   @Nullable
   Object[] preparedConstructorArguments;

   /** Common lock for the two post-processing fields below. */
   final Object postProcessingLock = new Object();

   /** Package-visible field that indicates MergedArtifactDefinitionPostProcessor having been applied. */
   boolean postProcessed = false;

   /** Package-visible field that indicates a before-instantiation post-processor having kicked in. */
   @Nullable
   volatile Boolean beforeInstantiationResolved;

   @Nullable
   private Set<Member> externallyManagedConfigMembers;

   @Nullable
   private Set<String> externallyManagedInitMethods;

   @Nullable
   private Set<String> externallyManagedDestroyMethods;

   /**
    * Create a new RootArtifactDefinition, to be configured through its artifact
    * properties and configuration methods.
    * @see #setArtifactClass
    * @see #setScope
    * @see #setConstructorArgumentValues
    * @see #setPropertyValues
    */
   public RootArtifactDefinition() {
      super();
   }

   /**
    * Create a new RootArtifactDefinition for a singleton.
    * @param artifactClass the class of the artifact to instantiate
    * @see #setArtifactClass
    */
   public RootArtifactDefinition(@Nullable Class<?> artifactClass) {
      super();
      setArtifactClass(artifactClass);
   }

   /**
    * Create a new RootArtifactDefinition for a singleton artifact, constructing each instance
    * through calling the given supplier (possibly a lambda or method reference).
    * @param artifactClass the class of the artifact to instantiate
    * @param instanceSupplier the supplier to construct a artifact instance,
    * as an alternative to a declaratively specified factory method
    *
    * @see #setInstanceSupplier
    */
   public <T> RootArtifactDefinition(@Nullable Class<T> artifactClass, @Nullable Supplier<T> instanceSupplier) {
      super();
      setArtifactClass(artifactClass);
      setInstanceSupplier(instanceSupplier);
   }

   /**
    * Create a new RootArtifactDefinition for a scoped artifact, constructing each instance
    * through calling the given supplier (possibly a lambda or method reference).
    * @param artifactClass the class of the artifact to instantiate
    * @param scope the name of the corresponding scope
    * @param instanceSupplier the supplier to construct a artifact instance,
    * as an alternative to a declaratively specified factory method
    * @see #setInstanceSupplier
    */
   public <T> RootArtifactDefinition(@Nullable Class<T> artifactClass, String scope, @Nullable Supplier<T> instanceSupplier) {
      super();
      setArtifactClass(artifactClass);
      setScope(scope);
      setInstanceSupplier(instanceSupplier);
   }

   /**
    * Create a new RootArtifactDefinition for a singleton,
    * using the given autowire mode.
    * @param artifactClass the class of the artifact to instantiate
    * @param autowireMode by name or type, using the constants in this interface
    * @param dependencyCheck whether to perform a dependency check for objects
    * (not applicable to autowiring a constructor, thus ignored there)
    */
   public RootArtifactDefinition(@Nullable Class<?> artifactClass, int autowireMode, boolean dependencyCheck) {
      super();
      setArtifactClass(artifactClass);
      setAutowireMode(autowireMode);
      if (dependencyCheck && getResolvedAutowireMode() != AUTOWIRE_CONSTRUCTOR) {
         setDependencyCheck(DEPENDENCY_CHECK_OBJECTS);
      }
   }

   /**
    * Create a new RootArtifactDefinition for a singleton,
    * providing constructor arguments and property values.
    * @param artifactClass the class of the artifact to instantiate
    * @param cargs the constructor argument values to apply
    * @param pvs the property values to apply
    */
   public RootArtifactDefinition(@Nullable Class<?> artifactClass, @Nullable ConstructorArgumentValues cargs,
                             @Nullable MutablePropertyValues pvs) {

      super(cargs, pvs);
      setArtifactClass(artifactClass);
   }

   /**
    * Create a new RootArtifactDefinition for a singleton,
    * providing constructor arguments and property values.
    * <p>Takes a artifact class name to avoid eager loading of the artifact class.
    * @param artifactClassName the name of the class to instantiate
    */
   public RootArtifactDefinition(String artifactClassName) {
      setArtifactClassName(artifactClassName);
   }

   /**
    * Create a new RootArtifactDefinition for a singleton,
    * providing constructor arguments and property values.
    * <p>Takes a artifact class name to avoid eager loading of the artifact class.
    * @param artifactClassName the name of the class to instantiate
    * @param cargs the constructor argument values to apply
    * @param pvs the property values to apply
    */
   public RootArtifactDefinition(String artifactClassName, ConstructorArgumentValues cargs, MutablePropertyValues pvs) {
      super(cargs, pvs);
      setArtifactClassName(artifactClassName);
   }

   /**
    * Create a new RootArtifactDefinition as deep copy of the given
    * artifact definition.
    * @param original the original artifact definition to copy from
    */
   public RootArtifactDefinition(RootArtifactDefinition original) {
      super(original);
      this.decoratedDefinition = original.decoratedDefinition;
      this.qualifiedElement = original.qualifiedElement;
      this.allowCaching = original.allowCaching;
      this.isFactoryMethodUnique = original.isFactoryMethodUnique;
      this.targetType = original.targetType;
      this.factoryMethodToIntrospect = original.factoryMethodToIntrospect;
   }

   /**
    * Create a new RootArtifactDefinition as deep copy of the given
    * artifact definition.
    * @param original the original artifact definition to copy from
    */
   RootArtifactDefinition(ArtifactDefinition original) {
      super(original);
   }


   @Override
   public String getParentName() {
      return null;
   }

   @Override
   public void setParentName(@Nullable String parentName) {
      if (parentName != null) {
         throw new IllegalArgumentException("Root artifact cannot be changed into a child artifact with parent reference");
      }
   }

   /**
    * Register a target definition that is being decorated by this artifact definition.
    */
   public void setDecoratedDefinition(@Nullable ArtifactDefinitionHolder decoratedDefinition) {
      this.decoratedDefinition = decoratedDefinition;
   }

   /**
    * Return the target definition that is being decorated by this artifact definition, if any.
    */
   @Nullable
   public ArtifactDefinitionHolder getDecoratedDefinition() {
      return this.decoratedDefinition;
   }

   /**
    * Specify the {@link AnnotatedElement} defining qualifiers,
    * to be used instead of the target class or factory method.
    * @see #setTargetType(ResolvableType)
    * @see #getResolvedFactoryMethod()
    */
   public void setQualifiedElement(@Nullable AnnotatedElement qualifiedElement) {
      this.qualifiedElement = qualifiedElement;
   }

   /**
    * Return the {@link AnnotatedElement} defining qualifiers, if any.
    * Otherwise, the factory method and target class will be checked.
    */
   @Nullable
   public AnnotatedElement getQualifiedElement() {
      return this.qualifiedElement;
   }

   /**
    * Specify a generics-containing target type of this artifact definition, if known in advance.
    */
   public void setTargetType(ResolvableType targetType) {
      this.targetType = targetType;
   }

   /**
    * Specify the target type of this artifact definition, if known in advance.
    */
   public void setTargetType(@Nullable Class<?> targetType) {
      this.targetType = (targetType != null ? ResolvableType.forClass(targetType) : null);
   }

   /**
    * Return the target type of this artifact definition, if known
    * (either specified in advance or resolved on first instantiation).
    */
   @Nullable
   public Class<?> getTargetType() {
      if (this.resolvedTargetType != null) {
         return this.resolvedTargetType;
      }
      ResolvableType targetType = this.targetType;
      return (targetType != null ? targetType.resolve() : null);
   }

   /**
    * Return a {@link ResolvableType} for this artifact definition,
    * either from runtime-cached type information or from configuration-time
    * {@link #setTargetType(ResolvableType)} or {@link #setArtifactClass(Class)},
    * also considering resolved factory method definitions.
    *
    * @see #setTargetType(ResolvableType)
    * @see #setArtifactClass(Class)
    * @see #setResolvedFactoryMethod(Method)
    */
   @Override
   public ResolvableType getResolvableType() {
      ResolvableType targetType = this.targetType;
      if (targetType != null) {
         return targetType;
      }
      ResolvableType returnType = this.factoryMethodReturnType;
      if (returnType != null) {
         return returnType;
      }
      Method factoryMethod = this.factoryMethodToIntrospect;
      if (factoryMethod != null) {
         return ResolvableType.forMethodReturnType(factoryMethod);
      }
      return super.getResolvableType();
   }

   /**
    * Determine preferred constructors to use for default construction, if any.
    * Constructor arguments will be autowired if necessary.
    * @return one or more preferred constructors, or {@code null} if none
    * (in which case the regular no-arg default constructor will be called)
    */
   @Nullable
   public Constructor<?>[] getPreferredConstructors() {
      return null;
   }

   /**
    * Specify a factory method name that refers to a non-overloaded method.
    */
   public void setUniqueFactoryMethodName(String name) {
      AssertUtils.hasText(name, "Factory method name must not be empty");
      setFactoryMethodName(name);
      this.isFactoryMethodUnique = true;
   }

   /**
    * Specify a factory method name that refers to an overloaded method.
    */
   public void setNonUniqueFactoryMethodName(String name) {
      AssertUtils.hasText(name, "Factory method name must not be empty");
      setFactoryMethodName(name);
      this.isFactoryMethodUnique = false;
   }

   /**
    * Check whether the given candidate qualifies as a factory method.
    */
   public boolean isFactoryMethod(Method candidate) {
      return candidate.getName().equals(getFactoryMethodName());
   }

   /**
    * Set a resolved Java Method for the factory method on this artifact definition.
    * @param method the resolved factory method, or {@code null} to reset it
    */
   public void setResolvedFactoryMethod(@Nullable Method method) {
      this.factoryMethodToIntrospect = method;
   }

   /**
    * Return the resolved factory method as a Java Method object, if available.
    * @return the factory method, or {@code null} if not found or not resolved yet
    */
   @Nullable
   public Method getResolvedFactoryMethod() {
      return this.factoryMethodToIntrospect;
   }

   public void registerExternallyManagedConfigMember(Member configMember) {
      synchronized (this.postProcessingLock) {
         if (this.externallyManagedConfigMembers == null) {
            this.externallyManagedConfigMembers = new HashSet<>(1);
         }
         this.externallyManagedConfigMembers.add(configMember);
      }
   }

   public boolean isExternallyManagedConfigMember(Member configMember) {
      synchronized (this.postProcessingLock) {
         return (this.externallyManagedConfigMembers != null &&
            this.externallyManagedConfigMembers.contains(configMember));
      }
   }

   public void registerExternallyManagedInitMethod(String initMethod) {
      synchronized (this.postProcessingLock) {
         if (this.externallyManagedInitMethods == null) {
            this.externallyManagedInitMethods = new HashSet<>(1);
         }
         this.externallyManagedInitMethods.add(initMethod);
      }
   }

   public boolean isExternallyManagedInitMethod(String initMethod) {
      synchronized (this.postProcessingLock) {
         return (this.externallyManagedInitMethods != null &&
            this.externallyManagedInitMethods.contains(initMethod));
      }
   }

   public void registerExternallyManagedDestroyMethod(String destroyMethod) {
      synchronized (this.postProcessingLock) {
         if (this.externallyManagedDestroyMethods == null) {
            this.externallyManagedDestroyMethods = new HashSet<>(1);
         }
         this.externallyManagedDestroyMethods.add(destroyMethod);
      }
   }

   public boolean isExternallyManagedDestroyMethod(String destroyMethod) {
      synchronized (this.postProcessingLock) {
         return (this.externallyManagedDestroyMethods != null &&
            this.externallyManagedDestroyMethods.contains(destroyMethod));
      }
   }


   @Override
   public RootArtifactDefinition cloneArtifactDefinition() {
      return new RootArtifactDefinition(this);
   }

   @Override
   public boolean equals(@Nullable Object other) {
      return (this == other || (other instanceof RootArtifactDefinition && super.equals(other)));
   }

   @Override
   public String toString() {
      return "Root artifact: " + super.toString();
   }
}
