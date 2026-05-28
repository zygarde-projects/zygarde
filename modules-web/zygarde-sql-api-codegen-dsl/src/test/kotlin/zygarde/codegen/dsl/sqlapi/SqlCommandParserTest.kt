package zygarde.codegen.dsl.sqlapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SqlCommandParserTest {
  @Test
  fun `should parse insert command params`() {
    val metadata = SqlCommandParser.parse(
      """
      insert into todo(description, check_times)
      values (:description, :checkTimes)
      """.trimIndent()
    )

    metadata.kind shouldBe SqlCommandKind.INSERT
    metadata.parameterNames.shouldContainExactly("description", "checkTimes")
  }

  @Test
  fun `should parse update command params and ignore comments`() {
    val metadata = SqlCommandParser.parse(
      """
      update todo
      set description = :description
      -- :ignored
      where id = :id
      """.trimIndent()
    )

    metadata.kind shouldBe SqlCommandKind.UPDATE
    metadata.parameterNames.shouldContainExactly("description", "id")
  }

  @Test
  fun `should parse delete command params`() {
    val metadata = SqlCommandParser.parse("delete from todo where id = :id")

    metadata.kind shouldBe SqlCommandKind.DELETE
    metadata.parameterNames.shouldContainExactly("id")
  }

  @Test
  fun `should reject select statements`() {
    val error = shouldThrow<IllegalArgumentException> {
      SqlCommandParser.parse("select id as id from todo")
    }

    error.message shouldBe "SQL API command only supports INSERT, UPDATE, and DELETE statements"
  }
}
