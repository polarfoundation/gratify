package foundation.polar.gratify.annotations.utils;

public interface CheckContext extends ProcessContext {
   SourceChecker getChecker();
   SourceVisitor<?, ?> getVisitor();
}
