package zygarde.data.jpa.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class SequenceLongIdEntity : SequenceIdEntity<Long>(), AutoIdGetter<Long>
