package zygarde.data.jpa

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Sort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.data.jpa.dao.TestAuthorDao
import zygarde.data.jpa.dao.TestAuthorGroupDao
import zygarde.data.jpa.dao.TestBookDao
import zygarde.data.jpa.dao.search
import zygarde.data.jpa.dao.searchCount
import zygarde.data.jpa.dao.searchOne
import zygarde.data.jpa.dao.searchPage
import zygarde.data.jpa.entity.Author
import zygarde.data.jpa.entity.AuthorGroup
import zygarde.data.jpa.entity.Book
import zygarde.data.jpa.search.action.dateRange
import zygarde.data.jpa.search.action.dateTimeRange
import zygarde.data.jpa.search.action.impl.SearchableImpl
import zygarde.data.jpa.search.request.PagingAndSortingRequest
import zygarde.data.jpa.search.request.PagingRequest
import zygarde.data.search.SearchDateRange
import zygarde.data.search.SearchDateTimeRange
import zygarde.data.search.SearchKeyword
import zygarde.data.search.SearchKeywordType
import zygarde.data.jpa.search.request.SortField
import zygarde.data.jpa.search.request.SortingRequest
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@DirtiesContext
class EnhancedSearchTest {

  @Autowired
  lateinit var groupDao: TestAuthorGroupDao

  @Autowired
  lateinit var authorDao: TestAuthorDao

  @Autowired
  lateinit var bookDao: TestBookDao

  @BeforeEach
  fun setUp() {
    if (groupDao.count() > 0L) {
      return
    }
    val easyRandom = EasyRandom(
      EasyRandomParameters()
        .stringLengthRange(10, 50)
        .overrideDefaultInitialization(true)
    )
    listOf("literature", "comic").forEach { groupName ->
      groupDao.save(AuthorGroup(name = groupName)).also { g ->
        repeat(10) {
          authorDao.save(Author(name = easyRandom.nextObject(String::class.java), authorGroup = g)).also { a ->
            repeat(50) {
              bookDao.save(
                Book(
                  name = if (it == 0) {
                    "zygarde"
                  } else {
                    easyRandom.nextObject(String::class.java)
                  },
                  author = a,
                  price = if (it == 0) {
                    100
                  } else {
                    101 + (Math.random() * 300).toInt()
                  }
                )
              )
            }
          }
        }
      }
    }
  }

  @Test
  fun `should able to search book by name`() {
    bookDao.search {
      field(SearchableImpl<Book, String>("name")) eq "zygarde"
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
    }.size shouldBe 20
  }

  @Test
  fun `should able to search book page by name`() {
    bookDao.searchPage(PagingAndSortingRequest()) {
      field(SearchableImpl<Book, String>("name")) eq "zygarde"
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
    }.totalPages shouldBe 2
  }

  @Test
  fun `should ignore search condition`() {
    bookDao.search { stringField("name") inList null }.size shouldBe 1000
    bookDao.search { stringField("name") inList emptyList() }.size shouldBe 1000
    bookDao.search { stringField("name") notInList null }.size shouldBe 1000
    bookDao.search { stringField("name") notInList emptyList() }.size shouldBe 1000
    bookDao.search { stringField("name") eq null }.size shouldBe 1000
    bookDao.search { stringField("name") keyword null }.size shouldBe 1000
    bookDao.search { stringField("name") keyword SearchKeyword() }.size shouldBe 1000
    bookDao.search { comparableField<LocalDate>("releaseDate") dateRange null }.size shouldBe 1000
    bookDao.search { comparableField<LocalDateTime>("createdAt") dateTimeRange null }.size shouldBe 1000
    bookDao.search { field<Author>("author").comparableField<String>("name") inList emptyList() }.size shouldBe 1000
    bookDao.search { field<Author>("author").field<LocalDate>(SearchableImpl("registerDate")) eq null }.size shouldBe 1000
  }

  @Test
  fun `should serach for not in and not eq`() {
    bookDao.search { stringField("name") notEq "zygarde" }.size shouldBeGreaterThan 0
    bookDao.search { stringField("name") notInList listOf("zygarde") }.size shouldBeGreaterThan 0
  }

  @Test
  fun `should able to search book by authorGroup name`() {
    bookDao.search {
      field(SearchableImpl<Book, Author>("author"))
        .field(SearchableImpl<Author, AuthorGroup>("authorGroup"))
        .field(SearchableImpl<AuthorGroup, String>("name")) eq "comic"
      field(SearchableImpl<Book, Author>("author"))
        .field(SearchableImpl<Author, AuthorGroup>("authorGroup"))
        .field(SearchableImpl<AuthorGroup, String>("name")) contains "com"
    }.size shouldBe 500
    bookDao.search {
      field<Author>("author")
        .field<AuthorGroup>("authorGroup")
        .field<String>("name") eq "comic"
    }.size shouldBe 500
  }

  @Test
  fun `should able to search with limit`() {
    bookDao.search(
      {
        stringField("name") eq "zygarde"
      },
      10
    ).size shouldBe 10
  }

  @Test
  fun `should able to search with order`() {
    bookDao.search(
      {
        comparableField<Int>("price").asc()
      },
      10
    ).first().price shouldBe 100
    bookDao.search(
      {
        comparableField<Int>("price").desc()
      },
      10
    ).first().price shouldNotBe 100
  }

  @Test
  fun `should able to perform isNull search`() {
    bookDao.search {
      stringField("name").isNull()
    }.size shouldBe 0
  }

  @Test
  fun `should able to perform string search`() {
    bookDao.search {
      stringField("name") startsWith "zygarde"
      stringField("name") endsWith "zygarde"
      stringField("name") contains "zygarde"
      stringField("name") keyword SearchKeyword("zygarde", SearchKeywordType.MATCH)
      stringField("name") keyword SearchKeyword().also {
        it.keyword = "zygarde"
        it.type = SearchKeywordType.MATCH
      }
      stringField("name") inList listOf("zygarde")
      stringField("name").isNotNull()
    }.size shouldBeGreaterThan 0
  }

  @Test
  fun `should able to perform comparable search`() {
    bookDao.search {
      or {
        comparableField<Int>("price") inList listOf(100)
        comparableField<Int>("price") gte 100
        comparableField<Int>("price") gt 99
        comparableField<Int>("price") lte 999
        comparableField<Int>("price") lt 1000
        comparableField<Int>("price") eq 100
        and {
          stringField("name") startsWith "zygarde"
          stringField("name") endsWith "zygarde"
        }
      }
    }.size shouldBeGreaterThan 0
  }

  @Test
  fun `should able to in search with large list`() {
    bookDao.search {
      stringField("name") inList (1..2000).map { "zygarde$it" }
    }.size shouldBe 0
  }

  @Test
  fun `should able to not in search with large list`() {
    bookDao.search {
      stringField("name") notInList (1..2000).map { "zygarde$it" }
    }.size shouldBeGreaterThan 0
  }

  @Test
  fun `should able to count book`() {
    bookDao.searchCount {
      comparableField<Int>("price") gt 100
    } shouldBe 980L
    bookDao.searchCount {
      comparableField<Int>("price") lte 100
    } shouldBe 20L
  }

  @Test
  fun `should able to search one`() {
    bookDao.searchOne {
      field(SearchableImpl<Book, Int>("id")) eq 1
    } shouldNotBe null
    bookDao.searchOne {
      field(SearchableImpl<Book, Int>("id")) eq 0
    } shouldBe null
  }

  @Test
  fun `should able to search page`() {
    bookDao.searchPage(
      PagingAndSortingRequest()
        .also {
          it.paging = PagingRequest(1, 10).also {
            it.page = 1
            it.pageSize = 10
          }
          it.sorting = SortingRequest().also {
            it.sortFields = listOf("id")
            it.sort = Sort.Direction.DESC
          }
        }
    ) {
      field(SearchableImpl<Book, LocalDate>("releaseDate")) dateRange SearchDateRange(
        from = LocalDate.now().minusDays(1),
        to = LocalDate.now().plusDays(1)
      )
      field(SearchableImpl<Book, LocalDate>("releaseDate")) dateRange SearchDateRange().also {
        it.from = LocalDate.now().minusDays(1)
      }
      field(SearchableImpl<Book, LocalDate>("releaseDate")) dateRange SearchDateRange().also {
        it.to = LocalDate.now().plusDays(1)
      }
      field(SearchableImpl<Book, LocalDateTime>("createdAt")) dateTimeRange SearchDateTimeRange(
        from = LocalDate.now().minusDays(1).atStartOfDay(),
        until = LocalDate.now().plusDays(1).atStartOfDay()
      )
      field(SearchableImpl<Book, LocalDateTime>("createdAt")) dateTimeRange SearchDateTimeRange().also {
        it.from = LocalDate.now().minusDays(1).atStartOfDay()
      }
      field(SearchableImpl<Book, LocalDateTime>("createdAt")) dateTimeRange SearchDateTimeRange().also {
        it.until = LocalDate.now().plusDays(1).atStartOfDay()
      }
    }.totalPages shouldBe 100

    bookDao.searchPage(PagingAndSortingRequest().also { it.paging = PagingRequest() }) {}.totalPages shouldBe 100
    bookDao.searchPage(PagingAndSortingRequest().also { it.sorting = SortingRequest() }) {}.totalPages shouldBe 100
  }

  @Test
  fun `should able to search page with multiple sorts`() {
    val page1 = bookDao.searchPage(
      PagingAndSortingRequest()
        .also {
          it.paging = PagingRequest(1, 10).also {
            it.page = 1
            it.pageSize = 10
          }
          it.sorting = SortingRequest().also {
            it.sortFields = listOf("id")
            it.sort = Sort.Direction.DESC
          }
          it.sorts = listOf(
            SortField(Sort.Direction.ASC, "name")
          )
        }
    ) {}

    val page2 = bookDao.searchPage(
      PagingAndSortingRequest()
        .also {
          it.paging = PagingRequest(1, 10).also {
            it.page = 1
            it.pageSize = 10
          }
          it.sorting = null
          it.sorts = listOf(
            SortField(Sort.Direction.DESC, "id"),
            SortField(Sort.Direction.ASC, "name")
          )
        }
    ) {}

    page1.first() shouldBe page2.first()
  }

  @Test
  fun `should able to search concat fields`() {
    bookDao.search {
      concat(
        stringField("name"),
        field<Author>("author").field<AuthorGroup>("authorGroup").stringField("name")
      ) contains "decom" // zygarde+comics => zygardecomics contains 'decom'
    }.size shouldBeGreaterThan 0
  }
}
