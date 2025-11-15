package zygarde.data.jpa.entity

import org.hibernate.envers.AuditOverride
import jakarta.persistence.MappedSuperclass

@AuditOverride(forClass = AuditedSequenceIdEntity::class)
@MappedSuperclass
abstract class AuditedSequenceLongIdEntity : AuditedSequenceIdEntity<Long>(), AutoIdGetter<Long>
