package foundation.polar.gratify.artifacts.factory.annotation;

import java.lang.annotation.*;

/**
 * An annotation that indicates 'lookup' methods, to be overridden by the container
 * to redirect them back to the {@link foundation.polar.gratify.artifacts.factory.ArtifactFactory}
 * for a {@code getArtifact} call. This is essentially an annotation-based version of the
 * XML {@code lookup-method} attribute, resulting in the same runtime arrangement.
 *
 * <p>The resolution of the target bean can either be based on the return type
 * ({@code getArtifact(Class)}) or on a suggested bean name ({@code getArtifact(String)}),
 * in both cases passing the method's arguments to the {@code getArtifact} call
 * for applying them as target factory method arguments or constructor arguments.
 *
 * <p>Such lookup methods can have default (stub) implementations that will simply
 * get replaced by the container, or they can be declared as abstract - for the
 * container to fill them in at runtime. In both cases, the container will generate
 * runtime subclasses of the method's containing class via CGLIB, which is why such
 * lookup methods can only work on beans that the container instantiates through
 * regular constructors: i.e. lookup methods cannot get replaced on beans returned
 * from factory methods where we cannot dynamically provide a subclass for them.
 *
 * <p><b>Concrete limitations in typical Gratify configuration scenarios:</b>
 * When used with component scanning or any other mechanism that filters out abstract
 * beans, provide stub implementations of your lookup methods to be able to declare
 * them as concrete classes. And please remember that lookup methods won't work on
 * beans returned from {@code @Artifact} methods in configuration classes; you'll have
 * to resort to {@code @Inject Provider<TargetArtifact>} or the like instead.
 *
 * @author Juergen Hoeller
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact(Class, Object...)
 * @see foundation.polar.gratify.artifacts.factory.ArtifactFactory#getArtifact(String, Object...)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lookup {
   /**
    * This annotation attribute may suggest a target bean name to look up.
    * If not specified, the target bean will be resolved based on the
    * annotated method's return type declaration.
    */
   String value() default "";
}
