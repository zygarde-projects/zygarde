package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class SequenceIntIdEntity : SequenceIdEntity<Int>(), AutoIdGetter<Int>
