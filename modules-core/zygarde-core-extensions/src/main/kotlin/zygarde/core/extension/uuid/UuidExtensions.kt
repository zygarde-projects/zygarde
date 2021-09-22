package zygarde.core.extension.uuid

import java.util.UUID

fun UUID.to32digits() = this.toString().replace("-".toRegex(), "")
