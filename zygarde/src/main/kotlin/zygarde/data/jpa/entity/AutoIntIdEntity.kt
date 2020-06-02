package zygarde.data.jpa.entity

import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AutoIntIdEntity : AutoIdEntity<Int>()
