package foundation.polar.gratify.core.convert.converter;

/**
 * A factory for "ranged" converters that can convert objects from S to subtypes of R.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @param <S> the source type converters created by this factory can convert from
 * @param <R> the target range (or base) type converters created by this factory can convert to;
 * for example {@link Number} for a set of number subtypes.
 * @see ConditionalConverter
 */
public interface ConverterFactory<S, R> {
   /**
    * Get the converter to convert from S to target type T, where T is also an instance of R.
    * @param <T> the target type
    * @param targetType the target type to convert to
    * @return a converter from S to T
    */
   <T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
