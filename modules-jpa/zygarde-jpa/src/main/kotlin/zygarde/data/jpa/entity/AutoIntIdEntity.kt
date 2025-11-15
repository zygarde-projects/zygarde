package zygarde.data.jpa.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AutoIntIdEntity : AutoIdEntity<Int>(), AutoIdGetter<Int>
