package foundation.polar.gratify.artifacts.factory.annotation;

import foundation.polar.gratify.annotation.AnnotatedElementUtils;
import foundation.polar.gratify.annotation.AnnotationAttributes;
import foundation.polar.gratify.annotation.AnnotationUtils;
import foundation.polar.gratify.artifacts.SimpleTypeConverter;
import foundation.polar.gratify.artifacts.TypeConverter;
import foundation.polar.gratify.artifacts.factory.NoSuchArtifactDefinitionException;
import foundation.polar.gratify.artifacts.factory.config.ArtifactDefinitionHolder;
import foundation.polar.gratify.artifacts.factory.config.DependencyDescriptor;
import foundation.polar.gratify.artifacts.factory.support.AutowireCandidateQualifier;
import foundation.polar.gratify.artifacts.factory.support.GenericTypeAwareAutowireCandidateResolver;
import foundation.polar.gratify.artifacts.factory.support.RootArtifactDefinition;
import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import foundation.polar.gratify.utils.ObjectUtils;
import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean definition qualifiers
 * against {@link Qualifier qualifier annotations} on the field or parameter to be autowired.
 * Also supports suggested expression values through a {@link Value value} annotation.
 *
 * <p>Also supports JSR-330's {@link javax.inject.Qualifier} annotation, if available.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see AutowireCandidateQualifier
 * @see Qualifier
 * @see Value
 */
public class QualifierAnnotationAutowireCandidateResolver extends GenericTypeAwareAutowireCandidateResolver {

   private final Set<Class<? extends Annotation>> qualifierTypes = new LinkedHashSet<>(2);

   private Class<? extends Annotation> valueAnnotationType = Value.class;

   /**
    * Create a new QualifierAnnotationAutowireCandidateResolver
    * for Gratify's standard {@link Qualifier} annotation.
    * <p>Also supports JSR-330's {@link javax.inject.Qualifier} annotation, if available.
    */
   @SuppressWarnings("unchecked")
   public QualifierAnnotationAutowireCandidateResolver() {
      this.qualifierTypes.add(Qualifier.class);
      try {
         this.qualifierTypes.add((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Qualifier",
            QualifierAnnotationAutowireCandidateResolver.class.getClassLoader()));
      }
      catch (ClassNotFoundException ex) {
         // JSR-330 API not available - simply skip.
      }
   }

   /**
    * Create a new QualifierAnnotationAutowireCandidateResolver
    * for the given qualifier annotation type.
    * @param qualifierType the qualifier annotation to look for
    */
   public QualifierAnnotationAutowireCandidateResolver(Class<? extends Annotation> qualifierType) {
      AssertUtils.notNull(qualifierType, "'qualifierType' must not be null");
      this.qualifierTypes.add(qualifierType);
   }

   /**
    * Create a new QualifierAnnotationAutowireCandidateResolver
    * for the given qualifier annotation types.
    * @param qualifierTypes the qualifier annotations to look for
    */
   public QualifierAnnotationAutowireCandidateResolver(Set<Class<? extends Annotation>> qualifierTypes) {
      AssertUtils.notNull(qualifierTypes, "'qualifierTypes' must not be null");
      this.qualifierTypes.addAll(qualifierTypes);
   }

   /**
    * Register the given type to be used as a qualifier when autowiring.
    * <p>This identifies qualifier annotations for direct use (on fields,
    * method parameters and constructor parameters) as well as meta
    * annotations that in turn identify actual qualifier annotations.
    * <p>This implementation only supports annotations as qualifier types.
    * The default is Gratify's {@link Qualifier} annotation which serves
    * as a qualifier for direct use and also as a meta annotation.
    * @param qualifierType the annotation type to register
    */
   public void addQualifierType(Class<? extends Annotation> qualifierType) {
      this.qualifierTypes.add(qualifierType);
   }

   /**
    * Set the 'value' annotation type, to be used on fields, method parameters
    * and constructor parameters.
    * <p>The default value annotation type is the Gratify-provided
    * {@link Value} annotation.
    * <p>This setter property exists so that developers can provide their own
    * (non-Gratify-specific) annotation type to indicate a default value
    * expression for a specific argument.
    */
   public void setValueAnnotationType(Class<? extends Annotation> valueAnnotationType) {
      this.valueAnnotationType = valueAnnotationType;
   }

   /**
    * Determine whether the provided bean definition is an autowire candidate.
    * <p>To be considered a candidate the bean's <em>autowire-candidate</em>
    * attribute must not have been set to 'false'. Also, if an annotation on
    * the field or parameter to be autowired is recognized by this bean factory
    * as a <em>qualifier</em>, the bean must 'match' against the annotation as
    * well as any attributes it may contain. The bean definition must contain
    * the same qualifier or match by meta attributes. A "value" attribute will
    * fallback to match against the bean name or an alias if a qualifier or
    * attribute does not match.
    * @see Qualifier
    */
   @Override
   public boolean isAutowireCandidate(ArtifactDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      boolean match = super.isAutowireCandidate(bdHolder, descriptor);
      if (match) {
         match = checkQualifiers(bdHolder, descriptor.getAnnotations());
         if (match) {
            MethodParameter methodParam = descriptor.getMethodParameter();
            if (methodParam != null) {
               Method method = methodParam.getMethod();
               if (method == null || void.class == method.getReturnType()) {
                  match = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
               }
            }
         }
      }
      return match;
   }

   /**
    * Match the given qualifier annotations against the candidate bean definition.
    */
   protected boolean checkQualifiers(ArtifactDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {
      if (ObjectUtils.isEmpty(annotationsToSearch)) {
         return true;
      }
      SimpleTypeConverter typeConverter = new SimpleTypeConverter();
      for (Annotation annotation : annotationsToSearch) {
         Class<? extends Annotation> type = annotation.annotationType();
         boolean checkMeta = true;
         boolean fallbackToMeta = false;
         if (isQualifier(type)) {
            if (!checkQualifier(bdHolder, annotation, typeConverter)) {
               fallbackToMeta = true;
            }
            else {
               checkMeta = false;
            }
         }
         if (checkMeta) {
            boolean foundMeta = false;
            for (Annotation metaAnn : type.getAnnotations()) {
               Class<? extends Annotation> metaType = metaAnn.annotationType();
               if (isQualifier(metaType)) {
                  foundMeta = true;
                  // Only accept fallback match if @Qualifier annotation has a value...
                  // Otherwise it is just a marker for a custom qualifier annotation.
                  if ((fallbackToMeta && StringUtils.isEmpty(AnnotationUtils.getValue(metaAnn))) ||
                     !checkQualifier(bdHolder, metaAnn, typeConverter)) {
                     return false;
                  }
               }
            }
            if (fallbackToMeta && !foundMeta) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * Checks whether the given annotation type is a recognized qualifier type.
    */
   protected boolean isQualifier(Class<? extends Annotation> annotationType) {
      for (Class<? extends Annotation> qualifierType : this.qualifierTypes) {
         if (annotationType.equals(qualifierType) || annotationType.isAnnotationPresent(qualifierType)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Match the given qualifier annotation against the candidate bean definition.
    */
   protected boolean checkQualifier(
      ArtifactDefinitionHolder bdHolder, Annotation annotation, TypeConverter typeConverter) {
      Class<? extends Annotation> type = annotation.annotationType();
      RootArtifactDefinition bd = (RootArtifactDefinition) bdHolder.getArtifactDefinition();

      AutowireCandidateQualifier qualifier = bd.getQualifier(type.getName());
      if (qualifier == null) {
         qualifier = bd.getQualifier(ClassUtils.getShortName(type));
      }
      if (qualifier == null) {
         // First, check annotation on qualified element, if any
         Annotation targetAnnotation = getQualifiedElementAnnotation(bd, type);
         // Then, check annotation on factory method, if applicable
         if (targetAnnotation == null) {
            targetAnnotation = getFactoryMethodAnnotation(bd, type);
         }
         if (targetAnnotation == null) {
            RootArtifactDefinition dbd = getResolvedDecoratedDefinition(bd);
            if (dbd != null) {
               targetAnnotation = getFactoryMethodAnnotation(dbd, type);
            }
         }
         if (targetAnnotation == null) {
            // Look for matching annotation on the target class
            if (getArtifactFactory() != null) {
               try {
                  Class<?> beanType = getArtifactFactory().getType(bdHolder.getArtifactName());
                  if (beanType != null) {
                     targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(beanType), type);
                  }
               }
               catch (NoSuchArtifactDefinitionException ex) {
                  // Not the usual case - simply forget about the type check...
               }
            }
            if (targetAnnotation == null && bd.hasArtifactClass()) {
               targetAnnotation = AnnotationUtils.getAnnotation(ClassUtils.getUserClass(bd.getArtifactClass()), type);
            }
         }
         if (targetAnnotation != null && targetAnnotation.equals(annotation)) {
            return true;
         }
      }

      Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
      if (attributes.isEmpty() && qualifier == null) {
         // If no attributes, the qualifier must be present
         return false;
      }
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
         String attributeName = entry.getKey();
         Object expectedValue = entry.getValue();
         Object actualValue = null;
         // Check qualifier first
         if (qualifier != null) {
            actualValue = qualifier.getAttribute(attributeName);
         }
         if (actualValue == null) {
            // Fall back on bean definition attribute
            actualValue = bd.getAttribute(attributeName);
         }
         if (actualValue == null && attributeName.equals(AutowireCandidateQualifier.VALUE_KEY) &&
            expectedValue instanceof String && bdHolder.matchesName((String) expectedValue)) {
            // Fall back on bean name (or alias) match
            continue;
         }
         if (actualValue == null && qualifier != null) {
            // Fall back on default, but only if the qualifier is present
            actualValue = AnnotationUtils.getDefaultValue(annotation, attributeName);
         }
         if (actualValue != null) {
            actualValue = typeConverter.convertIfNecessary(actualValue, expectedValue.getClass());
         }
         if (!expectedValue.equals(actualValue)) {
            return false;
         }
      }
      return true;
   }

   @Nullable
   protected Annotation getQualifiedElementAnnotation(RootArtifactDefinition bd, Class<? extends Annotation> type) {
      AnnotatedElement qualifiedElement = bd.getQualifiedElement();
      return (qualifiedElement != null ? AnnotationUtils.getAnnotation(qualifiedElement, type) : null);
   }

   @Nullable
   protected Annotation getFactoryMethodAnnotation(RootArtifactDefinition bd, Class<? extends Annotation> type) {
      Method resolvedFactoryMethod = bd.getResolvedFactoryMethod();
      return (resolvedFactoryMethod != null ? AnnotationUtils.getAnnotation(resolvedFactoryMethod, type) : null);
   }


   /**
    * Determine whether the given dependency declares an autowired annotation,
    * checking its required flag.
    * @see Autowired#required()
    */
   @Override
   public boolean isRequired(DependencyDescriptor descriptor) {
      if (!super.isRequired(descriptor)) {
         return false;
      }
      Autowired autowired = descriptor.getAnnotation(Autowired.class);
      return (autowired == null || autowired.required());
   }

   /**
    * Determine whether the given dependency declares a qualifier annotation.
    * @see #isQualifier(Class)
    * @see Qualifier
    */
   @Override
   public boolean hasQualifier(DependencyDescriptor descriptor) {
      for (Annotation ann : descriptor.getAnnotations()) {
         if (isQualifier(ann.annotationType())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine whether the given dependency declares a value annotation.
    * @see Value
    */
   @Override
   @Nullable
   public Object getSuggestedValue(DependencyDescriptor descriptor) {
      Object value = findValue(descriptor.getAnnotations());
      if (value == null) {
         MethodParameter methodParam = descriptor.getMethodParameter();
         if (methodParam != null) {
            value = findValue(methodParam.getMethodAnnotations());
         }
      }
      return value;
   }

   /**
    * Determine a suggested value from any of the given candidate annotations.
    */
   @Nullable
   protected Object findValue(Annotation[] annotationsToSearch) {
      if (annotationsToSearch.length > 0) {   // qualifier annotations have to be local
         AnnotationAttributes attr = AnnotatedElementUtils.getMergedAnnotationAttributes(
            AnnotatedElementUtils.forAnnotations(annotationsToSearch), this.valueAnnotationType);
         if (attr != null) {
            return extractValue(attr);
         }
      }
      return null;
   }

   /**
    * Extract the value attribute from the given annotation.
    */
   protected Object extractValue(AnnotationAttributes attr) {
      Object value = attr.get(AnnotationUtils.VALUE);
      if (value == null) {
         throw new IllegalStateException("Value annotation must have a value attribute");
      }
      return value;
   }
}
