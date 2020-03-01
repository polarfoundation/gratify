package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionFailedException;
import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.core.convert.converter.GenericConverter;
import foundation.polar.gratify.utils.AssertUtils;
import foundation.polar.gratify.utils.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Internal utilities for the conversion package.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 */
abstract class ConversionUtils {
   @Nullable
   public static Object invokeConverter(GenericConverter converter, @Nullable Object source,
                                        TypeDescriptor sourceType, TypeDescriptor targetType) {

      try {
         return converter.convert(source, sourceType, targetType);
      }
      catch (ConversionFailedException ex) {
         throw ex;
      }
      catch (Throwable ex) {
         throw new ConversionFailedException(sourceType, targetType, source, ex);
      }
   }

   public static boolean canConvertElements(@Nullable TypeDescriptor sourceElementType,
                                            @Nullable TypeDescriptor targetElementType, ConversionService conversionService) {

      if (targetElementType == null) {
         // yes
         return true;
      }
      if (sourceElementType == null) {
         // maybe
         return true;
      }
      if (conversionService.canConvert(sourceElementType, targetElementType)) {
         // yes
         return true;
      }
      if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
         // maybe
         return true;
      }
      // no
      return false;
   }

   public static Class<?> getEnumType(Class<?> targetType) {
      Class<?> enumType = targetType;
      while (enumType != null && !enumType.isEnum()) {
         enumType = enumType.getSuperclass();
      }
      AssertUtils.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
      return enumType;
   }
}
