package foundation.polar.gratify.artifacts.factory.support;

import foundation.polar.gratify.artifacts.ArtifactMetadataAttributeAccessor;
import foundation.polar.gratify.artifacts.MutablePropertyValues;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinition;
import foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.config.ConstructorArgumentValues;
import foundation.polar.gratify.core.ResolvableType;
import foundation.polar.gratify.core.io.DescriptiveResource;
import foundation.polar.gratify.core.io.Resource;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Supplier;

/**
 * Base class for concrete, full-fledged {@link ArtifactDefinition} classes,
 * factoring out common properties of {@link GenericArtifactDefinition},
 * {@link RootArtifactDefinition}, and {@link ChildArtifactDefinition}.
 *
 * <p>The autowire constants match the ones defined in the
 * {@link foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory}
 * interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Mark Fisher
 * @see GenericArtifactDefinition
 * @see RootArtifactDefinition
 * @see ChildArtifactDefinition
 */
@SuppressWarnings("serial")
public abstract class AbstractArtifactDefinition extends ArtifactMetadataAttributeAccessor
   implements ArtifactDefinition, Cloneable {
   /**
    * Constant for the default scope name: {@code ""}, equivalent to singleton
    * status unless overridden from a parent artifact definition (if applicable).
    */
   public static final String SCOPE_DEFAULT = "";

   /**
    * Constant that indicates no external autowiring at all.
    * @see #setAutowireMode
    */
   public static final int AUTOWIRE_NO = AutowireCapableArtifactFactory.AUTOWIRE_NO;

   /**
    * Constant that indicates autowiring artifact properties by name.
    * @see #setAutowireMode
    */
   public static final int AUTOWIRE_BY_NAME = AutowireCapableArtifactFactory.AUTOWIRE_BY_NAME;

   /**
    * Constant that indicates autowiring artifact properties by type.
    * @see #setAutowireMode
    */
   public static final int AUTOWIRE_BY_TYPE = AutowireCapableArtifactFactory.AUTOWIRE_BY_TYPE;

   /**
    * Constant that indicates autowiring a constructor.
    * @see #setAutowireMode
    */
   public static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableArtifactFactory.AUTOWIRE_CONSTRUCTOR;

   /**
    * Constant that indicates determining an appropriate autowire strategy
    * through introspection of the bean class.
    * @see #setAutowireMode
    * @deprecated as of Gratify 3.0: If you are using mixed autowiring strategies,
    * use annotation-based autowiring for clearer demarcation of autowiring needs.
    */
   @Deprecated
   public static final int AUTOWIRE_AUTODETECT = AutowireCapableArtifactFactory.AUTOWIRE_AUTODETECT;

   /**
    * Constant that indicates no dependency check at all.
    * @see #setDependencyCheck
    */
   public static final int DEPENDENCY_CHECK_NONE = 0;

   /**
    * Constant that indicates dependency checking for object references.
    * @see #setDependencyCheck
    */
   public static final int DEPENDENCY_CHECK_OBJECTS = 1;

   /**
    * Constant that indicates dependency checking for "simple" properties.
    * @see #setDependencyCheck
    * @see foundation.polar.gratify.artifacts.ArtifactUtils#isSimpleProperty
    */
   public static final int DEPENDENCY_CHECK_SIMPLE = 2;

   /**
    * Constant that indicates dependency checking for all properties
    * (object references as well as "simple" properties).
    * @see #setDependencyCheck
    */
   public static final int DEPENDENCY_CHECK_ALL = 3;

   /**
    * Constant that indicates the container should attempt to infer the
    * {@link #setDestroyMethodName destroy method name} for a artifact as opposed to
    * explicit specification of a method name. The value {@value} is specifically
    * designed to include characters otherwise illegal in a method name, ensuring
    * no possibility of collisions with legitimately named methods having the same
    * name.
    * <p>Currently, the method names detected during destroy method inference
    * are "close" and "shutdown", if present on the specific artifact class.
    */
   public static final String INFER_METHOD = "(inferred)";

   @Nullable
   private volatile Object artifactClass;

   @Nullable
   private String scope = SCOPE_DEFAULT;

   private boolean abstractFlag = false;

   @Nullable
   private Boolean lazyInit;

   private int autowireMode = AUTOWIRE_NO;

   private int dependencyCheck = DEPENDENCY_CHECK_NONE;

   @Nullable
   private String[] dependsOn;

   private boolean autowireCandidate = true;

   private boolean primary = false;

   private final Map<String, AutowireCandidateQualifier> qualifiers = new LinkedHashMap<>();

   @Nullable
   private Supplier<?> instanceSupplier;

   private boolean nonPublicAccessAllowed = true;

   private boolean lenientConstructorResolution = true;

   @Nullable
   private String factoryArtifactName;

   @Nullable
   private String factoryMethodName;

   @Nullable
   private ConstructorArgumentValues constructorArgumentValues;

   @Nullable
   private MutablePropertyValues propertyValues;

   private MethodOverrides methodOverrides = new MethodOverrides();

   @Nullable
   private String initMethodName;

   @Nullable
   private String destroyMethodName;

   private boolean enforceInitMethod = true;

   private boolean enforceDestroyMethod = true;

   private boolean synthetic = false;

   private int role = ArtifactDefinition.ROLE_APPLICATION;

   @Nullable
   private String description;

   @Nullable
   private Resource resource;

   /**
    * Create a new AbstractArtifactDefinition with default settings.
    */
   protected AbstractArtifactDefinition() {
      this(null, null);
   }

   /**
    * Create a new AbstractArtifactDefinition with the given
    * constructor argument values and property values.
    */
   protected AbstractArtifactDefinition(@Nullable ConstructorArgumentValues cargs, @Nullable MutablePropertyValues pvs) {
      this.constructorArgumentValues = cargs;
      this.propertyValues = pvs;
   }

   /**
    * Create a new AbstractArtifactDefinition as a deep copy of the given
    * artifact definition.
    * @param original the original artifact definition to copy from
    */
   protected AbstractArtifactDefinition(ArtifactDefinition original) {
      setParentName(original.getParentName());
      setArtifactClassName(original.getArtifactClassName());
      setScope(original.getScope());
      setAbstract(original.isAbstract());
      setFactoryArtifactName(original.getFactoryArtifactName());
      setFactoryMethodName(original.getFactoryMethodName());
      setRole(original.getRole());
      setSource(original.getSource());
      copyAttributesFrom(original);

      if (original instanceof AbstractArtifactDefinition) {
         AbstractArtifactDefinition originalAbd = (AbstractArtifactDefinition) original;
         if (originalAbd.hasArtifactClass()) {
            setArtifactClass(originalAbd.getArtifactClass());
         }
         if (originalAbd.hasConstructorArgumentValues()) {
            setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
         }
         if (originalAbd.hasPropertyValues()) {
            setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
         }
         if (originalAbd.hasMethodOverrides()) {
            setMethodOverrides(new MethodOverrides(originalAbd.getMethodOverrides()));
         }
         Boolean lazyInit = originalAbd.getLazyInit();
         if (lazyInit != null) {
            setLazyInit(lazyInit);
         }
         setAutowireMode(originalAbd.getAutowireMode());
         setDependencyCheck(originalAbd.getDependencyCheck());
         setDependsOn(originalAbd.getDependsOn());
         setAutowireCandidate(originalAbd.isAutowireCandidate());
         setPrimary(originalAbd.isPrimary());
         copyQualifiersFrom(originalAbd);
         setInstanceSupplier(originalAbd.getInstanceSupplier());
         setNonPublicAccessAllowed(originalAbd.isNonPublicAccessAllowed());
         setLenientConstructorResolution(originalAbd.isLenientConstructorResolution());
         setInitMethodName(originalAbd.getInitMethodName());
         setEnforceInitMethod(originalAbd.isEnforceInitMethod());
         setDestroyMethodName(originalAbd.getDestroyMethodName());
         setEnforceDestroyMethod(originalAbd.isEnforceDestroyMethod());
         setSynthetic(originalAbd.isSynthetic());
         setResource(originalAbd.getResource());
      }
      else {
         setConstructorArgumentValues(new ConstructorArgumentValues(original.getConstructorArgumentValues()));
         setPropertyValues(new MutablePropertyValues(original.getPropertyValues()));
         setLazyInit(original.isLazyInit());
         setResourceDescription(original.getResourceDescription());
      }
   }

   /**
    * Override settings in this artifact definition (presumably a copied parent
    * from a parent-child inheritance relationship) from the given artifact
    * definition (presumably the child).
    * <ul>
    * <li>Will override artifactClass if specified in the given artifact definition.
    * <li>Will always take {@code abstract}, {@code scope},
    * {@code lazyInit}, {@code autowireMode}, {@code dependencyCheck},
    * and {@code dependsOn} from the given artifact definition.
    * <li>Will add {@code constructorArgumentValues}, {@code propertyValues},
    * {@code methodOverrides} from the given artifact definition to existing ones.
    * <li>Will override {@code factoryArtifactName}, {@code factoryMethodName},
    * {@code initMethodName}, and {@code destroyMethodName} if specified
    * in the given artifact definition.
    * </ul>
    */
   public void overrideFrom(ArtifactDefinition other) {
      if (StringUtils.hasLength(other.getArtifactClassName())) {
         setArtifactClassName(other.getArtifactClassName());
      }
      if (StringUtils.hasLength(other.getScope())) {
         setScope(other.getScope());
      }
      setAbstract(other.isAbstract());
      if (StringUtils.hasLength(other.getFactoryArtifactName())) {
         setFactoryArtifactName(other.getFactoryArtifactName());
      }
      if (StringUtils.hasLength(other.getFactoryMethodName())) {
         setFactoryMethodName(other.getFactoryMethodName());
      }
      setRole(other.getRole());
      setSource(other.getSource());
      copyAttributesFrom(other);

      if (other instanceof AbstractArtifactDefinition) {
         AbstractArtifactDefinition otherAbd = (AbstractArtifactDefinition) other;
         if (otherAbd.hasArtifactClass()) {
            setArtifactClass(otherAbd.getArtifactClass());
         }
         if (otherAbd.hasConstructorArgumentValues()) {
            getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
         }
         if (otherAbd.hasPropertyValues()) {
            getPropertyValues().addPropertyValues(other.getPropertyValues());
         }
         if (otherAbd.hasMethodOverrides()) {
            getMethodOverrides().addOverrides(otherAbd.getMethodOverrides());
         }
         Boolean lazyInit = otherAbd.getLazyInit();
         if (lazyInit != null) {
            setLazyInit(lazyInit);
         }
         setAutowireMode(otherAbd.getAutowireMode());
         setDependencyCheck(otherAbd.getDependencyCheck());
         setDependsOn(otherAbd.getDependsOn());
         setAutowireCandidate(otherAbd.isAutowireCandidate());
         setPrimary(otherAbd.isPrimary());
         copyQualifiersFrom(otherAbd);
         setInstanceSupplier(otherAbd.getInstanceSupplier());
         setNonPublicAccessAllowed(otherAbd.isNonPublicAccessAllowed());
         setLenientConstructorResolution(otherAbd.isLenientConstructorResolution());
         if (otherAbd.getInitMethodName() != null) {
            setInitMethodName(otherAbd.getInitMethodName());
            setEnforceInitMethod(otherAbd.isEnforceInitMethod());
         }
         if (otherAbd.getDestroyMethodName() != null) {
            setDestroyMethodName(otherAbd.getDestroyMethodName());
            setEnforceDestroyMethod(otherAbd.isEnforceDestroyMethod());
         }
         setSynthetic(otherAbd.isSynthetic());
         setResource(otherAbd.getResource());
      }
      else {
         getConstructorArgumentValues().addArgumentValues(other.getConstructorArgumentValues());
         getPropertyValues().addPropertyValues(other.getPropertyValues());
         setLazyInit(other.isLazyInit());
         setResourceDescription(other.getResourceDescription());
      }
   }

   /**
    * Apply the provided default values to this artifact.
    * @param defaults the default settings to apply
    */
   public void applyDefaults(ArtifactDefinitionDefaults defaults) {
      Boolean lazyInit = defaults.getLazyInit();
      if (lazyInit != null) {
         setLazyInit(lazyInit);
      }
      setAutowireMode(defaults.getAutowireMode());
      setDependencyCheck(defaults.getDependencyCheck());
      setInitMethodName(defaults.getInitMethodName());
      setEnforceInitMethod(false);
      setDestroyMethodName(defaults.getDestroyMethodName());
      setEnforceDestroyMethod(false);
   }

   /**
    * Specify the artifact class name of this artifact definition.
    */
   @Override
   public void setArtifactClassName(@Nullable String artifactClassName) {
      this.artifactClass = artifactClassName;
   }

   /**
    * Return the current artifact class name of this artifact definition.
    */
   @Override
   @Nullable
   public String getArtifactClassName() {
      Object artifactClassObject = this.artifactClass;
      if (artifactClassObject instanceof Class) {
         return ((Class<?>) artifactClassObject).getName();
      }
      else {
         return (String) artifactClassObject;
      }
   }

   /**
    * Specify the class for this artifact.
    */
   public void setArtifactClass(@Nullable Class<?> artifactClass) {
      this.artifactClass = artifactClass;
   }

   /**
    * Return the class of the wrapped artifact (assuming it is resolved already).
    * @return the artifact class (never {@code null})
    * @throws IllegalStateException if the artifact definition does not define a artifact class,
    * or a specified artifact class name has not been resolved into an actual Class yet
    * @see #hasArtifactClass()
    * @see #setArtifactClass(Class)
    * @see #resolveArtifactClass(ClassLoader)
    */
   public Class<?> getArtifactClass() throws IllegalStateException {
      Object artifactClassObject = this.artifactClass;
      if (artifactClassObject == null) {
         throw new IllegalStateException("No artifact class specified on artifact definition");
      }
      if (!(artifactClassObject instanceof Class)) {
         throw new IllegalStateException(
            "Artifact class name [" + artifactClassObject + "] has not been resolved into an actual Class");
      }
      return (Class<?>) artifactClassObject;
   }

   /**
    * Return whether this definition specifies a artifact class.
    * @see #getArtifactClass()
    * @see #setArtifactClass(Class)
    * @see #resolveArtifactClass(ClassLoader)
    */
   public boolean hasArtifactClass() {
      return (this.artifactClass instanceof Class);
   }

   /**
    * Determine the class of the wrapped artifact, resolving it from a
    * specified class name if necessary. Will also reload a specified
    * Class from its name when called with the artifact class already resolved.
    * @param classLoader the ClassLoader to use for resolving a (potential) class name
    * @return the resolved artifact class
    * @throws ClassNotFoundException if the class name could be resolved
    */
   @Nullable
   public Class<?> resolveArtifactClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
      String className = getArtifactClassName();
      if (className == null) {
         return null;
      }
      Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
      this.artifactClass = resolvedClass;
      return resolvedClass;
   }

   /**
    * Return a resolvable type for this artifact definition.
    * <p>This implementation delegates to {@link #getArtifactClass()}.
    */
   @Override
   public ResolvableType getResolvableType() {
      return (hasArtifactClass() ? ResolvableType.forClass(getArtifactClass()) : ResolvableType.NONE);
   }

   /**
    * Set the name of the target scope for the artifact.
    * <p>The default is singleton status, although this is only applied once
    * a artifact definition becomes active in the containing factory. A artifact
    * definition may eventually inherit its scope from a parent artifact definition.
    * For this reason, the default scope name is an empty string (i.e., {@code ""}),
    * with singleton status being assumed until a resolved scope is set.
    * @see #SCOPE_SINGLETON
    * @see #SCOPE_PROTOTYPE
    */
   @Override
   public void setScope(@Nullable String scope) {
      this.scope = scope;
   }

   /**
    * Return the name of the target scope for the artifact.
    */
   @Override
   @Nullable
   public String getScope() {
      return this.scope;
   }

   /**
    * Return whether this a <b>Singleton</b>, with a single shared instance
    * returned from all calls.
    * @see #SCOPE_SINGLETON
    */
   @Override
   public boolean isSingleton() {
      return SCOPE_SINGLETON.equals(this.scope) || SCOPE_DEFAULT.equals(this.scope);
   }

   /**
    * Return whether this a <b>Prototype</b>, with an independent instance
    * returned for each call.
    * @see #SCOPE_PROTOTYPE
    */
   @Override
   public boolean isPrototype() {
      return SCOPE_PROTOTYPE.equals(this.scope);
   }

   /**
    * Set if this artifact is "abstract", i.e. not meant to be instantiated itself but
    * rather just serving as parent for concrete child artifact definitions.
    * <p>Default is "false". Specify true to tell the artifact factory to not try to
    * instantiate that particular artifact in any case.
    */
   public void setAbstract(boolean abstractFlag) {
      this.abstractFlag = abstractFlag;
   }

   /**
    * Return whether this artifact is "abstract", i.e. not meant to be instantiated
    * itself but rather just serving as parent for concrete child artifact definitions.
    */
   @Override
   public boolean isAbstract() {
      return this.abstractFlag;
   }

   /**
    * Set whether this artifact should be lazily initialized.
    * <p>If {@code false}, the artifact will get instantiated on startup by artifact
    * factories that perform eager initialization of singletons.
    */
   @Override
   public void setLazyInit(boolean lazyInit) {
      this.lazyInit = lazyInit;
   }

   /**
    * Return whether this artifact should be lazily initialized, i.e. not
    * eagerly instantiated on startup. Only applicable to a singleton artifact.
    * @return whether to apply lazy-init semantics ({@code false} by default)
    */
   @Override
   public boolean isLazyInit() {
      return (this.lazyInit != null && this.lazyInit.booleanValue());
   }

   /**
    * Return whether this artifact should be lazily initialized, i.e. not
    * eagerly instantiated on startup. Only applicable to a singleton artifact.
    * @return the lazy-init flag if explicitly set, or {@code null} otherwise
    */
   @Nullable
   public Boolean getLazyInit() {
      return this.lazyInit;
   }

   /**
    * Set the autowire mode. This determines whether any automagical detection
    * and setting of artifact references will happen. Default is AUTOWIRE_NO
    * which means there won't be convention-based autowiring by name or type
    * (however, there may still be explicit annotation-driven autowiring).
    * @param autowireMode the autowire mode to set.
    * Must be one of the constants defined in this class.
    * @see #AUTOWIRE_NO
    * @see #AUTOWIRE_BY_NAME
    * @see #AUTOWIRE_BY_TYPE
    * @see #AUTOWIRE_CONSTRUCTOR
    * @see #AUTOWIRE_AUTODETECT
    */
   public void setAutowireMode(int autowireMode) {
      this.autowireMode = autowireMode;
   }

   /**
    * Return the autowire mode as specified in the artifact definition.
    */
   public int getAutowireMode() {
      return this.autowireMode;
   }

   /**
    * Return the resolved autowire code,
    * (resolving AUTOWIRE_AUTODETECT to AUTOWIRE_CONSTRUCTOR or AUTOWIRE_BY_TYPE).
    * @see #AUTOWIRE_AUTODETECT
    * @see #AUTOWIRE_CONSTRUCTOR
    * @see #AUTOWIRE_BY_TYPE
    */
   public int getResolvedAutowireMode() {
      if (this.autowireMode == AUTOWIRE_AUTODETECT) {
         // Work out whether to apply setter autowiring or constructor autowiring.
         // If it has a no-arg constructor it's deemed to be setter autowiring,
         // otherwise we'll try constructor autowiring.
         Constructor<?>[] constructors = getArtifactClass().getConstructors();
         for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) {
               return AUTOWIRE_BY_TYPE;
            }
         }
         return AUTOWIRE_CONSTRUCTOR;
      }
      else {
         return this.autowireMode;
      }
   }

   /**
    * Set the dependency check code.
    * @param dependencyCheck the code to set.
    * Must be one of the four constants defined in this class.
    * @see #DEPENDENCY_CHECK_NONE
    * @see #DEPENDENCY_CHECK_OBJECTS
    * @see #DEPENDENCY_CHECK_SIMPLE
    * @see #DEPENDENCY_CHECK_ALL
    */
   public void setDependencyCheck(int dependencyCheck) {
      this.dependencyCheck = dependencyCheck;
   }

   /**
    * Return the dependency check code.
    */
   public int getDependencyCheck() {
      return this.dependencyCheck;
   }

   /**
    * Set the names of the artifacts that this artifact depends on being initialized.
    * The artifact factory will guarantee that these artifacts get initialized first.
    * <p>Note that dependencies are normally expressed through artifact properties or
    * constructor arguments. This property should just be necessary for other kinds
    * of dependencies like statics (*ugh*) or database preparation on startup.
    */
   @Override
   public void setDependsOn(@Nullable String... dependsOn) {
      this.dependsOn = dependsOn;
   }

   /**
    * Return the artifact names that this artifact depends on.
    */
   @Override
   @Nullable
   public String[] getDependsOn() {
      return this.dependsOn;
   }

   /**
    * Set whether this artifact is a candidate for getting autowired into some other artifact.
    * <p>Note that this flag is designed to only affect type-based autowiring.
    * It does not affect explicit references by name, which will get resolved even
    * if the specified artifact is not marked as an autowire candidate. As a consequence,
    * autowiring by name will nevertheless inject a artifact if the name matches.
    * @see #AUTOWIRE_BY_TYPE
    * @see #AUTOWIRE_BY_NAME
    */
   @Override
   public void setAutowireCandidate(boolean autowireCandidate) {
      this.autowireCandidate = autowireCandidate;
   }

   /**
    * Return whether this artifact is a candidate for getting autowired into some other artifact.
    */
   @Override
   public boolean isAutowireCandidate() {
      return this.autowireCandidate;
   }

   /**
    * Set whether this artifact is a primary autowire candidate.
    * <p>If this value is {@code true} for exactly one artifact among multiple
    * matching candidates, it will serve as a tie-breaker.
    */
   @Override
   public void setPrimary(boolean primary) {
      this.primary = primary;
   }

   /**
    * Return whether this artifact is a primary autowire candidate.
    */
   @Override
   public boolean isPrimary() {
      return this.primary;
   }

   /**
    * Register a qualifier to be used for autowire candidate resolution,
    * keyed by the qualifier's type name.
    * @see AutowireCandidateQualifier#getTypeName()
    */
   public void addQualifier(AutowireCandidateQualifier qualifier) {
      this.qualifiers.put(qualifier.getTypeName(), qualifier);
   }

   /**
    * Return whether this artifact has the specified qualifier.
    */
   public boolean hasQualifier(String typeName) {
      return this.qualifiers.containsKey(typeName);
   }

   /**
    * Return the qualifier mapped to the provided type name.
    */
   @Nullable
   public AutowireCandidateQualifier getQualifier(String typeName) {
      return this.qualifiers.get(typeName);
   }

   /**
    * Return all registered qualifiers.
    * @return the Set of {@link AutowireCandidateQualifier} objects.
    */
   public Set<AutowireCandidateQualifier> getQualifiers() {
      return new LinkedHashSet<>(this.qualifiers.values());
   }

   /**
    * Copy the qualifiers from the supplied AbstractArtifactDefinition to this artifact definition.
    * @param source the AbstractArtifactDefinition to copy from
    */
   public void copyQualifiersFrom(AbstractArtifactDefinition source) {
      AssertUtils.notNull(source, "Source must not be null");
      this.qualifiers.putAll(source.qualifiers);
   }

   /**
    * Specify a callback for creating an instance of the artifact,
    * as an alternative to a declaratively specified factory method.
    * <p>If such a callback is set, it will override any other constructor
    * or factory method metadata. However, artifact property population and
    * potential annotation-driven injection will still apply as usual.
    * @see #setConstructorArgumentValues(ConstructorArgumentValues)
    * @see #setPropertyValues(MutablePropertyValues)
    */
   public void setInstanceSupplier(@Nullable Supplier<?> instanceSupplier) {
      this.instanceSupplier = instanceSupplier;
   }

   /**
    * Return a callback for creating an instance of the artifact, if any.
    */
   @Nullable
   public Supplier<?> getInstanceSupplier() {
      return this.instanceSupplier;
   }

   /**
    * Specify whether to allow access to non-public constructors and methods,
    * for the case of externalized metadata pointing to those. The default is
    * {@code true}; switch this to {@code false} for public access only.
    * <p>This applies to constructor resolution, factory method resolution,
    * and also init/destroy methods. Artifact property accessors have to be public
    * in any case and are not affected by this setting.
    * <p>Note that annotation-driven configuration will still access non-public
    * members as far as they have been annotated. This setting applies to
    * externalized metadata in this artifact definition only.
    */
   public void setNonPublicAccessAllowed(boolean nonPublicAccessAllowed) {
      this.nonPublicAccessAllowed = nonPublicAccessAllowed;
   }

   /**
    * Return whether to allow access to non-public constructors and methods.
    */
   public boolean isNonPublicAccessAllowed() {
      return this.nonPublicAccessAllowed;
   }

   /**
    * Specify whether to resolve constructors in lenient mode ({@code true},
    * which is the default) or to switch to strict resolution (throwing an exception
    * in case of ambiguous constructors that all match when converting the arguments,
    * whereas lenient mode would use the one with the 'closest' type matches).
    */
   public void setLenientConstructorResolution(boolean lenientConstructorResolution) {
      this.lenientConstructorResolution = lenientConstructorResolution;
   }

   /**
    * Return whether to resolve constructors in lenient mode or in strict mode.
    */
   public boolean isLenientConstructorResolution() {
      return this.lenientConstructorResolution;
   }

   /**
    * Specify the factory artifact to use, if any.
    * This the name of the artifact to call the specified factory method on.
    * @see #setFactoryMethodName
    */
   @Override
   public void setFactoryArtifactName(@Nullable String factoryArtifactName) {
      this.factoryArtifactName = factoryArtifactName;
   }

   /**
    * Return the factory artifact name, if any.
    */
   @Override
   @Nullable
   public String getFactoryArtifactName() {
      return this.factoryArtifactName;
   }

   /**
    * Specify a factory method, if any. This method will be invoked with
    * constructor arguments, or with no arguments if none are specified.
    * The method will be invoked on the specified factory artifact, if any,
    * or otherwise as a static method on the local artifact class.
    * @see #setFactoryArtifactName
    * @see #setArtifactClassName
    */
   @Override
   public void setFactoryMethodName(@Nullable String factoryMethodName) {
      this.factoryMethodName = factoryMethodName;
   }

   /**
    * Return a factory method, if any.
    */
   @Override
   @Nullable
   public String getFactoryMethodName() {
      return this.factoryMethodName;
   }

   /**
    * Specify constructor argument values for this artifact.
    */
   public void setConstructorArgumentValues(ConstructorArgumentValues constructorArgumentValues) {
      this.constructorArgumentValues = constructorArgumentValues;
   }

   /**
    * Return constructor argument values for this artifact (never {@code null}).
    */
   @Override
   public ConstructorArgumentValues getConstructorArgumentValues() {
      if (this.constructorArgumentValues == null) {
         this.constructorArgumentValues = new ConstructorArgumentValues();
      }
      return this.constructorArgumentValues;
   }

   /**
    * Return if there are constructor argument values defined for this artifact.
    */
   @Override
   public boolean hasConstructorArgumentValues() {
      return (this.constructorArgumentValues != null && !this.constructorArgumentValues.isEmpty());
   }

   /**
    * Specify property values for this artifact, if any.
    */
   public void setPropertyValues(MutablePropertyValues propertyValues) {
      this.propertyValues = propertyValues;
   }

   /**
    * Return property values for this artifact (never {@code null}).
    */
   @Override
   public MutablePropertyValues getPropertyValues() {
      if (this.propertyValues == null) {
         this.propertyValues = new MutablePropertyValues();
      }
      return this.propertyValues;
   }

   /**
    * Return if there are property values values defined for this artifact.
    */
   @Override
   public boolean hasPropertyValues() {
      return (this.propertyValues != null && !this.propertyValues.isEmpty());
   }

   /**
    * Specify method overrides for the artifact, if any.
    */
   public void setMethodOverrides(MethodOverrides methodOverrides) {
      this.methodOverrides = methodOverrides;
   }

   /**
    * Return information about methods to be overridden by the IoC
    * container. This will be empty if there are no method overrides.
    * <p>Never returns {@code null}.
    */
   public MethodOverrides getMethodOverrides() {
      return this.methodOverrides;
   }

   /**
    * Return if there are method overrides defined for this artifact.
    */
   public boolean hasMethodOverrides() {
      return !this.methodOverrides.isEmpty();
   }

   /**
    * Set the name of the initializer method.
    * <p>The default is {@code null} in which case there is no initializer method.
    */
   @Override
   public void setInitMethodName(@Nullable String initMethodName) {
      this.initMethodName = initMethodName;
   }

   /**
    * Return the name of the initializer method.
    */
   @Override
   @Nullable
   public String getInitMethodName() {
      return this.initMethodName;
   }

   /**
    * Specify whether or not the configured init method is the default.
    * <p>The default value is {@code false}.
    * @see #setInitMethodName
    */
   public void setEnforceInitMethod(boolean enforceInitMethod) {
      this.enforceInitMethod = enforceInitMethod;
   }

   /**
    * Indicate whether the configured init method is the default.
    * @see #getInitMethodName()
    */
   public boolean isEnforceInitMethod() {
      return this.enforceInitMethod;
   }

   /**
    * Set the name of the destroy method.
    * <p>The default is {@code null} in which case there is no destroy method.
    */
   @Override
   public void setDestroyMethodName(@Nullable String destroyMethodName) {
      this.destroyMethodName = destroyMethodName;
   }

   /**
    * Return the name of the destroy method.
    */
   @Override
   @Nullable
   public String getDestroyMethodName() {
      return this.destroyMethodName;
   }

   /**
    * Specify whether or not the configured destroy method is the default.
    * <p>The default value is {@code false}.
    * @see #setDestroyMethodName
    */
   public void setEnforceDestroyMethod(boolean enforceDestroyMethod) {
      this.enforceDestroyMethod = enforceDestroyMethod;
   }

   /**
    * Indicate whether the configured destroy method is the default.
    * @see #getDestroyMethodName
    */
   public boolean isEnforceDestroyMethod() {
      return this.enforceDestroyMethod;
   }

   /**
    * Set whether this artifact definition is 'synthetic', that is, not defined
    * by the application itself (for example, an infrastructure artifact such
    * as a helper for auto-proxying, created through {@code <aop:config>}).
    */
   public void setSynthetic(boolean synthetic) {
      this.synthetic = synthetic;
   }

   /**
    * Return whether this artifact definition is 'synthetic', that is,
    * not defined by the application itself.
    */
   public boolean isSynthetic() {
      return this.synthetic;
   }

   /**
    * Set the role hint for this {@code ArtifactDefinition}.
    */
   @Override
   public void setRole(int role) {
      this.role = role;
   }

   /**
    * Return the role hint for this {@code ArtifactDefinition}.
    */
   @Override
   public int getRole() {
      return this.role;
   }

   /**
    * Set a human-readable description of this artifact definition.
    */
   @Override
   public void setDescription(@Nullable String description) {
      this.description = description;
   }

   /**
    * Return a human-readable description of this artifact definition.
    */
   @Override
   @Nullable
   public String getDescription() {
      return this.description;
   }

   /**
    * Set the resource that this artifact definition came from
    * (for the purpose of showing context in case of errors).
    */
   public void setResource(@Nullable Resource resource) {
      this.resource = resource;
   }

   /**
    * Return the resource that this artifact definition came from.
    */
   @Nullable
   public Resource getResource() {
      return this.resource;
   }

   /**
    * Set a description of the resource that this artifact definition
    * came from (for the purpose of showing context in case of errors).
    */
   public void setResourceDescription(@Nullable String resourceDescription) {
      this.resource = (resourceDescription != null ? new DescriptiveResource(resourceDescription) : null);
   }

   /**
    * Return a description of the resource that this artifact definition
    * came from (for the purpose of showing context in case of errors).
    */
   @Override
   @Nullable
   public String getResourceDescription() {
      return (this.resource != null ? this.resource.getDescription() : null);
   }

   /**
    * Set the originating (e.g. decorated) ArtifactDefinition, if any.
    */
   public void setOriginatingArtifactDefinition(ArtifactDefinition originatingBd) {
      this.resource = new ArtifactDefinitionResource(originatingBd);
   }

   /**
    * Return the originating ArtifactDefinition, or {@code null} if none.
    * Allows for retrieving the decorated artifact definition, if any.
    * <p>Note that this method returns the immediate originator. Iterate through the
    * originator chain to find the original ArtifactDefinition as defined by the user.
    */
   @Override
   @Nullable
   public ArtifactDefinition getOriginatingArtifactDefinition() {
      return (this.resource instanceof ArtifactDefinitionResource ?
         ((ArtifactDefinitionResource) this.resource).getArtifactDefinition() : null);
   }

   /**
    * Validate this artifact definition.
    * @throws ArtifactDefinitionValidationException in case of validation failure
    */
   public void validate() throws ArtifactDefinitionValidationException {
      if (hasMethodOverrides() && getFactoryMethodName() != null) {
         throw new ArtifactDefinitionValidationException(
            "Cannot combine factory method with container-generated method overrides: " +
               "the factory method must create the concrete artifact instance.");
      }
      if (hasArtifactClass()) {
         prepareMethodOverrides();
      }
   }

   /**
    * Validate and prepare the method overrides defined for this artifact.
    * Checks for existence of a method with the specified name.
    * @throws ArtifactDefinitionValidationException in case of validation failure
    */
   public void prepareMethodOverrides() throws ArtifactDefinitionValidationException {
      // Check that lookup methods exist and determine their overloaded status.
      if (hasMethodOverrides()) {
         getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
      }
   }

   /**
    * Validate and prepare the given method override.
    * Checks for existence of a method with the specified name,
    * marking it as not overloaded if none found.
    * @param mo the MethodOverride object to validate
    * @throws ArtifactDefinitionValidationException in case of validation failure
    */
   protected void prepareMethodOverride(MethodOverride mo) throws ArtifactDefinitionValidationException {
      int count = ClassUtils.getMethodCountForName(getArtifactClass(), mo.getMethodName());
      if (count == 0) {
         throw new ArtifactDefinitionValidationException(
            "Invalid method override: no method with name '" + mo.getMethodName() +
               "' on class [" + getArtifactClassName() + "]");
      }
      else if (count == 1) {
         // Mark override as not overloaded, to avoid the overhead of arg type checking.
         mo.setOverloaded(false);
      }
   }


   /**
    * Public declaration of Object's {@code clone()} method.
    * Delegates to {@link #cloneArtifactDefinition()}.
    * @see Object#clone()
    */
   @Override
   public Object clone() {
      return cloneArtifactDefinition();
   }

   /**
    * Clone this artifact definition.
    * To be implemented by concrete subclasses.
    * @return the cloned artifact definition object
    */
   public abstract AbstractArtifactDefinition cloneArtifactDefinition();

   @Override
   public boolean equals(@Nullable Object other) {
      if (this == other) {
         return true;
      }
      if (!(other instanceof AbstractArtifactDefinition)) {
         return false;
      }
      AbstractArtifactDefinition that = (AbstractArtifactDefinition) other;
      return (ObjectUtils.nullSafeEquals(getArtifactClassName(), that.getArtifactClassName()) &&
         ObjectUtils.nullSafeEquals(this.scope, that.scope) &&
         this.abstractFlag == that.abstractFlag &&
         this.lazyInit == that.lazyInit &&
         this.autowireMode == that.autowireMode &&
         this.dependencyCheck == that.dependencyCheck &&
         Arrays.equals(this.dependsOn, that.dependsOn) &&
         this.autowireCandidate == that.autowireCandidate &&
         ObjectUtils.nullSafeEquals(this.qualifiers, that.qualifiers) &&
         this.primary == that.primary &&
         this.nonPublicAccessAllowed == that.nonPublicAccessAllowed &&
         this.lenientConstructorResolution == that.lenientConstructorResolution &&
         ObjectUtils.nullSafeEquals(this.constructorArgumentValues, that.constructorArgumentValues) &&
         ObjectUtils.nullSafeEquals(this.propertyValues, that.propertyValues) &&
         ObjectUtils.nullSafeEquals(this.methodOverrides, that.methodOverrides) &&
         ObjectUtils.nullSafeEquals(this.factoryArtifactName, that.factoryArtifactName) &&
         ObjectUtils.nullSafeEquals(this.factoryMethodName, that.factoryMethodName) &&
         ObjectUtils.nullSafeEquals(this.initMethodName, that.initMethodName) &&
         this.enforceInitMethod == that.enforceInitMethod &&
         ObjectUtils.nullSafeEquals(this.destroyMethodName, that.destroyMethodName) &&
         this.enforceDestroyMethod == that.enforceDestroyMethod &&
         this.synthetic == that.synthetic &&
         this.role == that.role &&
         super.equals(other));
   }

   @Override
   public int hashCode() {
      int hashCode = ObjectUtils.nullSafeHashCode(getArtifactClassName());
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.scope);
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.constructorArgumentValues);
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.propertyValues);
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryArtifactName);
      hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.factoryMethodName);
      hashCode = 29 * hashCode + super.hashCode();
      return hashCode;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("class [");
      sb.append(getArtifactClassName()).append("]");
      sb.append("; scope=").append(this.scope);
      sb.append("; abstract=").append(this.abstractFlag);
      sb.append("; lazyInit=").append(this.lazyInit);
      sb.append("; autowireMode=").append(this.autowireMode);
      sb.append("; dependencyCheck=").append(this.dependencyCheck);
      sb.append("; autowireCandidate=").append(this.autowireCandidate);
      sb.append("; primary=").append(this.primary);
      sb.append("; factoryArtifactName=").append(this.factoryArtifactName);
      sb.append("; factoryMethodName=").append(this.factoryMethodName);
      sb.append("; initMethodName=").append(this.initMethodName);
      sb.append("; destroyMethodName=").append(this.destroyMethodName);
      if (this.resource != null) {
         sb.append("; defined in ").append(this.resource.getDescription());
      }
      return sb.toString();
   }
}
