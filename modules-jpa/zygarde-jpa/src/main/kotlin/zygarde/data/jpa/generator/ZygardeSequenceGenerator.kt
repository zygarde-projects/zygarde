package zygarde.data.jpa.generator

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.enhanced.SequenceStyleGenerator
import zygarde.data.jpa.entity.SequenceIdEntity
import java.io.Serializable

class ZygardeSequenceGenerator : SequenceStyleGenerator() {

  override fun generate(session: SharedSessionContractImplementor, obj: Any): Serializable {
    if (obj is SequenceIdEntity<*>) {
      val id = obj.id
      if (id != null) {
        return id
      }
    }
    return super.generate(session, obj)
  }
}
