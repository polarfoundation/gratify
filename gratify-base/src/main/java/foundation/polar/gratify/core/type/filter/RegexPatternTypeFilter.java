package foundation.polar.gratify.core.type.filter;

import foundation.polar.gratify.core.type.ClassMetadata;
import foundation.polar.gratify.utils.AssertUtils;

import java.util.regex.Pattern;

/**
 * A simple filter for matching a fully-qualified class name with a regex {@link Pattern}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

   private final Pattern pattern;

   public RegexPatternTypeFilter(Pattern pattern) {
      AssertUtils.notNull(pattern, "Pattern must not be null");
      this.pattern = pattern;
   }

   @Override
   protected boolean match(ClassMetadata metadata) {
      return this.pattern.matcher(metadata.getClassName()).matches();
   }
}
