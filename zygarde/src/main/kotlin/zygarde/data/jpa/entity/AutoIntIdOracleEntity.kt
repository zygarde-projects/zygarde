package zygarde.data.jpa.entity

import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.hibernate.id.enhanced.SequenceStyleGenerator
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AutoIntIdOracleEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @GenericGenerator(
    name = "sequenceGenerator",
    strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
    parameters = [
      Parameter(name = SequenceStyleGenerator.CONFIG_PREFER_SEQUENCE_PER_ENTITY, value = "true"),
      Parameter(name = SequenceStyleGenerator.OPT_PARAM, value = "pooled"),
      Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "1"),
      Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1")
    ]
  )
  override val id: Int?
) : AutoIdEntity<Int>(), AutoIdGetter<Int>
