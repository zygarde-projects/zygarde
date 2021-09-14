package sample.data.entity

import zygarde.codegen.ZyModel
import zygarde.codegen.meta.Comment
import zygarde.data.jpa.entity.AutoIntIdEntity
import javax.persistence.Entity

@ZyModel
@Entity
open class Image(
  @Comment("image url")
  var url: String,

) : AutoIntIdEntity()
