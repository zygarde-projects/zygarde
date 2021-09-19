package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AutoLongIdEntity : AutoIdEntity<Long>(), AutoIdGetter<Long>
