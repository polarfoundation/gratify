package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionCustomizer;
import foundation.polar.gratify.artifacts.factory.config.AutowiredPropertyMarker;
import foundation.polar.gratify.artifacts.factory.config.RuntimeArtifactReference;
import foundation.polar.gratify.utils.ObjectUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

/**
 * Programmatic means of constructing
 * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition ArtifactDefinitions}
 * using the builder pattern. Intended primarily for use when implementing Gratify 2.0
 * {@link foundation.polar.gratify.artifacts.factory.xml.NamespaceHandler NamespaceHandlers}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public final class ArtifactDefinitionBuilder {

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link GenericArtifactDefinition}.
    */
   public static ArtifactDefinitionBuilder genericArtifactDefinition() {
      return new ArtifactDefinitionBuilder(new GenericArtifactDefinition());
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link GenericArtifactDefinition}.
    * @param artifactClassName the class name for the bean that the definition is being created for
    */
   public static ArtifactDefinitionBuilder genericArtifactDefinition(String artifactClassName) {
      ArtifactDefinitionBuilder builder = new ArtifactDefinitionBuilder(new GenericArtifactDefinition());
      builder.artifactDefinition.setArtifactClassName(artifactClassName);
      return builder;
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link GenericArtifactDefinition}.
    * @param beanClass the {@code Class} of the bean that the definition is being created for
    */
   public static ArtifactDefinitionBuilder genericArtifactDefinition(Class<?> beanClass) {
      ArtifactDefinitionBuilder builder = new ArtifactDefinitionBuilder(new GenericArtifactDefinition());
      builder.artifactDefinition.setArtifactClass(beanClass);
      return builder;
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link GenericArtifactDefinition}.
    * @param beanClass the {@code Class} of the bean that the definition is being created for
    * @param instanceSupplier a callback for creating an instance of the bean
    */
   public static <T> ArtifactDefinitionBuilder genericArtifactDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
      ArtifactDefinitionBuilder builder = new ArtifactDefinitionBuilder(new GenericArtifactDefinition());
      builder.artifactDefinition.setArtifactClass(beanClass);
      builder.artifactDefinition.setInstanceSupplier(instanceSupplier);
      return builder;
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link RootArtifactDefinition}.
    * @param artifactClassName the class name for the bean that the definition is being created for
    */
   public static ArtifactDefinitionBuilder rootArtifactDefinition(String artifactClassName) {
      return rootArtifactDefinition(artifactClassName, null);
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link RootArtifactDefinition}.
    * @param artifactClassName the class name for the bean that the definition is being created for
    * @param factoryMethodName the name of the method to use to construct the bean instance
    */
   public static ArtifactDefinitionBuilder rootArtifactDefinition(String artifactClassName, @Nullable String factoryMethodName) {
      ArtifactDefinitionBuilder builder = new ArtifactDefinitionBuilder(new RootArtifactDefinition());
      builder.artifactDefinition.setArtifactClassName(artifactClassName);
      builder.artifactDefinition.setFactoryMethodName(factoryMethodName);
      return builder;
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link RootArtifactDefinition}.
    * @param beanClass the {@code Class} of the bean that the definition is being created for
    */
   public static ArtifactDefinitionBuilder rootArtifactDefinition(Class<?> beanClass) {
      return rootArtifactDefinition(beanClass, null);
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link RootArtifactDefinition}.
    * @param beanClass the {@code Class} of the bean that the definition is being created for
    * @param factoryMethodName the name of the method to use to construct the bean instance
    */
   public static ArtifactDefinitionBuilder rootArtifactDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
      ArtifactDefinitionBuilder builder = new ArtifactDefinitionBuilder(new RootArtifactDefinition());
      builder.artifactDefinition.setArtifactClass(beanClass);
      builder.artifactDefinition.setFactoryMethodName(factoryMethodName);
      return builder;
   }

   /**
    * Create a new {@code ArtifactDefinitionBuilder} used to construct a {@link ChildArtifactDefinition}.
    * @param parentName the name of the parent bean
    */
   public static ArtifactDefinitionBuilder childArtifactDefinition(String parentName) {
      return new ArtifactDefinitionBuilder(new ChildArtifactDefinition(parentName));
   }

   /**
    * The {@code ArtifactDefinition} instance we are creating.
    */
   private final AbstractArtifactDefinition artifactDefinition;

   /**
    * Our current position with respect to constructor args.
    */
   private int constructorArgIndex;

   /**
    * Enforce the use of factory methods.
    */
   private ArtifactDefinitionBuilder(AbstractArtifactDefinition artifactDefinition) {
      this.artifactDefinition = artifactDefinition;
   }

   /**
    * Return the current ArtifactDefinition object in its raw (unvalidated) form.
    * @see #getArtifactDefinition()
    */
   public AbstractArtifactDefinition getRawArtifactDefinition() {
      return this.artifactDefinition;
   }

   /**
    * Validate and return the created ArtifactDefinition object.
    */
   public AbstractArtifactDefinition getArtifactDefinition() {
      this.artifactDefinition.validate();
      return this.artifactDefinition;
   }


   /**
    * Set the name of the parent definition of this bean definition.
    */
   public ArtifactDefinitionBuilder setParentName(String parentName) {
      this.artifactDefinition.setParentName(parentName);
      return this;
   }

   /**
    * Set the name of a static factory method to use for this definition,
    * to be called on this bean's class.
    */
   public ArtifactDefinitionBuilder setFactoryMethod(String factoryMethod) {
      this.artifactDefinition.setFactoryMethodName(factoryMethod);
      return this;
   }

   /**
    * Set the name of a non-static factory method to use for this definition,
    * including the bean name of the factory instance to call the method on.
    * @param factoryMethod the name of the factory method
    * @param factoryArtifact the name of the bean to call the specified factory method on
    */
   public ArtifactDefinitionBuilder setFactoryMethodOnArtifact(String factoryMethod, String factoryArtifact) {
      this.artifactDefinition.setFactoryMethodName(factoryMethod);
      this.artifactDefinition.setFactoryArtifactName(factoryArtifact);
      return this;
   }

   /**
    * Add an indexed constructor arg value. The current index is tracked internally
    * and all additions are at the present point.
    */
   public ArtifactDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
      this.artifactDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
         this.constructorArgIndex++, value);
      return this;
   }

   /**
    * Add a reference to a named bean as a constructor arg.
    * @see #addConstructorArgValue(Object)
    */
   public ArtifactDefinitionBuilder addConstructorArgReference(String beanName) {
      this.artifactDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
         this.constructorArgIndex++, new RuntimeArtifactReference(beanName));
      return this;
   }

   /**
    * Add the supplied property value under the given property name.
    */
   public ArtifactDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
      this.artifactDefinition.getPropertyValues().add(name, value);
      return this;
   }

   /**
    * Add a reference to the specified bean name under the property specified.
    * @param name the name of the property to add the reference to
    * @param beanName the name of the bean being referenced
    */
   public ArtifactDefinitionBuilder addPropertyReference(String name, String beanName) {
      this.artifactDefinition.getPropertyValues().add(name, new RuntimeArtifactReference(beanName));
      return this;
   }

   /**
    * Add an autowired marker for the specified property on the specified bean.
    * @param name the name of the property to mark as autowired
    * @see AutowiredPropertyMarker
    */
   public ArtifactDefinitionBuilder addAutowiredProperty(String name) {
      this.artifactDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
      return this;
   }

   /**
    * Set the init method for this definition.
    */
   public ArtifactDefinitionBuilder setInitMethodName(@Nullable String methodName) {
      this.artifactDefinition.setInitMethodName(methodName);
      return this;
   }

   /**
    * Set the destroy method for this definition.
    */
   public ArtifactDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
      this.artifactDefinition.setDestroyMethodName(methodName);
      return this;
   }

   /**
    * Set the scope of this definition.
    * @see foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition#SCOPE_SINGLETON
    * @see foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition#SCOPE_PROTOTYPE
    */
   public ArtifactDefinitionBuilder setScope(@Nullable String scope) {
      this.artifactDefinition.setScope(scope);
      return this;
   }

   /**
    * Set whether or not this definition is abstract.
    */
   public ArtifactDefinitionBuilder setAbstract(boolean flag) {
      this.artifactDefinition.setAbstract(flag);
      return this;
   }

   /**
    * Set whether beans for this definition should be lazily initialized or not.
    */
   public ArtifactDefinitionBuilder setLazyInit(boolean lazy) {
      this.artifactDefinition.setLazyInit(lazy);
      return this;
   }

   /**
    * Set the autowire mode for this definition.
    */
   public ArtifactDefinitionBuilder setAutowireMode(int autowireMode) {
      this.artifactDefinition.setAutowireMode(autowireMode);
      return this;
   }

   /**
    * Set the dependency check mode for this definition.
    */
   public ArtifactDefinitionBuilder setDependencyCheck(int dependencyCheck) {
      this.artifactDefinition.setDependencyCheck(dependencyCheck);
      return this;
   }

   /**
    * Append the specified bean name to the list of beans that this definition
    * depends on.
    */
   public ArtifactDefinitionBuilder addDependsOn(String beanName) {
      if (this.artifactDefinition.getDependsOn() == null) {
         this.artifactDefinition.setDependsOn(beanName);
      }
      else {
         String[] added = ObjectUtils.addObjectToArray(this.artifactDefinition.getDependsOn(), beanName);
         this.artifactDefinition.setDependsOn(added);
      }
      return this;
   }

   /**
    * Set whether this bean is a primary autowire candidate.
    */
   public ArtifactDefinitionBuilder setPrimary(boolean primary) {
      this.artifactDefinition.setPrimary(primary);
      return this;
   }

   /**
    * Set the role of this definition.
    */
   public ArtifactDefinitionBuilder setRole(int role) {
      this.artifactDefinition.setRole(role);
      return this;
   }

   /**
    * Apply the given customizers to the underlying bean definition.
    */
   public ArtifactDefinitionBuilder applyCustomizers(ArtifactDefinitionCustomizer... customizers) {
      for (ArtifactDefinitionCustomizer customizer : customizers) {
         customizer.customize(this.artifactDefinition);
      }
      return this;
   }

}
