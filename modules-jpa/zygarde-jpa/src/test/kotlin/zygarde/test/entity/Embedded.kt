package zygarde.test.entity

import zygarde.data.jpa.entity.AutoLongIdEntity
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SecondaryTable
import jakarta.persistence.SecondaryTables

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
