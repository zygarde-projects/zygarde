package zygarde.codegen.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.extensions.asModelMetaField
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.meta.CodegenDtoSimple
import java.net.URLClassLoader
import kotlin.reflect.KProperty1

/**
 * Property references compiled by Kotlin 2.3 for an enum `name` that also overrides an interface `val name`
 * make kotlin-reflect throw from javaField. See zygarde-core src/test/fixtures/kotlin23-intersection-override.
 */
class Kotlin23IntersectionOverrideTest {
  enum class TestDtos : CodegenDtoSimple {
    ColorDto,
  }

  private val fixtureClassLoader = URLClassLoader(
    arrayOf(javaClass.getResource("/fixtures/kotlin23-intersection-override.jar")),
    javaClass.classLoader,
  )

  private val namedEnumName: KProperty1<*, *> = fixtureClassLoader.loadClass("zygarde.fixture.kotlin23.Refs").let { refs ->
    refs.getMethod("getNamedEnumName").invoke(refs.getField("INSTANCE").get(null)) as KProperty1<*, *>
  }

  @Test
  fun `should resolve model meta field without comment`() {
    val field = namedEnumName.asModelMetaField()
    field.modelClass shouldBe ClassName("zygarde.fixture.kotlin23", "NamedEnum")
    field.fieldName shouldBe "name"
    field.fieldClass shouldBe String::class.asClassName()
    field.fieldNullable shouldBe false
    field.comment shouldBe ""
  }

  @Test
  fun `should map from, apply to and sort by intersection override property`() {
    val mappings = mutableListOf<DtoFieldMapping>()
    val spec = ModelMappingSpec(TestDtos.ColorDto, mappings)

    spec.from(namedEnumName)
    spec.applyTo(namedEnumName)
    spec.sortableFields(namedEnumName)

    mappings shouldHaveSize 2
    mappings.forEach {
      it.modelField.fieldName shouldBe "name"
      it.additionalAnnotations.shouldBeEmpty()
    }
    spec.dtoSortableFieldPaths.single().path shouldBe "name"
  }
}
