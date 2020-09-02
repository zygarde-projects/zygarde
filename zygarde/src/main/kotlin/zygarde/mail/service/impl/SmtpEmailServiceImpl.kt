package zygarde.mail.service.impl

import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import zygarde.mail.service.EmailService
import zygarde.mail.types.SendEmailRequest
import java.util.function.Consumer

/**
 * @author leo
 */
class SmtpEmailServiceImpl(val javaMailSender: JavaMailSender) : EmailService {

  override fun sendEmail(request: SendEmailRequest, extraMimeMessageProcessor: Consumer<MimeMessageHelper>?) {
    javaMailSender.send { mimeMessage ->
      val mimeMessageHelper = if (request.attachments.isNotEmpty() || request.inlineResources.isNotEmpty()) {
        MimeMessageHelper(mimeMessage, true, "UTF-8")
      } else {
        MimeMessageHelper(mimeMessage)
      }

      mimeMessageHelper.setTo(request.to.toTypedArray())
      mimeMessageHelper.setSubject(request.subject)
      mimeMessageHelper.setText(request.content, request.html)

      request.replyTo?.let(mimeMessageHelper::setReplyTo)
      request.fromEmail?.let {
        if (request.fromName != null) {
          mimeMessageHelper.setFrom(it, request.fromName)
        } else {
          mimeMessageHelper.setFrom(it)
        }
      }

      request.bcc.takeIf { it.isNotEmpty() }?.let { mimeMessageHelper.setBcc(it.toTypedArray()) }

      request.attachments.forEach {
        mimeMessageHelper.addAttachment(it.name) { it.resource.inputStream }
      }

      request.inlineResources.forEach {
        mimeMessageHelper.addInline(it.name, it.resource)
      }

      extraMimeMessageProcessor?.apply { accept(mimeMessageHelper) }
    }
  }
}
