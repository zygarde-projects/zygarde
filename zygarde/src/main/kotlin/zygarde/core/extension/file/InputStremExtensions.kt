package zygarde.core.extension.file

import java.io.InputStream
import java.io.OutputStream

fun InputStream.safeCopyTo(target: OutputStream) = this.use { ins -> target.use { ous -> ins.copyTo(ous) } }
