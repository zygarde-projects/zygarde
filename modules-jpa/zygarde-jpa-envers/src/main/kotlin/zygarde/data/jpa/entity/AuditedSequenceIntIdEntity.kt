package zygarde.data.jpa.entity

import org.hibernate.envers.AuditOverride
import javax.persistence.MappedSuperclass

@AuditOverride(forClass = AuditedSequenceIdEntity::class)
@MappedSuperclass
abstract class AuditedSequenceIntIdEntity : AuditedSequenceIdEntity<Int>(), AutoIdGetter<Int>
