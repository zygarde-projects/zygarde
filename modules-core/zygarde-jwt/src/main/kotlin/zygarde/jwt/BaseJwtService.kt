package zygarde.jwt

import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts

open class BaseJwtService {

  private val jwtParser = Jwts.parserBuilder().build()

  fun parseWithoutKey(token: String): Jwt<out Header<*>, *> {
    val splitToken = token.split(".")
    val unsignedToken = splitToken[0] + "." + splitToken[1] + "."
    return jwtParser.parse(unsignedToken)
  }
}
