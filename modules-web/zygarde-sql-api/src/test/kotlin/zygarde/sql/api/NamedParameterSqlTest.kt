package zygarde.sql.api

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class NamedParameterSqlTest {
  @Test
  fun `should parse named parameters outside single quoted strings`() {
    val parsed = NamedParameterSql.parse(
      "select ':ignored', ''':still_ignored''' from todo where id = :id and name like :name"
    )

    parsed.sql shouldBe "select ':ignored', ''':still_ignored''' from todo where id = ? and name like ?"
    parsed.parameterNames.shouldContainExactly("id", "name")
  }

  @Test
  fun `should not parse postgres cast syntax as named parameter`() {
    val parsed = NamedParameterSql.parse(
      "select created_at::date as created_date from todo where owner_id = :ownerId"
    )

    parsed.sql shouldBe "select created_at::date as created_date from todo where owner_id = ?"
    parsed.parameterNames.shouldContainExactly("ownerId")
  }

  @Test
  fun `should not parse parameters in SQL comments or quoted identifiers`() {
    val parsed = NamedParameterSql.parse(
      """
      select ":ignored_column", `:ignored_backtick`, [:ignored_bracket]
      from todo
      -- :ignored_line
      where id = :id
        /* :ignored_block */
        and name = :name
      """.trimIndent()
    )

    parsed.parameterNames.shouldContainExactly("id", "name")
  }

  @Test
  fun `should expand iterable parameters and preserve bind order`() {
    val bound = NamedParameterSql.parse(
      "select * from todo where owner_id = :ownerId and id in (:ids) or fallback_id in (:ids)"
    ).bind(
      mapOf("ownerId" to 7, "ids" to listOf(1, 2, 3))
    )

    bound.sql shouldBe "select * from todo where owner_id = ? and id in (?, ?, ?) or fallback_id in (?, ?, ?)"
    bound.parameterValues.shouldContainExactly(7, 1, 2, 3, 1, 2, 3)
  }

  @Test
  fun `should reject empty iterable parameters`() {
    val error = shouldThrow<IllegalArgumentException> {
      NamedParameterSql.parse("select * from todo where id in (:ids)").bind(mapOf("ids" to emptyList<Int>()))
    }

    error.message shouldBe "SQL parameter 'ids' must not be empty"
  }
}
