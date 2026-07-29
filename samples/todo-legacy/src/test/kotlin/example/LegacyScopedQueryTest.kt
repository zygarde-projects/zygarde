package example

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import zygarde.data.jpa.search.ScopeFilter
import zygarde.generated.data.dao.LegacyScopedRecordDao
import zygarde.generated.data.dao.LegacyScopedRecordScope
import zygarde.generated.data.dao.search

@DataJpaTest
@AutoConfigureTestDatabase
class LegacyScopedQueryTest {
  @Autowired
  lateinit var dao: LegacyScopedRecordDao

  @BeforeEach
  fun setUp() {
    dao.deleteAll()
    dao.saveAll(
      listOf(
        LegacyScopedRecord(label = "t1-active", tenantId = "t1", excludedCode = "ACTIVE"),
        LegacyScopedRecord(label = "t1-blocked", tenantId = "t1", excludedCode = "BLOCKED"),
        LegacyScopedRecord(label = "t1-null", tenantId = "t1", excludedCode = null),
        LegacyScopedRecord(label = "t2-active", tenantId = "t2", excludedCode = "ACTIVE"),
      )
    )
  }

  @Test
  fun `KAPT generated scope filters against H2`() {
    dao.search(LegacyScopedRecordScope(tenantId = "t1"))
      .map { it.label } shouldContainExactlyInAnyOrder listOf("t1-active", "t1-blocked", "t1-null")
  }

  @Test
  fun `KAPT generated empty scope collection matches nothing`() {
    dao.search(LegacyScopedRecordScope(tenantId = emptyList())) shouldHaveSize 0
  }

  @Test
  fun `KAPT generated NOT_IN preserves or excludes null according to sentinel`() {
    dao.search(
      LegacyScopedRecordScope(
        tenantId = ScopeFilter.Of(listOf("t1")),
        excludedCode = ScopeFilter.Of(listOf("BLOCKED")),
      )
    ).map { it.label } shouldContainExactlyInAnyOrder listOf("t1-active", "t1-null")

    dao.search(
      LegacyScopedRecordScope(
        tenantId = ScopeFilter.Of(listOf("t1")),
        excludedCode = ScopeFilter.Of(listOf("NULL-CODE")),
      )
    ).map { it.label } shouldContainExactlyInAnyOrder listOf("t1-active", "t1-blocked")
  }
}
