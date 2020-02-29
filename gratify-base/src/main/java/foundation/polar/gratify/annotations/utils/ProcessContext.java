package foundation.polar.gratify.annotations.utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public interface ProcessContext {
   ProcessingEnvironment getProcessingEnviroment();
   Elements getElementUtils();
   Types getTypeUtils();
   AnnotationProvider getAnnotationProvider();
}
