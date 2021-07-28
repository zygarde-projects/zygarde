package sample.data.entity

import zygarde.codegen.meta.ZyModelMeta
import zygarde.data.jpa.entity.AutoIntIdEntity
import javax.persistence.Entity

@ZyModelMeta
@Entity
class Image(
  var url: String,
) : AutoIntIdEntity()
