package zygarde.mail.types

import org.springframework.core.io.Resource

/**
 * @author leo
 */
data class SendEmailAttachmentRequest(
  val name: String,
  val resource: Resource
)
