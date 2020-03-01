package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.converter.Converter;
import foundation.polar.gratify.core.convert.converter.ConverterFactory;
import foundation.polar.gratify.core.convert.converter.ConverterRegistry;
import foundation.polar.gratify.core.convert.converter.GenericConverter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * A factory for common {@link foundation.polar.gratify.core.convert.ConversionService}
 * configurations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class ConversionServiceFactory {

   private ConversionServiceFactory() {
   }

   /**
    * Register the given Converter objects with the given target ConverterRegistry.
    * @param converters the converter objects: implementing {@link Converter},
    * {@link ConverterFactory}, or {@link GenericConverter}
    * @param registry the target registry
    */
   public static void registerConverters(@Nullable Set<?> converters, ConverterRegistry registry) {
      if (converters != null) {
         for (Object converter : converters) {
            if (converter instanceof GenericConverter) {
               registry.addConverter((GenericConverter) converter);
            }
            else if (converter instanceof Converter<?, ?>) {
               registry.addConverter((Converter<?, ?>) converter);
            }
            else if (converter instanceof ConverterFactory<?, ?>) {
               registry.addConverterFactory((ConverterFactory<?, ?>) converter);
            }
            else {
               throw new IllegalArgumentException("Each converter object must implement one of the " +
                  "Converter, ConverterFactory, or GenericConverter interfaces");
            }
         }
      }
   }
}

