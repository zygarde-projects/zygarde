package zygarde.mail.types

import org.springframework.core.io.Resource

/**
 * @author leo
 */
data class SendEmailInlineResourceRequest(
  val name: String,
  val resource: Resource
)
