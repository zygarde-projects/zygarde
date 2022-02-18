package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedSequenceIntIdEntity : AuditedSequenceIdEntity<Int>(), AutoIdGetter<Int>
