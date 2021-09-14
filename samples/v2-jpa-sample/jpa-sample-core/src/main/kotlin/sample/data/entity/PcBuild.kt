package sample.data.entity

import zygarde.codegen.ZyModel
import zygarde.codegen.meta.Comment
import zygarde.data.jpa.entity.AutoIntIdEntity
import java.time.LocalDateTime
import javax.persistence.Entity

@ZyModel
@Entity
class PcBuild(
  var createdAt: LocalDateTime = LocalDateTime.now(),

  @Comment("nnnnn")
  var name: String = "",

  var description: String? = null,

  var imageId: Int? = null,
) : AutoIntIdEntity()
