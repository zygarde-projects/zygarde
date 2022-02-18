package zygarde.data.jpa.entity

import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import javax.persistence.MappedSuperclass

@Audited
@MappedSuperclass
abstract class AuditedEntity {
  @CreatedDate
  open var createdAt: LocalDateTime = LocalDateTime.now()

  @LastModifiedDate
  open var updatedAt: LocalDateTime? = null

  @CreatedBy
  open var createdBy: String? = null

  @LastModifiedBy
  open var updatedBy: String? = null
}
