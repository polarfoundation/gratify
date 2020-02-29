package foundation.polar.gratify.annotations.utils;

import java.util.Map;
import java.util.Set;

public interface OptionConfiguration {
   Map<String, String> getOptions();
   boolean hasOption(String name);
   String getOption(String name);
   String getOption(String name, String defaultValue);
   boolean getBooleanOption(String name);
   boolean getBooleanOption(String name, boolean defaultValue);
   Set<String> getSupportedOptions();
}
