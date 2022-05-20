package zygarde.data.jpa.entity

import org.hibernate.envers.AuditOverride
import javax.persistence.MappedSuperclass

@AuditOverride(forClass = AuditedAutoIdEntity::class)
@MappedSuperclass
abstract class AuditedAutoIntIdEntity : AuditedAutoIdEntity<Int>(), AutoIdGetter<Int>
