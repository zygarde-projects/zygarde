package sample.data.entity

import zygarde.codegen.ZyModel
import zygarde.codegen.meta.Comment
import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import javax.persistence.Entity

@ZyModelMeta
@ZyModel
@Entity
open class Image(
  @Comment("image url")
  var url: String,

) : AutoIntIdEntity()
