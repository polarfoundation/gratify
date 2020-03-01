package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.converter.ConverterRegistry;

/**
 * Configuration interface to be implemented by most if not all {@link ConversionService}
 * types. Consolidates the read-only operations exposed by {@link ConversionService} and
 * the mutating operations of {@link ConverterRegistry} to allow for convenient ad-hoc
 * addition and removal of {@link foundation.polar.gratify.core.convert.converter.Converter
 * Converters} through. The latter is particularly useful when working against a
 * {@link foundation.polar.gratify.env.ConfigurableEnvironment ConfigurableEnvironment}
 * instance in application context bootstrapping code.
 *
 * @author Chris Beams
 * @since 3.1
 * @see foundation.polar.gratify.env.ConfigurablePropertyResolver#getConversionService()
 * @see foundation.polar.gratify.env.ConfigurableEnvironment
 * @see foundation.polar.gratify.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface ConfigurableConversionService extends ConversionService, ConverterRegistry {}

