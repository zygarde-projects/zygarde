package zygarde.data.jpa.entity

import org.hibernate.envers.AuditOverride
import javax.persistence.MappedSuperclass

@AuditOverride(forClass = AuditedAutoIdEntity::class)
@MappedSuperclass
abstract class AuditedAutoLongIdEntity : AuditedAutoIdEntity<Long>(), AutoIdGetter<Long>
