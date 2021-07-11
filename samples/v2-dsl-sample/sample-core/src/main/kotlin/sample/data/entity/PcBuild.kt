package sample.data.entity

import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import java.time.LocalDateTime
import javax.persistence.Entity

@ZyModelMeta
@Entity
class PcBuild(
  var createdAt: LocalDateTime = LocalDateTime.now(),

  var name: String = "",

  var description: String? = null,
) : AutoIntIdEntity()
