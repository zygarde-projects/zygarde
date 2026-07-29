package zygarde.codegen.dsl.sqlapi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.update.Update
import org.junit.jupiter.api.Test

class SqlValidationRuleTest {
  @Test
  fun `should expose original SQL and nested CTE AST to validation rules`() {
    var validatedSql: String? = null
    var validatedStatement: Statement? = null
    val sql = """
      with scoped_orders as (
        select id, category
        from resource_order
        where category = :category
      )
      select id as id
      from scoped_orders
    """.trimIndent()
    val api = dslSqlApi(
      rules = listOf(
        SqlValidationRule { originalSql, statement ->
          validatedSql = originalSql
          validatedStatement = statement
          emptyList()
        },
      ),
    ) {
      query("findOrders", "/search") {
        sql(sql)
        param<String>("category")
        column<Long>("id")
      }
    }

    api.toSqlApiToGenerateVo()

    validatedSql shouldBe sql
    (validatedStatement is PlainSelect) shouldBe true
    (validatedStatement as PlainSelect).withItemsList.single().toString() shouldContain "resource_order"
  }

  @Test
  fun `should aggregate rule findings for main and count SQL in stable order`() {
    val error = shouldThrow<IllegalArgumentException> {
      dslSqlApi(
        rules = listOf(
          SqlValidationRule { _, _ -> listOf("first rule") },
          SqlValidationRule { _, _ -> listOf("second rule") },
        ),
      ) {
        query("pageOrders", "/search") {
          sql("select id as id from resource_order limit :pageSize offset :offset")
          returnsPage("select count(*) as totalCount from resource_order")
          column<Long>("id")
        }
      }
    }

    error.message shouldBe """
      SQL validation failed for SQL API 'OrderApi', query 'pageOrders':
      - main SQL: first rule
      - main SQL: second rule
      - count SQL: first rule
      - count SQL: second rule
    """.trimIndent()
  }

  @Test
  fun `should validate insert update and delete command statements`() {
    val statementTypes = mutableListOf<Class<out Statement>>()
    val api = dslSqlApi(
      rules = listOf(
        SqlValidationRule { _, statement ->
          statementTypes.add(statement.javaClass)
          emptyList()
        },
      ),
    ) {
      command("createOrder", "/create") {
        sql("insert into resource_order(category) values (:category)")
        param<String>("category")
      }
      command("updateOrder", "/update") {
        sql("update resource_order set category = :category where id = :id")
        param<String>("category")
        param<Long>("id")
      }
      command("deleteOrder", "/delete") {
        sql("delete from resource_order where id = :id")
        param<Long>("id")
      }
    }

    api.toSqlApiToGenerateVo()

    statementTypes.shouldContainExactly(Insert::class.java, Update::class.java, Delete::class.java)
  }

  @Test
  fun `should preserve validation rule exceptions as implementation failures`() {
    val cause = IllegalStateException("broken rule")
    val error = shouldThrow<IllegalStateException> {
      dslSqlApi(
        rules = listOf(SqlValidationRule { _, _ -> throw cause }),
      ) {
        command("deleteOrder", "/delete") {
          sql("delete from resource_order where id = :id")
          param<Long>("id")
        }
      }
    }

    error.message shouldBe
      "SQL validation rule #1 threw while validating command SQL for SQL API 'OrderApi', command 'deleteOrder'"
    error.cause shouldBe cause
  }

  private fun dslSqlApi(
    rules: List<SqlValidationRule>,
    dsl: DslSqlApi.() -> Unit,
  ): DslSqlApi {
    val config = SqlApiDslCodegenConfig(
      dtoPackage = "generated.dto",
      apiInterfacePackage = "generated.api",
      controllerPackage = "generated.controller",
      serviceInterfacePackage = "generated.service",
      serviceImplPackage = "generated.service.impl",
    )
    return DslSqlApi(config, "OrderApi", "/orders", rules).also(dsl)
  }
}
