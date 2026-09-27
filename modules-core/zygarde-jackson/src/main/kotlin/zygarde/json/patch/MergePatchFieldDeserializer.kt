package zygarde.json.patch

import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.databind.BeanProperty
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.ValueDeserializer

class MergePatchFieldDeserializer(
  private val valueType: JavaType? = null
) : ValueDeserializer<MergePatchField<*>>() {
  override fun createContextual(
    ctxt: DeserializationContext,
    property: BeanProperty?
  ): ValueDeserializer<*> {
    val patchFieldType = property?.type ?: ctxt.contextualType
    val contextualValueType = patchFieldType?.containedType(0)
      ?: ctxt.typeFactory.constructType(Any::class.java)
    return MergePatchFieldDeserializer(contextualValueType)
  }

  override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MergePatchField<*> {
    if (p.currentToken() == JsonToken.VALUE_NULL) {
      return MergePatchField.NullValue
    }
    val targetType = valueType ?: ctxt.typeFactory.constructType(Any::class.java)
    return MergePatchField.Value(ctxt.readValue<Any>(p, targetType))
  }

  override fun getNullValue(ctxt: DeserializationContext): MergePatchField<*> {
    return MergePatchField.NullValue
  }
}
