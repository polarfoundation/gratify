package foundation.polar.gratify.artifacts;


import foundation.polar.gratify.core.MethodParameter;
import foundation.polar.gratify.core.convert.ConversionException;
import foundation.polar.gratify.core.convert.ConverterNotFoundException;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;

/**
 * Base implementation of the {@link TypeConverter} interface, using a package-private delegate.
 * Mainly serves as base class for {@link ArtifactWrapperImpl}.
 *
 * @author Juergen Hoeller
 * @see SimpleTypeConverter
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {

   @Nullable
   TypeConverterDelegate typeConverterDelegate;

   @Override
   @Nullable
   public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
      return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
   }

   @Override
   @Nullable
   public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                   @Nullable MethodParameter methodParam) throws TypeMismatchException {

      return convertIfNecessary(value, requiredType,
         (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
   }

   @Override
   @Nullable
   public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
      throws TypeMismatchException {

      return convertIfNecessary(value, requiredType,
         (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
   }

   @Nullable
   @Override
   public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                   @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

      AssertUtils.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
      try {
         return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
      }
      catch (ConverterNotFoundException | IllegalStateException ex) {
         throw new ConversionNotSupportedException(value, requiredType, ex);
      }
      catch (ConversionException | IllegalArgumentException ex) {
         throw new TypeMismatchException(value, requiredType, ex);
      }
   }

}
