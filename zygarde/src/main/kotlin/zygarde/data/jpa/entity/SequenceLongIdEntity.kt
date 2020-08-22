package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class SequenceLongIdEntity : SequenceIdEntity<Long>(), AutoIdGetter<Long>
