package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedAutoLongIdEntity : AuditedAutoIdEntity<Long>(), AutoIdGetter<Long>
