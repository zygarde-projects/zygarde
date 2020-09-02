package zygarde.mail

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessagePreparator

/**
 * @author leo
 */
@Configuration
class EmailTestConfig {

  @Primary
  @Bean(name = ["mockJavaMailSender"])
  fun javaMailSender(): JavaMailSender {
    val javaMailSender = Mockito.mock(JavaMailSender::class.java)

    Mockito.`when`(javaMailSender.send(ArgumentMatchers.any(MimeMessagePreparator::class.java)))
      .thenAnswer {
        val mimeMessage = JavaMailSenderImpl().createMimeMessage()
        val arg = it.arguments[0]
        if (arg is MimeMessagePreparator) {
          arg.prepare(mimeMessage)
        }
      }

    return javaMailSender
  }
}
