package sample

import javax.persistence.Entity
import javax.persistence.Id

@Entity
class SampleEntity(
  @Id
  var id: String,

  var name: String,
)
