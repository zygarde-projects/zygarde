package zygarde.json.patch

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer

class MergePatchFieldDeserializer(
  private val valueType: JavaType? = null
) : JsonDeserializer<MergePatchField<*>>(), ContextualDeserializer {
  override fun createContextual(
    ctxt: DeserializationContext,
    property: BeanProperty?
  ): JsonDeserializer<*> {
    val patchFieldType = property?.type ?: ctxt.contextualType
    val contextualValueType = patchFieldType?.containedType(0)
      ?: ctxt.typeFactory.constructType(Any::class.java)
    return MergePatchFieldDeserializer(contextualValueType)
  }

  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MergePatchField<*> {
    if (p.currentToken == JsonToken.VALUE_NULL) {
      return MergePatchField.NullValue
    }
    val targetType = valueType ?: ctxt.typeFactory.constructType(Any::class.java)
    return MergePatchField.Value(ctxt.readValue<Any>(p, targetType))
  }

  override fun getNullValue(ctxt: DeserializationContext): MergePatchField<*> {
    return MergePatchField.NullValue
  }
}
