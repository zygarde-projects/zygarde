package zygarde.data.jpa.entity

import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import zygarde.data.jpa.audit.AuditInfoContainer
import java.time.LocalDateTime
import javax.persistence.MappedSuperclass

@Audited
@MappedSuperclass
abstract class AuditedEntity : AuditInfoContainer {

  @CreatedDate
  override var createdAt: LocalDateTime = LocalDateTime.now()

  @LastModifiedDate
  override var updatedAt: LocalDateTime? = null

  @CreatedBy
  override var createdBy: String? = null

  @LastModifiedBy
  override var updatedBy: String? = null
}
