package zygarde.mail.autoconfigure

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import zygarde.mail.service.EmailService
import zygarde.mail.service.impl.SmtpEmailServiceImpl

/**
 * @author leo
 */
@Configuration
class ZygardeSmtpMailConfig {

  @ConditionalOnBean(JavaMailSender::class)
  @Bean
  fun emailService(@Autowired javaMailSender: JavaMailSender): EmailService {
    return SmtpEmailServiceImpl(javaMailSender)
  }
}
