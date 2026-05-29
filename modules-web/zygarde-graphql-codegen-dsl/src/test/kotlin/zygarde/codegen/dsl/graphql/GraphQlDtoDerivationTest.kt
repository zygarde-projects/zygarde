package zygarde.codegen.dsl.graphql

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.ModelMappingCodegenSpec
import zygarde.codegen.dsl.meta.DtoMetaResolver
import zygarde.codegen.dsl.meta.ModelMappingMetadata
import zygarde.codegen.generator.GraphQlApiGenerator
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.codegen.model.graphql.GraphQlApiToGenerateVo
import zygarde.codegen.model.graphql.GraphQlTypeDefinitionKind
import zygarde.data.provider.DataProvider
import zygarde.data.provider.DataProviderContext
import zygarde.core.annotation.Comment
import java.time.LocalDate

class GraphQlDtoDerivationTest {
  data class BookDto(val id: Int, val title: String)

  data class CreateBookReq(val title: String)

  data class ProductDto(val id: Int, val name: String, val file: FileDto?)

  data class ProductWithFileIdDto(val id: Int, val fileId: String?, val file: FileDto?)

  data class FileDto(val id: String)

  class Author {
    var id: Int = 0
    var name: String = ""
  }

  class Book {
    var id: Int = 0

    @Comment("the book title")
    var title: String = ""
    var pageCount: Int? = null
    var status: BookStatus = BookStatus.DRAFT
    var publishedAt: LocalDate? = null
  }

  class Product {
    var id: Int = 0
    var name: String = ""
    var fileId: String? = null
  }

  class FileProvider : DataProvider<String, FileDto> {
    override fun load(keys: Collection<String>, context: DataProviderContext): Map<String, FileDto> =
      keys.associateWith { FileDto(it) }
  }

  enum class BookStatus {
    DRAFT,
    PUBLISHED,
  }

  enum class TestDto : CodegenDtoSimple {
    AuthorDto,
    BookDto,
    CreateBookReq,
  }

  enum class ProviderDto : CodegenDtoSimple {
    ProductDto,
    ProductWithFileIdDto,
  }

  class TestModelSpec : ModelMappingCodegenSpec({
    TestDto.AuthorDto {
      fromAutoIntId(Author::id)
      from(Author::name)
    }
    TestDto.BookDto {
      fromAutoIntId(Book::id)
      from(Book::title, Book::pageCount, Book::status, Book::publishedAt)
      fromRef("author", TestDto.AuthorDto)
    }
    TestDto.CreateBookReq {
      applyTo(Book::title)
    }
  })

  class ProviderModelSpec : ModelMappingCodegenSpec({
    ProviderDto.ProductDto {
      fromAutoIntId(Product::id)
      from(Product::name)
      provide<FileProvider, String, FileDto>("file") {
        key(Product::fileId)
        nullable()
      }
    }
    ProviderDto.ProductWithFileIdDto {
      fromAutoIntId(Product::id)
      from(Product::fileId)
      provide<FileProvider, String, FileDto>("file") {
        key(Product::fileId)
        nullable()
      }
    }
  })

  private fun metadata(): ModelMappingMetadata = DtoMetaResolver.resolve(TestModelSpec().dtoFieldMappings)

  private fun providerMetadata(): ModelMappingMetadata = DtoMetaResolver.resolve(ProviderModelSpec().dtoFieldMappings)

  private fun deriveSchema(
    metadata: ModelMappingMetadata = metadata(),
    build: DslGraphQlSchema.() -> Unit,
  ): GraphQlApiToGenerateVo {
    val dsl = object : GraphQlDslCodegen() {
      override fun codegen() {
        schema("TestGraphQl", build)
      }
    }
    dsl.modelMappingMetadata = metadata
    dsl.codegen()
    return dsl.apisToGenerate.single()
  }

  @Test
  fun `should derive a GraphQL type from a model-mapping DTO including ids nullability and descriptions`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      typeFrom(TestDto.BookDto, name = "Book")
    }

    val book = api.typeDefinitions.single { it.name == "Book" }
    book.kind shouldBe GraphQlTypeDefinitionKind.TYPE
    book.fields.map { it.name } shouldBe listOf("id", "title", "pageCount", "status", "publishedAt", "author")
    book.fields.single { it.name == "id" }.apply {
      graphQlType shouldBe "ID"
      nullable shouldBe false
    }
    book.fields.single { it.name == "title" }.apply {
      graphQlType shouldBe "String"
      nullable shouldBe false
      description shouldBe "the book title"
    }
    book.fields.single { it.name == "pageCount" }.apply {
      graphQlType shouldBe "Int"
      nullable shouldBe true
    }
    book.fields.single { it.name == "publishedAt" }.graphQlType shouldBe "Date"
  }

  @Test
  fun `should transitively derive referenced DTO types and enum types`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      typeFrom(TestDto.BookDto, name = "Book")
    }

    api.typeDefinitions.map { it.name }.toSet() shouldBe setOf("Book", "AuthorDto", "BookStatus", "Date")

    val book = api.typeDefinitions.single { it.name == "Book" }
    book.fields.single { it.name == "author" }.graphQlType shouldBe "AuthorDto"
    book.fields.single { it.name == "status" }.graphQlType shouldBe "BookStatus"

    val author = api.typeDefinitions.single { it.name == "AuthorDto" }
    author.kind shouldBe GraphQlTypeDefinitionKind.TYPE
    author.fields.map { it.name } shouldBe listOf("id", "name")

    val status = api.typeDefinitions.single { it.name == "BookStatus" }
    status.kind shouldBe GraphQlTypeDefinitionKind.ENUM
    status.enumValues.map { it.name } shouldBe listOf("DRAFT", "PUBLISHED")
  }

  @Test
  fun `should derive a GraphQL input from a model-mapping request DTO`() {
    val api = deriveSchema {
      inputFrom(TestDto.CreateBookReq, name = "CreateBookInput")
    }

    val input = api.typeDefinitions.single()
    input.kind shouldBe GraphQlTypeDefinitionKind.INPUT
    input.name shouldBe "CreateBookInput"
    input.fields.single().apply {
      name shouldBe "title"
      graphQlType shouldBe "String"
    }
  }

  @Test
  fun `should exclude fields from a derived type`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      typeFrom(TestDto.BookDto, name = "Book", exclude = setOf("pageCount", "publishedAt"))
    }

    api.typeDefinitions.single { it.name == "Book" }
      .fields.map { it.name } shouldBe listOf("id", "title", "status", "author")
  }

  @Test
  fun `should reuse a manually declared enum instead of re-deriving it`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      enumType<BookStatus>()
      typeFrom(TestDto.BookDto, name = "Book")
    }

    api.typeDefinitions.count { it.name == "BookStatus" } shouldBe 1
  }

  @Test
  fun `should fail when a DTO field type cannot be mapped to a GraphQL type`() {
    val ex = shouldThrow<IllegalArgumentException> {
      deriveSchema {
        typeFrom(TestDto.BookDto, name = "Book")
      }
    }
    ex.message shouldContain "publishedAt"
  }

  @Test
  fun `should fail when deriving a DTO absent from model-mapping metadata`() {
    val ex = shouldThrow<IllegalArgumentException> {
      deriveSchema(metadata = ModelMappingMetadata.EMPTY) {
        typeFrom(TestDto.BookDto, name = "Book")
      }
    }
    ex.message shouldContain "model-mapping metadata"
  }

  @Test
  fun `should reject a derived type whose name collides with an existing declaration`() {
    shouldThrow<IllegalArgumentException> {
      deriveSchema {
        type("Book") {
          field<String>("title")
        }
        typeFrom(TestDto.BookDto, name = "Book")
      }
    }
  }

  @Test
  fun `should bind derived DTO GraphQL names to operation arguments and responses`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      typeFrom<BookDto>(TestDto.BookDto, name = "Book")
      inputFrom<CreateBookReq>(TestDto.CreateBookReq, name = "CreateBookInput")
      mutation("createBook") {
        argument<CreateBookReq>("input")
        returns<BookDto>()
      }
    }

    val mutation = api.functions.single()
    mutation.arguments.single().graphQlType shouldBe "CreateBookInput"
    mutation.responseGraphQlType shouldBe "Book"
  }

  @Test
  fun `should render derived types into the generated GraphQL schema`() {
    val api = deriveSchema {
      mapScalar<LocalDate>("Date")
      scalar("Date")
      typeFrom(TestDto.BookDto, name = "Book")
      inputFrom(TestDto.CreateBookReq, name = "CreateBookInput")
    }

    val schema = GraphQlApiGenerator(listOf(api)).generateApis().schemas.single().content

    schema shouldContain "type Book {"
    schema shouldContain "id: ID!"
    schema shouldContain "publishedAt: Date"
    schema shouldContain "author: AuthorDto!"
    schema shouldContain "type AuthorDto {"
    schema shouldContain "enum BookStatus {"
    schema shouldContain "input CreateBookInput {"
  }

  @Test
  fun `should expose model mapping provider metadata`() {
    val provider = providerMetadata().providerFieldsOf(ProviderDto.ProductDto).orEmpty().single()

    provider.fieldName shouldBe "file"
    provider.providerType.simpleName shouldBe "FileProvider"
    provider.keySourceFieldName shouldBe "fileId"
    provider.nullable shouldBe true
  }

  @Test
  fun `should derive lazy GraphQL provider source and batch resolver metadata`() {
    val api = deriveSchema(metadata = providerMetadata()) {
      type("File") {
        field<String>("id")
      }
      typeFrom<ProductDto>(ProviderDto.ProductDto, name = "Product") {
        lazyProviders {
          provider("file") {
            graphQlType("File")
            nullable()
          }
        }
      }
      query("products") {
        returnsCollection<ProductDto>()
      }
    }

    val product = api.typeDefinitions.single { it.name == "Product" }
    product.fields.map { it.name } shouldBe listOf("id", "name", "file")
    product.fields.single { it.name == "file" }.apply {
      graphQlType shouldBe "File"
      nullable shouldBe true
    }

    val lazyType = api.lazyTypes.single()
    lazyType.sourceType.simpleName shouldBe "ProductGraphQlSource"
    lazyType.sourceFields.map { it.name } shouldBe listOf("id", "name", "fileId")
    lazyType.providers.single().apply {
      fieldName shouldBe "file"
      keySourceFieldName shouldBe "fileId"
      providerType.simpleName shouldBe "FileProvider"
    }

    val generated = GraphQlApiGenerator(listOf(api)).generateApis()
    generated.schemas.single().content shouldContain "file: File"
    generated.schemas.single().content shouldContain "products: [Product!]!"
    generated.schemas.single().content shouldContain "type Product {"
    generated.schemas.single().content shouldContain "name: String!"
    generated.schemas.single().content shouldContain "file: File"
    generated.schemas.single().content shouldContain "type File {"
    generated.schemas.single().content shouldContain "id: String!"
    generated.schemas.single().content.contains("fileId") shouldBe false
    generated.serviceInterfaces.single().toString() shouldContain "Collection<ProductGraphQlSource>"
    generated.supportTypes.map { it.name }.toSet() shouldBe setOf("ProductGraphQlSource", "ProductGraphQlSourceAssembler")
    generated.controllers.single().toString().also {
      it shouldContain "@BatchMapping"
      it shouldContain "fun productFile("
      it shouldContain "typeName = \"Product\""
      it shouldContain "field = \"file\""
      it shouldContain "fileProvider.load(keys, DataProviderContext.EMPTY)"
    }
  }

  @Test
  fun `should not duplicate source fields when provider key is also public`() {
    val api = deriveSchema(metadata = providerMetadata()) {
      typeFrom<ProductWithFileIdDto>(ProviderDto.ProductWithFileIdDto, name = "ProductWithFileId") {
        lazyProviders()
      }
    }

    api.lazyTypes.single().sourceFields.map { it.name } shouldBe listOf("id", "fileId")
  }

  @Test
  fun `should reject lazy provider override for unknown DTO provider field`() {
    val ex = shouldThrow<IllegalArgumentException> {
      deriveSchema(metadata = providerMetadata()) {
        typeFrom<ProductDto>(ProviderDto.ProductDto, name = "Product") {
          lazyProviders {
            provider("owner") {
              graphQlType("Owner")
            }
          }
        }
      }
    }

    ex.message shouldContain "no provider field 'owner'"
  }
}
