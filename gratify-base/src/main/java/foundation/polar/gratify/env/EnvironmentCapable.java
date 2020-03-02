package foundation.polar.gratify.env;


/**
 * Interface indicating a component that contains and exposes an {@link Environment} reference.
 *
 * <p>All Spring application contexts are EnvironmentCapable, and the interface is used primarily
 * for performing {@code instanceof} checks in framework methods that accept BeanFactory
 * instances that may or may not actually be ApplicationContext instances in order to interact
 * with the environment if indeed it is available.
 *
 * <p>As mentioned, {@link foundation.polar.gratify.context.ApplicationContext ApplicationContext}
 * extends EnvironmentCapable, and thus exposes a {@link #getEnvironment()} method; however,
 * {@link foundation.polar.gratify.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * redefines {@link foundation.polar.gratify.context.ConfigurableApplicationContext#getEnvironment
 * getEnvironment()} and narrows the signature to return a {@link ConfigurableEnvironment}.
 * The effect is that an Environment object is 'read-only' until it is being accessed from
 * a ConfigurableApplicationContext, at which point it too may be configured.
 *
 * @author Chris Beams
 * @see Environment
 * @see ConfigurableEnvironment
 * @see foundation.polar.gratify.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface EnvironmentCapable {

   /**
    * Return the {@link Environment} associated with this component.
    */
   Environment getEnvironment();

}
