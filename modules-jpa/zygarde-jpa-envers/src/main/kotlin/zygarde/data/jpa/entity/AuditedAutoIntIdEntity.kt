package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedAutoIntIdEntity : AuditedAutoIdEntity<Int>(), AutoIdGetter<Int>
