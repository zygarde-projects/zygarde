package zygarde.codegen.dsl.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoFieldValidation
import zygarde.codegen.dsl.model.type.ForceNull
import zygarde.codegen.dsl.model.type.ValueProviderParameterType
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.meta.ModelMetaField
import zygarde.codegen.value.ValueProvider
import zygarde.data.provider.DataProvider
import zygarde.data.provider.DataProviderContext

class DtoFieldMappingCodeGeneratorComprehensiveTest {
  data class Product(
    val id: Int?,
    var name: String,
    var rawCode: String?,
    var fileId: String?,
    var tags: List<String>,
    val owner: Owner?,
    val owners: List<Owner>,
  )

  data class Owner(val id: Int?, val name: String)

  data class FileDto(val id: String)

  interface NamedDto {
    val name: String
  }

  enum class TestDtos : CodegenDtoSimple {
    ProductDto,
    ProductSummaryDto,
    OwnerDto,
    UpdateProductReq,
    ProductPatchReq,
    InterfaceDto,
    AnnotatedDto,
    ProductWithFileDto,
  }

  class UppercaseProvider : ValueProvider<String?, String> {
    override fun getValue(v: String?): String = v.orEmpty().uppercase()
  }

  class ProductNameProvider : ValueProvider<Product, String> {
    override fun getValue(v: Product): String = v.name
  }

  class ParseIntProvider : ValueProvider<String, Int> {
    override fun getValue(v: String): Int = v.toInt()
  }

  class FileProvider : DataProvider<String, FileDto> {
    override fun load(keys: Collection<String>, context: DataProviderContext): Map<String, FileDto> {
      return keys.associateWith { FileDto(it) }
    }
  }

  private fun field(
    fieldName: String,
    fieldClass: com.squareup.kotlinpoet.TypeName,
    modelClass: ClassName = Product::class.asClassName(),
    nullable: Boolean = false,
    extra: Boolean = false
  ) = ModelMetaField(
    modelClass = modelClass,
    fieldName = fieldName,
    fieldClass = fieldClass,
    fieldNullable = nullable,
    extra = extra
  )

  @Test
  fun `should generate dto classes with defaults annotations inheritance and validation`() {
    val dto = object : CodegenDtoSimple {
      override val name: String = "InterfaceDto"

      override fun superClass() = NamedDto::class
    }
    val annotatedDto = object : CodegenDtoSimple {
      override val name: String = "AnnotatedDto"

      override fun annotations() = listOf(com.squareup.kotlinpoet.AnnotationSpec.builder(Deprecated::class).build())
    }

    val result = DtoFieldMappingCodeGenerator(
      listOf(
        DtoFieldMapping.DtoFieldNoMapping(field("name", String::class.asTypeName()), dto),
        DtoFieldMapping.DtoFieldNoMapping(
          field("tags", List::class.asClassName().parameterizedBy(String::class.asTypeName()), nullable = false),
          TestDtos.ProductDto
        ),
        DtoFieldMapping.DtoFieldNoMapping(field("owner", Owner::class.asTypeName(), nullable = true), TestDtos.ProductDto).also {
          it.dtoRef = TestDtos.OwnerDto
          it.comment = "Owner DTO"
          it.validations.add(DtoFieldValidation.NotBlank("required"))
        },
        DtoFieldMapping.DtoFieldNoMapping(field("externalOwner", Owner::class.asTypeName()), TestDtos.ProductDto).also {
          it.dtoRefClass = ClassName("com.example.external", "ExternalOwnerDto")
        },
        DtoFieldMapping.DtoFieldNoMapping(field("nullableOwners", Owner::class.asTypeName(), nullable = true), TestDtos.ProductDto).also {
          it.dtoRef = TestDtos.OwnerDto
          it.refCollection = true
        },
        DtoFieldMapping.DtoFieldNoMapping(field("name", String::class.asTypeName()), annotatedDto),
      )
    ).generateFileSpec()

    result.dtoFileSpecs.first { it.name == "InterfaceDto" }.toString().also {
      it shouldContain "data class InterfaceDto"
      it shouldContain "override var name: String"
      it shouldContain "Serializable"
      it shouldContain "NamedDto"
    }
    result.dtoFileSpecs.first { it.name == "ProductDto" }.toString().also {
      it shouldContain "tags: List<String> = emptyList()"
      it shouldContain "owner: OwnerDto? = null"
      it shouldContain """description="Owner DTO""""
      it shouldContain "requiredMode=Schema.RequiredMode.REQUIRED"
      it shouldContain "requiredMode=Schema.RequiredMode.NOT_REQUIRED"
      it shouldContain """@field:NotBlank(message="required")"""
      it shouldContain "externalOwner: ExternalOwnerDto"
      it shouldContain "nullableOwners: Collection<OwnerDto>?"
    }
    result.dtoFileSpecs.first { it.name == "AnnotatedDto" }.toString() shouldContain "@Deprecated"
  }

  @Test
  fun `should generate to dto apply from dto extra values and compound builders`() {
    val nonCompoundMappings = listOf(
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("id", Int::class.asTypeName(), nullable = true), TestDtos.ProductDto).also {
        it.forceNull = ForceNull.NOT_NULL
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("name", String::class.asTypeName()), TestDtos.ProductDto),
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("code", String::class.asTypeName(), nullable = true), TestDtos.ProductDto).also {
        it.valueProvider = UppercaseProvider::class.asClassName()
        it.valueProviderParameterField = "rawCode"
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("displayName", String::class.asTypeName()), TestDtos.ProductDto).also {
        it.valueProvider = ProductNameProvider::class.asClassName()
        it.valueProviderParameterType = ValueProviderParameterType.OBJECT
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("badge", String::class.asTypeName(), extra = true), TestDtos.ProductDto),
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("owner", Owner::class.asTypeName(), nullable = true), TestDtos.ProductDto).also {
        it.dtoRef = TestDtos.OwnerDto
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("owners", Owner::class.asTypeName()), TestDtos.ProductDto).also {
        it.dtoRef = TestDtos.OwnerDto
        it.refCollection = true
      },
      DtoFieldMapping.ModelApplyFromDtoFieldMappingVo(field("name", String::class.asTypeName()), TestDtos.UpdateProductReq),
      DtoFieldMapping.ModelApplyFromDtoFieldMappingVo(field("id", String::class.asTypeName()), TestDtos.UpdateProductReq).also {
        it.valueProvider = ParseIntProvider::class
      },
      DtoFieldMapping.PatchReqFieldMapping(field("name", String::class.asTypeName()), TestDtos.ProductPatchReq),
      DtoFieldMapping.PatchReqFieldMapping(field("rawCode", String::class.asTypeName(), nullable = true), TestDtos.ProductPatchReq),
      DtoFieldMapping.PatchReqFieldMapping(
        field("tags", List::class.asClassName().parameterizedBy(String::class.asTypeName())),
        TestDtos.ProductPatchReq
      ),
    )
    val compoundMappings = listOf(
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("id", Int::class.asTypeName(), nullable = true), TestDtos.ProductSummaryDto).also {
        it.compound = true
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("name", String::class.asTypeName()), TestDtos.ProductSummaryDto).also {
        it.compound = true
      },
      DtoFieldMapping.ModelToDtoFieldMappingVo(field("badge", String::class.asTypeName(), extra = true), TestDtos.ProductSummaryDto).also {
        it.compound = true
      },
    )

    val result = DtoFieldMappingCodeGenerator(nonCompoundMappings + compoundMappings).generateFileSpec()

    result.modelMappingFileSpecs.first { it.name == "ProductToProductDtoExtraValues" }.toString().also {
      it shouldContain "data class ProductToProductDtoExtraValues"
      it shouldContain "val badge: String"
    }
    result.modelMappingFileSpecs.first { it.name == "ProductToDtoExtensions" }.toString().also {
      it shouldContain "toProductDto(extraValues: ProductToProductDtoExtraValues)"
      it shouldContain "ProductDto = ProductDto"
      it shouldContain "id = this.id"
      it shouldContain "UppercaseProvider().getValue(this.rawCode)"
      it shouldContain "ProductNameProvider().getValue(this)"
      it shouldContain "badge = extraValues.badge"
      it shouldContain "owner = this.owner?.toOwnerDto()"
      it shouldContain "owners = this.owners.map{it.toOwnerDto()}"
    }
    result.modelMappingFileSpecs.first { it.name == "ProductApplyValueExtensions" }.toString().also {
      it shouldContain "applyFrom(req: UpdateProductReq)"
      it shouldContain "Product {"
      it shouldContain "this.name = req.name"
      it shouldContain "ParseIntProvider().getValue(req.id)"
      it shouldContain "return this"
    }
    result.dtoFileSpecs.first { it.name == "ProductPatchReq" }.toString().also {
      it shouldContain "@JsonIgnoreProperties(ignoreUnknown = false)"
      it shouldContain "@JsonAnySetter"
      it shouldContain "@Suppress(\"UNUSED_PARAMETER\")"
      it shouldContain "fun rejectUnknownPatchField("
      it shouldContain "Unknown JSON merge patch field '"
      it shouldContain "name: MergePatchField<String> = MergePatchField.Absent"
      it shouldContain "rawCode: MergePatchField<String> = MergePatchField.Absent"
      it shouldContain "tags: MergePatchField<List<String>> = MergePatchField.Absent"
      it shouldContain "implementation = String::class"
      it shouldContain "@ArraySchema"
      it shouldContain "schema = Schema(implementation = String::class)"
      it shouldContain "nullable = false"
      it shouldContain "nullable = true"
      it shouldContain "requiredMode=Schema.RequiredMode.NOT_REQUIRED"
    }
    result.modelMappingFileSpecs.first { it.name == "ProductPatchExtensions" }.toString().also {
      it shouldContain "applyPatch(req: ProductPatchReq)"
      it shouldContain "MergePatchField.Absent -> Unit"
      it shouldContain "MergePatchField.NullValue -> require(false)"
      it shouldContain "MergePatchField.NullValue -> this.rawCode = null"
      it shouldContain "is MergePatchField.Value -> this.name = patchField.value"
      it shouldContain "is MergePatchField.Value -> this.tags = patchField.value"
    }
    result.modelMappingFileSpecs.first { it.name == "ProductSummaryDtoBuilder" }.toString().also {
      it shouldContain "object ProductSummaryDtoBuilder"
      it shouldContain "fun build(product:"
      it shouldContain "badge: String)"
      it shouldContain "ProductSummaryDto = ProductSummaryDto"
      it shouldContain "id = product.id"
      it shouldContain "name = product.name"
      it shouldContain "badge = badge"
    }
  }

  @Test
  fun `should reject DTO that mixes patchReq with other mappings`() {
    val ex = shouldThrow<IllegalArgumentException> {
      DtoFieldMappingCodeGenerator(
        listOf(
          DtoFieldMapping.PatchReqFieldMapping(field("name", String::class.asTypeName()), TestDtos.ProductPatchReq),
          DtoFieldMapping.ModelToDtoFieldMappingVo(field("id", Int::class.asTypeName()), TestDtos.ProductPatchReq),
        )
      ).generateFileSpec()
    }

    ex.message shouldContain "Patch request DTO 'ProductPatchReq' cannot mix patchReq mappings with from/field/applyTo mappings."
  }

  @Test
  fun `should not generate extra value class for compound-only extra mappings`() {
    val result = DtoFieldMappingCodeGenerator(
      listOf(
        DtoFieldMapping.ModelToDtoFieldMappingVo(field("badge", String::class.asTypeName(), extra = true), TestDtos.ProductSummaryDto).also {
          it.compound = true
        }
      )
    ).generateFileSpec()

    result.modelMappingFileSpecs.filter { it.name.contains("ExtraValues") } shouldHaveSize 0
    result.modelMappingFileSpecs.first { it.name == "ProductSummaryDtoBuilder" }.toString() shouldContain "badge: String"
  }

  @Test
  fun `should generate provider dto field and spring assembler with batched keys`() {
    val result = DtoFieldMappingCodeGenerator(
      listOf(
        DtoFieldMapping.ModelToDtoFieldMappingVo(field("id", Int::class.asTypeName(), nullable = true), TestDtos.ProductWithFileDto).also {
          it.forceNull = ForceNull.NOT_NULL
        },
        DtoFieldMapping.ModelToDtoFieldMappingVo(field("name", String::class.asTypeName()), TestDtos.ProductWithFileDto),
        DtoFieldMapping.ModelToDtoFieldMappingVo(
          field("file", FileDto::class.asTypeName(), extra = true),
          TestDtos.ProductWithFileDto,
        ).also {
          it.dataProvider = FileProvider::class.asClassName()
          it.dataProviderKeyType = String::class.asTypeName()
          it.dataProviderValueType = FileDto::class.asTypeName()
          it.dataProviderKeyField = field("fileId", String::class.asTypeName(), nullable = true)
          it.nullable()
        },
      )
    ).generateFileSpec()

    result.dtoFileSpecs.first { it.name == "ProductWithFileDto" }.toString().also {
      it shouldContain "public var `file`: DtoFieldMappingCodeGeneratorComprehensiveTest.FileDto? = null"
      it shouldNotContain "fileId"
    }
    result.modelMappingFileSpecs.first { it.name == "ProductWithFileDtoAssembler" }.toString().also {
      it shouldContain "@Component"
      it shouldContain "private val fileProvider: DtoFieldMappingCodeGeneratorComprehensiveTest.FileProvider"
      it shouldContain "val fileKeys = modelList.mapNotNull { it.fileId }.distinct()"
      it shouldContain "val fileValues = fileProvider.load(fileKeys)"
      it shouldContain "id = model.id"
      it shouldContain "name = model.name"
      it shouldContain "file = model.fileId?.let { fileValues[it] }"
    }
  }
}
