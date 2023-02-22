package zygarde.data.jpa.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Parameter
import org.hibernate.id.enhanced.SequenceStyleGenerator
import java.io.Serializable
import java.util.*
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class SequenceIdEntity<T : Serializable> : Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
  @GenericGenerator(
    name = "sequenceGenerator",
    strategy = "zygarde.data.jpa.generator.ZygardeSequenceGenerator",
    parameters = [
      Parameter(name = SequenceStyleGenerator.OPT_PARAM, value = "pooled"),
      Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "1"),
      Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "1")
    ]
  )
  open val id: T? = null

  override fun equals(other: Any?): Boolean {
    return if (other == null) {
      false
    } else if (other is SequenceIdEntity<*>) {
      if (other.id == null || this.id == null) {
        return false
      }
      if (Hibernate.getClass(other) == Hibernate.getClass(this)) {
        Objects.equals(id, other.id)
      } else {
        false
      }
    } else {
      false
    }
  }

  override fun hashCode() = Objects.hashCode(id)
}
