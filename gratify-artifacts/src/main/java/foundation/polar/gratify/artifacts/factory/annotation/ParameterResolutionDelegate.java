package foundation.polar.gratify.artifacts.factory.annotation;


import foundation.polar.gratify.annotation.AnnotatedElementUtils;
import foundation.polar.gratify.annotation.SynthesizingMethodParameter;
import foundation.polar.gratify.artifacts.ArtifactsException;
import foundation.polar.gratify.artifacts.factory.config.AutowireCapableArtifactFactory;
import foundation.polar.gratify.artifacts.factory.config.DependencyDescriptor;
import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

/**
 * Public delegate for resolving autowirable parameters on externally managed
 * constructors and methods.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see #isAutowirable
 * @see #resolveDependency
 */
public final class ParameterResolutionDelegate {

   private static final AnnotatedElement EMPTY_ANNOTATED_ELEMENT = new AnnotatedElement() {
      @Override
      @Nullable
      public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
         return null;
      }
      @Override
      public Annotation[] getAnnotations() {
         return new Annotation[0];
      }
      @Override
      public Annotation[] getDeclaredAnnotations() {
         return new Annotation[0];
      }
   };

   private ParameterResolutionDelegate() {
   }

   /**
    * Determine if the supplied {@link Parameter} can <em>potentially</em> be
    * autowired from an {@link AutowireCapableArtifactFactory}.
    * <p>Returns {@code true} if the supplied parameter is annotated or
    * meta-annotated with {@link Autowired @Autowired},
    * {@link Qualifier @Qualifier}, or {@link Value @Value}.
    * <p>Note that {@link #resolveDependency} may still be able to resolve the
    * dependency for the supplied parameter even if this method returns {@code false}.
    * @param parameter the parameter whose dependency should be autowired
    * (must not be {@code null})
    * @param parameterIndex the index of the parameter in the constructor or method
    * that declares the parameter
    * @see #resolveDependency
    */
   public static boolean isAutowirable(Parameter parameter, int parameterIndex) {
      AssertUtils.notNull(parameter, "Parameter must not be null");
      AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
      return (AnnotatedElementUtils.hasAnnotation(annotatedParameter, Autowired.class) ||
         AnnotatedElementUtils.hasAnnotation(annotatedParameter, Qualifier.class) ||
         AnnotatedElementUtils.hasAnnotation(annotatedParameter, Value.class));
   }

   /**
    * Resolve the dependency for the supplied {@link Parameter} from the
    * supplied {@link AutowireCapableArtifactFactory}.
    * <p>Provides comprehensive autowiring support for individual method parameters
    * on par with Gratify's dependency injection facilities for autowired fields and
    * methods, including support for {@link Autowired @Autowired},
    * {@link Qualifier @Qualifier}, and {@link Value @Value} with support for property
    * placeholders and SpEL expressions in {@code @Value} declarations.
    * <p>The dependency is required unless the parameter is annotated or meta-annotated
    * with {@link Autowired @Autowired} with the {@link Autowired#required required}
    * flag set to {@code false}.
    * <p>If an explicit <em>qualifier</em> is not declared, the name of the parameter
    * will be used as the qualifier for resolving ambiguities.
    * @param parameter the parameter whose dependency should be resolved (must not be
    * {@code null})
    * @param parameterIndex the index of the parameter in the constructor or method
    * that declares the parameter
    * @param containingClass the concrete class that contains the parameter; this may
    * differ from the class that declares the parameter in that it may be a subclass
    * thereof, potentially substituting type variables (must not be {@code null})
    * @param beanFactory the {@code AutowireCapableArtifactFactory} from which to resolve
    * the dependency (must not be {@code null})
    * @return the resolved object, or {@code null} if none found
    * @throws ArtifactsException if dependency resolution failed
    * @see #isAutowirable
    * @see Autowired#required
    * @see SynthesizingMethodParameter#forExecutable(Executable, int)
    * @see AutowireCapableArtifactFactory#resolveDependency(DependencyDescriptor, String)
    */
   @Nullable
   public static Object resolveDependency(
      Parameter parameter, int parameterIndex, Class<?> containingClass, AutowireCapableArtifactFactory beanFactory)
      throws ArtifactsException {

      AssertUtils.notNull(parameter, "Parameter must not be null");
      AssertUtils.notNull(containingClass, "Containing class must not be null");
      AssertUtils.notNull(beanFactory, "AutowireCapableArtifactFactory must not be null");

      AnnotatedElement annotatedParameter = getEffectiveAnnotatedParameter(parameter, parameterIndex);
      Autowired autowired = AnnotatedElementUtils.findMergedAnnotation(annotatedParameter, Autowired.class);
      boolean required = (autowired == null || autowired.required());

      MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(
         parameter.getDeclaringExecutable(), parameterIndex);
      DependencyDescriptor descriptor = new DependencyDescriptor(methodParameter, required);
      descriptor.setContainingClass(containingClass);
      return beanFactory.resolveDependency(descriptor, null);
   }

   /**
    * Due to a bug in {@code javac} on JDK versions prior to JDK 9, looking up
    * annotations directly on a {@link Parameter} will fail for inner class
    * constructors.
    * <h4>Bug in javac in JDK &lt; 9</h4>
    * <p>The parameter annotations array in the compiled byte code excludes an entry
    * for the implicit <em>enclosing instance</em> parameter for an inner class
    * constructor.
    * <h4>Workaround</h4>
    * <p>This method provides a workaround for this off-by-one error by allowing the
    * caller to access annotations on the preceding {@link Parameter} object (i.e.,
    * {@code index - 1}). If the supplied {@code index} is zero, this method returns
    * an empty {@code AnnotatedElement}.
    * <h4>WARNING</h4>
    * <p>The {@code AnnotatedElement} returned by this method should never be cast and
    * treated as a {@code Parameter} since the metadata (e.g., {@link Parameter#getName()},
    * {@link Parameter#getType()}, etc.) will not match those for the declared parameter
    * at the given index in an inner class constructor.
    * @return the supplied {@code parameter} or the <em>effective</em> {@code Parameter}
    * if the aforementioned bug is in effect
    */
   private static AnnotatedElement getEffectiveAnnotatedParameter(Parameter parameter, int index) {
      Executable executable = parameter.getDeclaringExecutable();
      if (executable instanceof Constructor && ClassUtils.isInnerClass(executable.getDeclaringClass()) &&
         executable.getParameterAnnotations().length == executable.getParameterCount() - 1) {
         // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
         // for inner classes, so access it with the actual parameter index lowered by 1
         return (index == 0 ? EMPTY_ANNOTATED_ELEMENT : executable.getParameters()[index - 1]);
      }
      return parameter;
   }
}
