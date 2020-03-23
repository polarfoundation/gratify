package foundation.polar.gratify.artifacts.factory;

/**
 * A marker superinterface indicating that a bean is eligible to be notified by the
 * Gratify container of a particular framework object through a callback-style method.
 * The actual method signature is determined by individual subinterfaces but should
 * typically consist of just one void-returning method that accepts a single argument.
 *
 * <p>Note that merely implementing {@link Aware} provides no default functionality.
 * Rather, processing must be done explicitly, for example in a
 * {@link foundation.polar.gratify.artifacts.factory.config.ArtifactPostProcessor}.
 * Refer to {@link foundation.polar.gratify.context.support.ApplicationContextAwareProcessor}
 * for an example of processing specific {@code *Aware} interface callbacks.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public interface Aware {
}
