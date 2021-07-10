package sample.data.entity

import zygarde.codegen.meta.ZyModelMeta
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@ZyModelMeta
@Entity
class PcBuild(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,

  var createdAt: LocalDateTime = LocalDateTime.now(),

  var name: String = "",

  var description: String? = null,
)
