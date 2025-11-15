package zygarde.test

import io.mockk.every
import io.mockk.mockkClass
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessagePreparator
import zygarde.mail.service.EmailService
import zygarde.mail.service.impl.SmtpEmailServiceImpl

/**
 * @author leo
 */
@SpringBootApplication
class EmailTestApp {

  @Bean
  fun javaMailSender(): JavaMailSender {
    val javaMailSender = mockkClass(JavaMailSender::class)
    every { javaMailSender.send(any<MimeMessagePreparator>()) } answers {
      val mimeMessage = JavaMailSenderImpl().createMimeMessage()
      val arg = firstArg<MimeMessagePreparator>()
      arg.prepare(mimeMessage)
    }
    return javaMailSender
  }

  @Bean
  fun emailService(javaMailSender: JavaMailSender): EmailService {
    return SmtpEmailServiceImpl(javaMailSender)
  }
}
