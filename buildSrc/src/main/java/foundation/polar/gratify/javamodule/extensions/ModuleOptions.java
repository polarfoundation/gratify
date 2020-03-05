package foundation.polar.gratify.javamodule.extensions;

import foundation.polar.gratify.javamodule.JavaProjectHelper;
import foundation.polar.gratify.javamodule.utils.MergeClassesHelper;
import foundation.polar.gratify.javamodule.utils.StreamHelper;
import foundation.polar.gratify.javamodule.utils.TaskOption;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.*;
import java.util.stream.Stream;

public abstract class ModuleOptions {
   private static final Logger LOGGER = Logging.getLogger(ModuleOptions.class);
   private final Project project;

   private List<String> addModules = new ArrayList<>();
   private Map<String, String> addReads = new LinkedHashMap<>();
   private Map<String, String> addExports = new LinkedHashMap<>();

   protected ModuleOptions(Project project) {
      this.project = project;
   }
   public List<String> getAddModules() {
      return addModules;
   }
   public void setAddModules(List<String> addModules) {
      this.addModules = addModules;
   }
   public Map<String, String> getAddReads() {
      return addReads;
   }
   public void setAddReads(Map<String, String> addReads) {
      this.addReads = addReads;
   }
   public Map<String, String> getAddExports() {
      return addExports;
   }
   public void setAddExports(Map<String, String> addExports) {
      this.addExports = addExports;
   }

   public void mutateArgs(List<String> args) {
      buildFullOptionStreamLogged().forEach(o -> o.mutateArgs(args));
   }
   public Stream<TaskOption> buildFullOptionStreamLogged() {
      LOGGER.debug("Updating module '{}' with...", helper().moduleName());
      return buildFullOptionStream().peek(option -> LOGGER.debug("  {} {}", option.getFlag(), option.getValue()));
   }

   protected Stream<TaskOption> buildFullOptionStream() {
      return StreamHelper.concat(
         addModulesOption().stream(),
         addReadsOptionStream(),
         addExportsOptionStream()
      );
   }

   private Optional<TaskOption> addModulesOption() {
      if (addModules.isEmpty()) {
         return Optional.empty();
      }
      return Optional.of(new TaskOption("--add-modules", String.join(",", addModules)));
   }

   private Stream<TaskOption> addReadsOptionStream() {
      return buildOptionStream("--add-reads", addReads);
   }

   private Stream<TaskOption> addExportsOptionStream() {
      return buildOptionStream("--add-exports", addExports);
   }

   protected final Stream<TaskOption> buildOptionStream(String flag, Map<String, String> map) {
      if (map.isEmpty()) {
         return Stream.empty();
      }

      return map.entrySet().stream()
         .map(entry -> entry.getKey() + "=" + entry.getValue())
         .map(value -> new TaskOption(flag, value));
   }

   protected final JavaProjectHelper helper() {
      return new JavaProjectHelper(project);
   }
   protected final MergeClassesHelper mergeClassesHelper() {
      return new MergeClassesHelper(project);
   }
}
