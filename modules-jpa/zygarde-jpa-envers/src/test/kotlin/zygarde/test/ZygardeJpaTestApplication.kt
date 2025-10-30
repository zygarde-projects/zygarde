package zygarde.test

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import zygarde.data.jpa.dao.ZygardeJpaRepository

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(
  basePackages = ["zygarde.test.dao"],
  repositoryBaseClass = ZygardeJpaRepository::class
)
class ZygardeJpaTestApplication
