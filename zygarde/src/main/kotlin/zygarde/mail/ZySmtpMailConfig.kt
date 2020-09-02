package zygarde.mail

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
@ConditionalOnBean(JavaMailSender::class)
class ZySmtpMailConfig {

  @Bean
  fun emailService(@Autowired javaMailSender: JavaMailSender): EmailService {
    return SmtpEmailServiceImpl(javaMailSender)
  }
}
