package foundation.polar.gratify.artifacts.propertyeditors;

import foundation.polar.gratify.utils.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.beans.PropertyEditorSupport;

/**
 * Editor for a {@link Character}, to populate a property
 * of type {@code Character} or {@code char} from a String value.
 *
 * <p>Note that the JDK does not contain a default
 * {@link java.beans.PropertyEditor property editor} for {@code char}!
 * {@link foundation.polar.gratify.artifacts.ArtifactWrapperImpl} will register this
 * editor by default.
 *
 * <p>Also supports conversion from a Unicode character sequence; e.g.
 * {@code u0041} ('A').
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 *
 * @see Character
 * @see foundation.polar.gratify.artifacts.ArtifactWrapperImpl
 */
public class CharacterEditor extends PropertyEditorSupport {

   /**
    * The prefix that identifies a string as being a Unicode character sequence.
    */
   private static final String UNICODE_PREFIX = "\\u";

   /**
    * The length of a Unicode character sequence.
    */
   private static final int UNICODE_LENGTH = 6;
   private final boolean allowEmpty;

   /**
    * Create a new CharacterEditor instance.
    * <p>The "allowEmpty" parameter controls whether an empty String is to be
    * allowed in parsing, i.e. be interpreted as the {@code null} value when
    * {@link #setAsText(String) text is being converted}. If {@code false},
    * an {@link IllegalArgumentException} will be thrown at that time.
    * @param allowEmpty if empty strings are to be allowed
    */
   public CharacterEditor(boolean allowEmpty) {
      this.allowEmpty = allowEmpty;
   }

   @Override
   public void setAsText(@Nullable String text) throws IllegalArgumentException {
      if (this.allowEmpty && !StringUtils.hasLength(text)) {
         // Treat empty String as null value.
         setValue(null);
      }
      else if (text == null) {
         throw new IllegalArgumentException("null String cannot be converted to char type");
      }
      else if (isUnicodeCharacterSequence(text)) {
         setAsUnicode(text);
      }
      else if (text.length() == 1) {
         setValue(Character.valueOf(text.charAt(0)));
      }
      else {
         throw new IllegalArgumentException("String [" + text + "] with length " +
            text.length() + " cannot be converted to char type: neither Unicode nor single character");
      }
   }

   @Override
   public String getAsText() {
      Object value = getValue();
      return (value != null ? value.toString() : "");
   }

   private boolean isUnicodeCharacterSequence(String sequence) {
      return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
   }

   private void setAsUnicode(String text) {
      int code = Integer.parseInt(text.substring(UNICODE_PREFIX.length()), 16);
      setValue(Character.valueOf((char) code));
   }
}
