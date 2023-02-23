package zygarde.template.thymeleaf

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.StringTemplateResolver
import zygarde.template.thymeleaf.impl.TemplateServiceImpl

@Configuration
@ConditionalOnBean(TemplateEngine::class)
class ZygradeSpringThymeleafConfig {

  @Bean("stringTemplateEngine")
  fun stringTemplateEngine(): TemplateEngine {
    val templateEngine = TemplateEngine()
    val stringTemplateResolver = StringTemplateResolver()
    templateEngine.setTemplateResolver(stringTemplateResolver)
    // SpringStandardDialect has been remove.
    // templateEngine.setDialect(SpringStandardDialect())
    return templateEngine
  }

  @ConditionalOnBean(value = [TemplateEngine::class], name = ["templateEngine"])
  @Bean
  fun templateService(
    @Autowired
    @Qualifier("templateEngine")
    fileTemplateEngine: TemplateEngine,
    @Autowired
    @Qualifier("stringTemplateEngine")
    stringTemplateEngine: TemplateEngine
  ): TemplateService {
    return TemplateServiceImpl(
      fileTemplateEngine,
      stringTemplateEngine
    )
  }
}
