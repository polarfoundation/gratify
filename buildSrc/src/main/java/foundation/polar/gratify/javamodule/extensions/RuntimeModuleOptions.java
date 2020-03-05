package foundation.polar.gratify.javamodule.extensions;

import foundation.polar.gratify.javamodule.utils.TaskOption;
import org.gradle.api.Project;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class RuntimeModuleOptions extends ModuleOptions {
   private Map<String, String> addOpens = new LinkedHashMap<>();
   public RuntimeModuleOptions(Project project) {
      super(project);
   }

   public Map<String, String> getAddOpens() {
      return addOpens;
   }
   public void setAddOpens(Map<String, String> addOpens) {
      this.addOpens = addOpens;
   }

   protected Stream<TaskOption> buildFullOptionStream() {
      return Stream.concat(
         super.buildFullOptionStream(),
         addOpensOptionStream()
      );
   }

   private Stream<TaskOption> addOpensOptionStream() {
      return buildOptionStream("--add-opens", addOpens);
   }
}
