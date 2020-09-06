package zygarde.core.extension.uuid

import java.util.UUID

fun UUID.to36digits() = this.toString().replace("-".toRegex(), "")
