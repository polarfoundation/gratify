package foundation.polar.gratify.core.convert.converter;

/**
 * A {@link GenericConverter} that may conditionally execute based on attributes
 * of the {@code source} and {@code target} {@link TypeDescriptor}.
 *
 * <p>See {@link ConditionalConverter} for details.
 *
 * @author Keith Donald
 * @author Phillip Webb
 * @see GenericConverter
 * @see ConditionalConverter
 */
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {

}