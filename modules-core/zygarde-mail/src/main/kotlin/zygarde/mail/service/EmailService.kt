package zygarde.mail.service

import org.springframework.mail.javamail.MimeMessageHelper
import zygarde.mail.types.SendEmailRequest
import java.util.function.Consumer

/**
 * @author leo
 */
interface EmailService {

  fun sendEmail(request: SendEmailRequest, extraMimeMessageProcessor: Consumer<MimeMessageHelper>? = null)
}
