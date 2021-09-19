package zygarde.data.jpa.entity

fun AutoIdGetter<Int>.getId(): Int = id!!
fun AutoIdGetter<Long>.getId(): Long = id!!
