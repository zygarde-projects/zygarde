package zygarde.core.extension.reflect

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URLClassLoader
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/**
 * Uses classes compiled by Kotlin 2.3 (see src/test/fixtures/kotlin23-intersection-override).
 */
class KPropertyExtensionsTest {
  private val fixtureClassLoader = URLClassLoader(
    arrayOf(javaClass.getResource("/fixtures/kotlin23-intersection-override.jar")),
    javaClass.classLoader,
  )

  private fun fixtureRef(name: String): KProperty1<*, *> {
    val refs = fixtureClassLoader.loadClass("zygarde.fixture.kotlin23.Refs")
    val instance = refs.getField("INSTANCE").get(null)
    return refs.getMethod("get${name.replaceFirstChar { it.uppercase() }}").invoke(instance) as KProperty1<*, *>
  }

  @Test
  fun `fixture should reproduce kotlin 2_3 intersection override reflection error`() {
    val ref = fixtureRef("namedEnumName")
    shouldThrow<KotlinReflectionInternalError> { ref.javaField }
    shouldThrow<KotlinReflectionInternalError> { ref.javaGetter }
  }

  @Test
  fun `should resolve intersection override through owner member properties`() {
    val ref = fixtureRef("namedEnumName")
    val resolved = ref.resolvable()
    resolved.name shouldBe "name"
    resolved.returnType.classifier shouldBe String::class
    resolved.returnType.isMarkedNullable shouldBe false
    ref.javaFieldOrNull().shouldBeNull()
    ref.javaGetterOrNull().shouldNotBeNull().returnType shouldBe String::class.java
  }

  @Test
  fun `should behave like javaField and javaGetter for resolvable properties`() {
    fixtureRef("plainEnumName").javaFieldOrNull().shouldBeNull()
    fixtureRef("plainEnumName").javaGetterOrNull()?.name shouldBe "name"

    fixtureRef("plainEnumName").resolvable() shouldBe fixtureRef("plainEnumName")

    val label = fixtureRef("namedEnumLabel")
    label.javaFieldOrNull().shouldNotBeNull().name shouldBe "label"
    label.javaGetterOrNull().shouldNotBeNull().name shouldBe "getLabel"
  }
}
