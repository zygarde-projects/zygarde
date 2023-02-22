package zygarde.data.jpa.entity

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import zygarde.data.jpa.audit.AuditInfoContainer
import java.io.Serializable
import java.time.LocalDateTime
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedSequenceIdEntity<T : Serializable> : SequenceIdEntity<T>(), AuditInfoContainer {

  override fun auditContainerKey(): String = "${this::javaClass.name}:$id"

  @CreatedDate
  override var createdAt: LocalDateTime = LocalDateTime.now()

  @LastModifiedDate
  override var updatedAt: LocalDateTime? = null

  @CreatedBy
  override var createdBy: String? = null

  @LastModifiedBy
  override var updatedBy: String? = null
}
