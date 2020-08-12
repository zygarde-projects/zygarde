package zygarde.template.thymeleaf

import org.thymeleaf.context.Context

interface TemplateService {

  fun generateFromFileTemplate(templateName: String, templateVariableProcessor: (context: Context) -> Unit): String

  fun generateFromStringTemplate(templateString: String, templateVariableProcessor: (context: Context) -> Unit): String
}
