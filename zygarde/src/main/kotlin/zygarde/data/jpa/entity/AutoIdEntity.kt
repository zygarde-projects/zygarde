package zygarde.data.jpa.entity

import java.io.Serializable
import java.util.Objects
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import org.hibernate.Hibernate

@MappedSuperclass
abstract class AutoIdEntity<T : Serializable> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: T? = null

  override fun equals(other: Any?): Boolean {
    return if (other == null) {
      false
    } else if (other is AutoIdEntity<*>) {
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
