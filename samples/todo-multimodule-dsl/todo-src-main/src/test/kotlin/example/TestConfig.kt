package example

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import zygarde.test.feign.autoconfigure.ZygardeSpringTestFeignConfig

@Configuration
@EnableFeignClients("example")
@Import(ZygardeSpringTestFeignConfig::class)
class TestConfig
