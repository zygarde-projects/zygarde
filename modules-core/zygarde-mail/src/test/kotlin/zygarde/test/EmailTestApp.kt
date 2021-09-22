package zygarde.test

import io.mockk.every
import io.mockk.mockkClass
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessagePreparator

/**
 * @author leo
 */
@SpringBootApplication
class EmailTestApp {

  @Primary
  @Bean(name = ["mockJavaMailSender"])
  fun javaMailSender(): JavaMailSender {
    val javaMailSender = mockkClass(JavaMailSender::class)
    every { javaMailSender.send(any<MimeMessagePreparator>()) } answers {
      val mimeMessage = JavaMailSenderImpl().createMimeMessage()
      val arg = firstArg<MimeMessagePreparator>()
      arg.prepare(mimeMessage)
    }
    return javaMailSender
  }
}
