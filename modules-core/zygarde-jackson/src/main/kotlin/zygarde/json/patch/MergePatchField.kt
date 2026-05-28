package zygarde.json.patch

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import java.io.Serializable

@JsonDeserialize(using = MergePatchFieldDeserializer::class)
sealed class MergePatchField<out T> : Serializable {
  object Absent : MergePatchField<Nothing>()

  object NullValue : MergePatchField<Nothing>()

  data class Value<T>(
    val value: T
  ) : MergePatchField<T>()

  fun isPresent(): Boolean = this !is Absent

  fun valueOrNull(): T? = when (this) {
    Absent -> null
    NullValue -> null
    is Value -> value
  }
}
