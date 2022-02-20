package zygarde.security

abstract class ApiRole {
  abstract fun antPattern(): String
  abstract fun authHeader(): String
  abstract fun desc(): String
  abstract fun authTokenService(): ApiRoleAuthService?
}
