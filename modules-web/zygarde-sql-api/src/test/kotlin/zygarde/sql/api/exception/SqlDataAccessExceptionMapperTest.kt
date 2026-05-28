package zygarde.sql.api.exception

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.jdbc.BadSqlGrammarException
import java.sql.SQLException
import zygarde.core.exception.ApiErrorCode

class SqlDataAccessExceptionMapperTest {
  private val mapper = SqlDataAccessExceptionMapper()

  @Test
  fun `should map data integrity violations to conflict`() {
    val exception = DataIntegrityViolationException("duplicate key")

    mapper.supported(exception) shouldBe true
    mapper.handle(exception).code shouldBe ApiErrorCode.CONFLICT
  }

  @Test
  fun `should map SQL bind input problems to bad request`() {
    val missingParameter = IllegalArgumentException("Missing SQL parameter 'id'")
    val invalidApiUsage = InvalidDataAccessApiUsageException("invalid bind")

    mapper.supported(missingParameter) shouldBe true
    mapper.handle(missingParameter).code shouldBe ApiErrorCode.BAD_REQUEST
    mapper.handle(invalidApiUsage).code shouldBe ApiErrorCode.BAD_REQUEST
  }

  @Test
  fun `should map other data access exceptions to server error`() {
    val exception = BadSqlGrammarException("select", "select * from missing", SQLException("missing table"))

    mapper.supported(exception) shouldBe true
    mapper.handle(exception).code shouldBe ApiErrorCode.SERVER_ERROR
  }
}
