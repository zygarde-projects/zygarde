package zygarde.data.jpa.entity

import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
open class Author(
  var name: String = "",
  @ManyToOne(targetEntity = AuthorGroup::class, fetch = FetchType.LAZY)
  open var authorGroup: AuthorGroup? = null,
  @OneToMany(targetEntity = Book::class, fetch = FetchType.LAZY, mappedBy = "author")
  var books: Set<Book> = emptySet(),
  open var registerDate: LocalDate = LocalDate.now()
) : AutoIntIdEntity()
