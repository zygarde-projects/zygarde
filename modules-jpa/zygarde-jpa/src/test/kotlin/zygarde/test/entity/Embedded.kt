package zygarde.test.entity

import zygarde.data.jpa.entity.AutoLongIdEntity
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SecondaryTable
import javax.persistence.SecondaryTables

@Entity
@SecondaryTables(
  SecondaryTable(
    name = "gpu",
    pkJoinColumns = [PrimaryKeyJoinColumn(name = "computer_id")]
  ),
)
class Computer(
  var name: String = "",
  @Embedded
  var gpu: Gpu = Gpu(),
) : AutoLongIdEntity()

@Embeddable
class Gpu(
  @Column(table = "gpu")
  var price: Double = 1000.0,
)
