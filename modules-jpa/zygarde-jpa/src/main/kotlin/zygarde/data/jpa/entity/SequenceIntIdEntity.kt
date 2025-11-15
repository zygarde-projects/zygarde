package zygarde.data.jpa.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class SequenceIntIdEntity : SequenceIdEntity<Int>(), AutoIdGetter<Int>
