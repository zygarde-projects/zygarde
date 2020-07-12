package zygarde.test.feign

import feign.Request
import feign.Response
import feign.Util
import feign.slf4j.Slf4jLogger
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import zygarde.core.extension.general.fallbackWhenNull
import zygarde.json.toJsonString

class TestFeignLogger(clazz: Class<*>) : Slf4jLogger(clazz) {
  private val jwtParser = Jwts.parserBuilder().build()

  override fun logRequest(configKey: String?, logLevel: Level?, request: Request) {
    val token = request.headers()["Authorization"]?.firstOrNull()
    val tokenContent = if (token != null) {
      val splitToken = token.split(".")
      val unsignedToken = splitToken[0] + "." + splitToken[1] + "."
      val jwt: Jwt<*, *> = jwtParser.parse(unsignedToken)
      jwt.body.toJsonString()
    } else {
      ""
    }
    log(configKey, "---> %s %s", request.httpMethod().name, request.url())
    log(configKey, "token: $tokenContent")
    if (request.requestBody().asBytes() != null) {
      log(configKey, "%s", request.requestBody().asString().fallbackWhenNull("Binary data"))
    }
  }

  override fun logAndRebufferResponse(configKey: String?, logLevel: Level?, response: Response?, elapsedTime: Long): Response {
    val reason = if (response!!.reason() != null && logLevel!!.compareTo(Level.NONE) > 0) " " + response.reason() else ""
    val status = response.status()
    if (response.body() != null) {
      val bodyData = Util.toByteArray(response.body().asInputStream())
      val bodyLength = bodyData.size
      if (bodyLength > 0) {
        log(configKey, "%s", Util.decodeOrDefault(bodyData, Util.UTF_8, "Binary data"))
      }
      log(configKey, "<--- %s%s (%sms)", status, reason, elapsedTime)
      return response.toBuilder().body(bodyData).build()
    } else {
      return response
    }
  }
}
