package zygarde.test.entity

import zygarde.data.jpa.entity.AutoIntIdEntity
import java.time.LocalDate
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
open class Author(
  var name: String = "",
  @ManyToOne(targetEntity = AuthorGroup::class, fetch = FetchType.LAZY)
  open var authorGroup: AuthorGroup? = null,
  @OneToMany(targetEntity = Book::class, fetch = FetchType.LAZY, mappedBy = "author")
  var books: Set<Book> = emptySet(),
  open var registerDate: LocalDate = LocalDate.now()
) : AutoIntIdEntity()
