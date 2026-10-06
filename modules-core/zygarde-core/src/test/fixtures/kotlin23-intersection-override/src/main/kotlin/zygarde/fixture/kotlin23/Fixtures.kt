package zygarde.fixture.kotlin23

import kotlin.reflect.KProperty1

interface Named {
  val name: String
}

enum class PlainEnum { A }

/** `name` is an intersection override of Enum.name and Named.name. */
enum class NamedEnum(val label: String) : Named { A("a") }

object Refs {
  val plainEnumName: KProperty1<PlainEnum, String> = PlainEnum::name
  val namedEnumName: KProperty1<NamedEnum, String> = NamedEnum::name
  val namedEnumLabel: KProperty1<NamedEnum, String> = NamedEnum::label
}
