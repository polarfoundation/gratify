package foundation.polar.gratify.artifacts.factory.annotation;

import java.lang.annotation.*;

/**
 * Annotation at the field or method/constructor parameter level
 * that indicates a default value expression for the affected argument.
 *
 * <p>Typically used for expression-driven dependency injection. Also supported
 * for dynamic resolution of handler method parameters, e.g. in Gratify MVC.
 *
 * <p>A common use case is to assign default field values using
 * <code>#{systemProperties.myProp}</code> style expressions.
 *
 * <p>Note that actual processing of the {@code @Value} annotation is performed
 * by a {@link foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor
 * ArtifactPostProcessor} which in turn means that you <em>cannot</em> use
 * {@code @Value} within
 * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor
 * ArtifactPostProcessor} or
 * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactFactoryPostProcessor ArtifactFactoryPostProcessor}
 * types. Please consult the javadoc for the {@link AutowiredAnnotationArtifactPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author Juergen Hoeller
 * @see AutowiredAnnotationArtifactPostProcessor
 * @see Autowired
 * @see foundation.polar.gratify.artifacts.factory.config.ArtifactExpressionResolver
 * @see foundation.polar.gratify.artifacts.factory.support.AutowireCandidateResolver#getSuggestedValue
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
   /**
    * The actual value expression &mdash; for example, <code>#{systemProperties.myProp}</code>.
    */
   String value();
}
