package foundation.polar.gratify.annotations.checker.processor.base;


import foundation.polar.gratify.annotations.utils.CheckContext;

public interface BaseTypeContext extends CheckContext {
   @Override
   BaseTypeChecker getChecker();

   @Override
   BaseTypeVisitor<?> getVisitor();

   GenericAnnotatedTypeFactory<?, ?, ?, ?> getTypeFactory();
}
