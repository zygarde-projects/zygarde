package zygarde.codegen.dsl

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.model.CodegenConfig
import zygarde.codegen.dsl.model.EntityFieldToDtoFieldMapping
import zygarde.codegen.dsl.model.EntityToDtoMapping
import zygarde.codegen.dsl.model.FieldType
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.value.AutoIntIdValueProvider
import zygarde.codegen.value.ValueProvider
import zygarde.data.jpa.entity.AutoIntIdEntity
import java.nio.file.Files

data class MyFooEntity(
  var foo: String,
) : AutoIntIdEntity()

data class MyBarEntity(
  var bar: Int,
  var foo: MyFooEntity,
  var fooList: Collection<MyFooEntity>,
)

class ReverseStringValueProvider : ValueProvider<String, String> {
  override fun getValue(v: String): String {
    return v.reversed()
  }
}

class EntityToDtoMappingCodegenTest {

  @Test
  fun `should able to codegen entity to dto`() {
    val codegen = ModelMappingCodeGenerator(CodegenConfig("zygarde.codegen"))
    codegen.applyEntityToDtoMapping(
      EntityToDtoMapping(
        MyFooEntity::class.java.canonicalName,
        "zygarde.codegen.dto.MyFooDto",
        listOf(
          EntityFieldToDtoFieldMapping(
            comment = "foo foo",
            entityField = FieldType("foo", "java.lang.String", false),
            dtoField = FieldType("foo2", "java.lang.String", false),
          ),
          EntityFieldToDtoFieldMapping(
            comment = "foo reversed",
            entityField = FieldType("foo", "java.lang.String", false),
            dtoField = FieldType("fooReversed", "java.lang.String", false),
            valueProviderClassName = ReverseStringValueProvider::class.java.canonicalName,
          ),
          EntityFieldToDtoFieldMapping(
            comment = "id",
            entityField = null,
            dtoField = FieldType("id", "java.lang.Integer", false),
            valueProviderClassName = AutoIntIdValueProvider::class.java.canonicalName,
            valueProviderParameterType = ValueProviderParameterType.OBJECT
          ),
        )
      )
    )

    codegen.applyEntityToDtoMapping(
      EntityToDtoMapping(
        MyFooEntity::class.java.canonicalName,
        "zygarde.codegen.dto.MyFooDetailDto",
        listOf(
          EntityFieldToDtoFieldMapping(
            comment = "foo foo",
            entityField = FieldType("foo", "java.lang.String", false),
            dtoField = FieldType("foo2", "java.lang.String", false),
          ),
          EntityFieldToDtoFieldMapping(
            comment = "foo reversed",
            entityField = FieldType("foo", "java.lang.String", false),
            dtoField = FieldType("fooReversed", "java.lang.String", false),
            valueProviderClassName = ReverseStringValueProvider::class.java.canonicalName,
          ),
          EntityFieldToDtoFieldMapping(
            comment = "id",
            entityField = null,
            dtoField = FieldType("id", "java.lang.Integer", false),
            valueProviderClassName = AutoIntIdValueProvider::class.java.canonicalName,
            valueProviderParameterType = ValueProviderParameterType.OBJECT
          ),
          EntityFieldToDtoFieldMapping(
            comment = "id",
            entityField = null,
            dtoField = FieldType("relatedFooList", "java.util.Collection", false, listOf("zygarde.codegen.dto.MyFooDto")),
          ),
        )
      )
    )

    codegen.applyEntityToDtoMapping(
      EntityToDtoMapping(
        MyBarEntity::class.java.canonicalName,
        "zygarde.codegen.dto.MyBarDto",
        listOf(
          EntityFieldToDtoFieldMapping(
            comment = "bar bar",
            entityField = FieldType("bar", "java.lang.Integer", false),
            dtoField = FieldType("bar", "java.lang.Integer", false),
          ),
          EntityFieldToDtoFieldMapping(
            comment = "foo",
            entityField = FieldType("foo", MyFooEntity::class.java.canonicalName, false),
            dtoField = FieldType("foo", "zygarde.codegen.dto.MyFooDto", false),
            dtoRef = "MyFooDto",
          ),
          EntityFieldToDtoFieldMapping(
            comment = "foo list",
            entityField = FieldType("fooList", MyFooEntity::class.java.canonicalName, false),
            dtoField = FieldType("fooList", "java.util.Collection", false, listOf("zygarde.codegen.dto.MyFooDto")),
            dtoRef = "MyFooDto",
            dtoRefCollection = true,
          ),
        )
      )
    )

    val codegenDir = Files.createTempDirectory("codegen").toFile()
    codegen.allFileSpecs().forEach { fs ->
      fs.writeTo(codegenDir)
      fs.writeTo(System.out)
    }
    val generatedCodes = FileUtils.listFiles(codegenDir, null, true)
    val result = KotlinCompilation().apply {
      sources = generatedCodes.map { SourceFile.fromPath(it) }
      jvmTarget = JvmTarget.JVM_1_8.description
      inheritClassPath = true
      messageOutputStream = System.out
    }.compile()
    result.exitCode shouldBe KotlinCompilation.ExitCode.OK
  }
}
