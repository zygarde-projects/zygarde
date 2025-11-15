package zygarde.data.jpa.entity

import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AutoLongIdEntity : AutoIdEntity<Long>(), AutoIdGetter<Long>
