package zygarde.codegen.dsl.generator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import zygarde.codegen.dsl.ModelMappingSpec
import zygarde.codegen.dsl.model.internal.DtoFieldMapping
import zygarde.codegen.dsl.model.internal.DtoSortableFieldPath
import zygarde.codegen.meta.CodegenDtoSimple
import zygarde.data.api.KeywordPagingAndSortingRequest
import zygarde.data.api.PagingAndSortingRequest

class DtoSortableFieldsCodeGeneratorTest {
  data class Author(val name: String)

  data class Book(
    val id: Long,
    val title: String?,
    val author: Author?,
  )

  enum class Dtos : CodegenDtoSimple {
    SearchBookReq {
      override fun superClass() = PagingAndSortingRequest::class
    },
    EmptySearchBookReq {
      override fun superClass() = KeywordPagingAndSortingRequest::class
    },
    InvalidSearchBookReq,
  }

  @Test
  fun `should generate sortable metadata on paging request dto`() {
    val mappings = mutableListOf<DtoFieldMapping>()
    val sortableFields = mutableListOf<DtoSortableFieldPath>()
    ModelMappingSpec(Dtos.SearchBookReq, mappings, sortableFields).apply {
      fieldNullable(Book::title)
      sortableFields(Book::id, Book::title)
      sortableField(Book::author, Author::name)
    }

    val dto = DtoFieldMappingCodeGenerator(
      dtoFieldMappings = mappings,
      sealedInterfaces = emptyList(),
      dtoSortableFieldPaths = sortableFields,
    ).generateFileSpec().dtoFileSpecs.single().toString()

    dto shouldContain "@OpenApiSortableFields(value = [\"id\", \"title\", \"author.name\"])"
    dto shouldContain "public data class SearchBookReq("
    dto shouldContain ") : PagingAndSortingRequest(),"
    dto shouldContain "Serializable"
  }

  @Test
  fun `should generate regular class for metadata-only paging request`() {
    val sortableFields = mutableListOf<DtoSortableFieldPath>()
    ModelMappingSpec(Dtos.EmptySearchBookReq, mutableListOf(), sortableFields)
      .sortableFields(Book::id)

    val dto = DtoFieldMappingCodeGenerator(
      dtoFieldMappings = emptyList(),
      sealedInterfaces = emptyList(),
      dtoSortableFieldPaths = sortableFields,
    ).generateFileSpec().dtoFileSpecs.single().toString()

    dto shouldContain "public class EmptySearchBookReq() : KeywordPagingAndSortingRequest(), Serializable"
    dto shouldNotContain "data class EmptySearchBookReq"
  }

  @Test
  fun `should reject sortable metadata on non-paging dto`() {
    val sortableFields = mutableListOf<DtoSortableFieldPath>()
    ModelMappingSpec(Dtos.InvalidSearchBookReq, mutableListOf(), sortableFields)
      .sortableFields(Book::id)

    shouldThrow<IllegalArgumentException> {
      DtoFieldMappingCodeGenerator(
        dtoFieldMappings = emptyList(),
        sealedInterfaces = emptyList(),
        dtoSortableFieldPaths = sortableFields,
      ).generateFileSpec()
    }.message shouldBe "DTO 'InvalidSearchBookReq' declares sortable fields but does not extend PagingAndSortingRequest."
  }
}
