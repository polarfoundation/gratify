package foundation.polar.gratify.annotations.utils;

import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public interface ProcessContext {
   ProcessingEnvironment getProcessingEnvironment();
   Elements getElementUtils();
   Types getTypeUtils();
   Trees getTreeUtils();
   AnnotationProvider getAnnotationProvider();
   OptionConfiguration getOptionConfiguration();
}
