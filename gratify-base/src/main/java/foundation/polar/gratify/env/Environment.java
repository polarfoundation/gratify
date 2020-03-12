package foundation.polar.gratify.env;

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the gratify-beans 3.1 schema
 * or the {@link foundation.polar.gratify.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the environment object with relation to properties is to
 * provide the user with a convenient service interface for configuring property sources
 * and resolving properties from them.
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * foundation.polar.gratify.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 *
 * <p>In most cases, however, application-level beans should not need to interact with the
 * {@code Environment} directly but instead may have to have {@code ${...}} property
 * values replaced by a property placeholder configurer such as
 * {@link foundation.polar.gratify.context.support.PropertySourcesPlaceholderConfigurer
 * PropertySourcesPlaceholderConfigurer}, which itself is {@code EnvironmentAware} and
 * as of Spring 3.1 is registered by default when using
 * {@code <context:property-placeholder/>}.
 *
 * <p>Configuration of the environment object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 *
 * @author Chris Beams
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see foundation.polar.gratify.di.EnvironmentAware
 * @see foundation.polar.gratify.di.ConfigurableApplicationContext#getEnvironment
 * @see foundation.polar.gratify.di.ConfigurableApplicationContext#setEnvironment
 * @see foundation.polar.gratify.di.support.AbstractApplicationContext#createEnvironment
 */

public interface Environment extends PropertyResolver {
   /**
    * Return the set of profiles explicitly made active for this environment. Profiles
    * are used for creating logical groupings of bean definitions to be registered
    * conditionally, for example based on deployment environment. Profiles can be
    * activated by setting {@linkplain AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
    * "spring.profiles.active"} as a system property or by calling
    * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
    * <p>If no profiles have explicitly been specified as active, then any
    * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
    * @see #getDefaultProfiles
    * @see ConfigurableEnvironment#setActiveProfiles
    * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
    */
   String[] getActiveProfiles();

   /**
    * Return the set of profiles to be active by default when no active profiles have
    * been set explicitly.
    * @see #getActiveProfiles
    * @see ConfigurableEnvironment#setDefaultProfiles
    * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
    */
   String[] getDefaultProfiles();

   /**
    * Return whether the {@linkplain #getActiveProfiles() active profiles}
    * match the given {@link Profiles} predicate.
    */
   boolean acceptsProfiles(Profiles profiles);
}
