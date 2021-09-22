package zygarde.fixture

abstract class Fixture {

  @Throws(Throwable::class)
  abstract fun run()

  open fun order(): Int = 0
}
