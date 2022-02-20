package zygarde.security

import org.springframework.security.core.Authentication

interface ApiRoleAuthService {
  fun auth(authHeaderValue: String): Authentication
}
