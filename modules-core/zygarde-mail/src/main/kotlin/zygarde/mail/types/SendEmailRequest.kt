package zygarde.mail.types

/**
 * @author leo
 */
data class SendEmailRequest(
  val to: List<String>,
  val replyTo: String? = null,
  val subject: String,
  val content: String,
  val html: Boolean,
  val fromName: String? = null,
  val fromEmail: String? = null,
  val bcc: List<String> = emptyList(),
  val attachments: List<SendEmailAttachmentRequest> = emptyList(),
  val inlineResources: List<SendEmailInlineResourceRequest> = emptyList()
)
