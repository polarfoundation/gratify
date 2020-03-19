package foundation.polar.gratify.core.convert.support;

import foundation.polar.gratify.core.convert.ConversionService;
import foundation.polar.gratify.core.convert.TypeDescriptor;
import foundation.polar.gratify.utils.AssertUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;

/**
 * Adapter that exposes a {@link java.beans.PropertyEditor} for any given
 * {@link foundation.polar.gratify.core.convert.ConversionService} and specific target type.
 *
 * @author Juergen Hoeller
 */
public class ConvertingPropertyEditorAdapter extends PropertyEditorSupport {

   private final ConversionService conversionService;
   private final TypeDescriptor targetDescriptor;
   private final boolean canConvertToString;

   /**
    * Create a new ConvertingPropertyEditorAdapter for a given
    * {@link foundation.polar.gratify.core.convert.ConversionService}
    * and the given target type.
    * @param conversionService the ConversionService to delegate to
    * @param targetDescriptor the target type to convert to
    */
   public ConvertingPropertyEditorAdapter(ConversionService conversionService, TypeDescriptor targetDescriptor) {
      AssertUtils.notNull(conversionService, "ConversionService must not be null");
      AssertUtils.notNull(targetDescriptor, "TypeDescriptor must not be null");
      this.conversionService = conversionService;
      this.targetDescriptor = targetDescriptor;
      this.canConvertToString = conversionService.canConvert(this.targetDescriptor, TypeDescriptor.valueOf(String.class));
   }

   @Override
   public void setAsText(@Nullable String text) throws IllegalArgumentException {
      setValue(this.conversionService.convert(text, TypeDescriptor.valueOf(String.class), this.targetDescriptor));
   }

   @Override
   @Nullable
   public String getAsText() {
      if (this.canConvertToString) {
         return (String) this.conversionService.convert(getValue(), this.targetDescriptor, TypeDescriptor.valueOf(String.class));
      }
      else {
         return null;
      }
   }
}
