package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedSequenceLongIdEntity : AuditedSequenceIdEntity<Long>(), AutoIdGetter<Long>
