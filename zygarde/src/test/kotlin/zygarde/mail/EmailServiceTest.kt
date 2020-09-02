package zygarde.mail

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.FileSystemResource
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.data.jpa.ZygardeJpaTestApplication
import zygarde.mail.service.EmailService
import zygarde.mail.types.SendEmailAttachmentRequest
import zygarde.mail.types.SendEmailInlineResourceRequest
import zygarde.mail.types.SendEmailRequest
import java.io.File

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class EmailServiceTest {
  @Autowired lateinit var emailService: EmailService

  @Test
  fun `should able to send html email with fromName`() {
    emailService.sendEmail(
      SendEmailRequest(
        to = listOf("test@puni.tw"),
        subject = "test",
        content = "<h1>oh ya</h1>",
        html = true,
        replyTo = "foo@puni.tw",
        fromEmail = "service@puni.tw",
        fromName = "service",
        bcc = listOf("admin@puni.tw"),
        attachments = listOf(
          SendEmailAttachmentRequest("attachment1", ByteArrayResource("test".toByteArray()))
        ),
        inlineResources = listOf(
          SendEmailInlineResourceRequest(
            "attachment1",
            FileSystemResource(File.createTempFile("test", ".txt"))
          )
        )
      )
    )
  }

  @Test
  fun `should able to send none html email`() {
    emailService.sendEmail(
      SendEmailRequest(
        to = listOf("test@puni.tw"),
        subject = "test",
        content = "oh ya",
        html = false,
        fromEmail = "service@puni.tw"
      )
    )
  }
}
