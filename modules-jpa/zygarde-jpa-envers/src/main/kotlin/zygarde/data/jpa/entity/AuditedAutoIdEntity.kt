package zygarde.data.jpa.entity

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.io.Serializable
import java.time.LocalDateTime
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedAutoIdEntity<T : Serializable> : AutoIdEntity<T>() {
  @CreatedDate
  open var createdAt: LocalDateTime = LocalDateTime.now()

  @LastModifiedDate
  open var updatedAt: LocalDateTime? = null

  @CreatedBy
  open var createdBy: String? = null

  @LastModifiedBy
  open var updatedBy: String? = null
}
