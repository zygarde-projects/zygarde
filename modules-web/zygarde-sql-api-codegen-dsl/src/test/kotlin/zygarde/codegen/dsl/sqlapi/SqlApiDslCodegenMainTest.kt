package zygarde.codegen.dsl.sqlapi

import io.kotest.matchers.file.shouldExist
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SqlApiDslCodegenMainTest {
  @Test
  fun `should write all generated file groups when targets use same directory`(
    @TempDir tempDir: Path
  ) {
    val outputDir = tempDir.resolve("generated").toString()
    val properties = listOf(
      "zygarde.codegen.dsl.sql-api.dto.write-to",
      "zygarde.codegen.dsl.sql-api.api-interface.write-to",
      "zygarde.codegen.dsl.sql-api.feign-interface.write-to",
      "zygarde.codegen.dsl.sql-api.controller.write-to",
      "zygarde.codegen.dsl.sql-api.service-interface.write-to",
      "zygarde.codegen.dsl.sql-api.service-impl.write-to",
    )
    properties.forEach { System.setProperty(it, outputDir) }

    try {
      main()

      tempDir.resolve("generated/zygarde/generated/dto/SearchTodosReq.kt").toFile().shouldExist()
      tempDir.resolve("generated/zygarde/generated/api/TodoReportApi.kt").toFile().shouldExist()
      tempDir.resolve("generated/zygarde/generated/api/TodoReportApiFeign.kt").toFile().shouldExist()
      tempDir.resolve("generated/zygarde/generated/controller/TodoReportApiController.kt").toFile().shouldExist()
      tempDir.resolve("generated/zygarde/generated/service/TodoReportApiService.kt").toFile().shouldExist()
      tempDir.resolve("generated/zygarde/generated/service/impl/TodoReportApiServiceImpl.kt").toFile().shouldExist()
    } finally {
      properties.forEach(System::clearProperty)
    }
  }
}
