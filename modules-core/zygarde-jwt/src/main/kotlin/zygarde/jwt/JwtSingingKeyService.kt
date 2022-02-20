package zygarde.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.crypto.spec.SecretKeySpec

open class JwtSingingKeyService(
  private val jwtKey: String,
  private val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.HS512,
) : BaseJwtService() {

  private val signingKeyBytes: ByteArray by lazy {
    jwtKey.toByteArray()
  }

  private val jwtSigningKey: SecretKeySpec by lazy {
    SecretKeySpec(
      signingKeyBytes,
      signatureAlgorithm.jcaName,
    )
  }

  fun sign(claims: Map<String, Any>, expireAt: LocalDateTime): String {
    return Jwts.builder()
      .setIssuedAt(Date())
      .setExpiration(
        Date.from(
          expireAt.atZone(ZoneId.systemDefault()).toInstant()
        )
      )
      .addClaims(claims)
      .signWith(jwtSigningKey)
      .compact()
  }

  fun parseClaimsJws(token: String): Jws<Claims> {
    return Jwts.parserBuilder()
      .setSigningKey(signingKeyBytes)
      .build()
      .parseClaimsJws(token)
  }
}
