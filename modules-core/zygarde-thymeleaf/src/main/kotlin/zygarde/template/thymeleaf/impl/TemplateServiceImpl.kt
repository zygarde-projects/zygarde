package zygarde.template.thymeleaf.impl

import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import zygarde.template.thymeleaf.TemplateService

class TemplateServiceImpl(
  private val fileTemplateEngine: TemplateEngine,
  private val stringTemplateEngine: TemplateEngine
) : TemplateService {
  override fun generateFromFileTemplate(
    templateName: String,
    templateVariableProcessor: (context: Context) -> Unit
  ): String {
    val context = Context()
    templateVariableProcessor.invoke(context)
    return fileTemplateEngine.process(templateName, context)
  }

  override fun generateFromStringTemplate(
    templateString: String,
    templateVariableProcessor: (context: Context) -> Unit
  ): String {
    val context = Context()
    templateVariableProcessor.invoke(context)
    return stringTemplateEngine.process(templateString, context)
  }
}
