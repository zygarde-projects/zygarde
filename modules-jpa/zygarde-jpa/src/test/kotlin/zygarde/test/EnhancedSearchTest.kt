package zygarde.test

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import zygarde.data.api.PagingAndSortingRequest
import zygarde.data.api.PagingRequest
import zygarde.data.api.SortDirection
import zygarde.data.api.SortField
import zygarde.data.jpa.dao.remove
import zygarde.data.jpa.dao.search
import zygarde.data.jpa.dao.searchCount
import zygarde.data.jpa.dao.searchOne
import zygarde.data.jpa.dao.searchPage
import zygarde.data.jpa.entity.getId
import zygarde.data.jpa.search.action.ComparableConditionAction
import zygarde.data.jpa.search.action.ConditionAction
import zygarde.data.jpa.search.action.StringConditionAction
import zygarde.data.jpa.search.action.dateRange
import zygarde.data.jpa.search.action.dateTimeRange
import zygarde.data.jpa.search.action.range
import zygarde.data.jpa.search.crossJoin
import zygarde.data.search.SearchDateRange
import zygarde.data.search.SearchDateTimeRange
import zygarde.data.search.SearchKeyword
import zygarde.data.search.SearchKeywordType
import zygarde.data.search.range.SearchIntRangeOverlap
import zygarde.data.search.range.SearchRange
import zygarde.test.dao.TestAuthorDao
import zygarde.test.dao.TestAuthorGroupDao
import zygarde.test.dao.TestBookDao
import zygarde.test.dao.TestComputerDao
import zygarde.test.entity.Author
import zygarde.test.entity.AuthorGroup
import zygarde.test.entity.Book
import zygarde.test.entity.BookStatus
import zygarde.test.entity.Computer
import zygarde.test.entity.Gpu
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = [ZygardeJpaTestApplication::class])
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DirtiesContext
class EnhancedSearchTest {

  @Autowired
  lateinit var groupDao: TestAuthorGroupDao

  @Autowired
  lateinit var authorDao: TestAuthorDao

  @Autowired
  lateinit var bookDao: TestBookDao

  @Autowired
  lateinit var computerDao: TestComputerDao

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
        repeat(10) { authorIdx ->
          authorDao.save(Author(name = "Author$authorIdx", authorGroup = g)).also { a ->
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
                  },
                  aId = a.getId()
                )
              )
            }
          }
        }
      }
    }
  }

  @Order(100)
  @Test
  fun `should able to search book by name`() {
    bookDao.search {
      field(Book::name) eq "zygarde"
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
    }.size shouldBe 20
  }

  @Order(200)
  @Test
  fun `should able to search book page by name`() {
    bookDao.searchPage(PagingAndSortingRequest()) {
      field(Book::name) eq "zygarde"
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
    }.totalPages shouldBe 2
  }

  @Order(300)
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
    bookDao.search { field<Author>("author").field<LocalDate>("registerDate") eq null }.size shouldBe 1000
    bookDao.search { rangeOverlap({ field(Book::minPrice) }, { field(Book::maxPrice) }, SearchIntRangeOverlap(0, 1000)) }.size shouldBe 1000
  }

  @Order(400)
  @Test
  fun `should serach for not in and not eq`() {
    bookDao.search { stringField("name") notEq "zygarde" }.size shouldBeGreaterThan 0
    bookDao.search { stringField("name") notInList listOf("zygarde") }.size shouldBeGreaterThan 0
  }

  @Order(500)
  @Test
  fun `should able to search book by authorGroup name`() {
    bookDao.search {
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
      field(Book::author)
        .field<AuthorGroup>(Author::authorGroup.name)
        .field(AuthorGroup::name) eq "comic"
      val author: ConditionAction<Book, Book, Author> = field(Book::author)
      val authorGroup: ConditionAction<Book, Author, AuthorGroup> = author.field(Author::authorGroup)
      val authorGroupName: StringConditionAction<Book, AuthorGroup> = authorGroup.field(AuthorGroup::name)
      authorGroupName contains "com"
    }.size shouldBe 500
    bookDao.search {
      field<Author>("author").field<AuthorGroup>("authorGroup").join()
      field<Author>("author").field<AuthorGroup>("authorGroup")
        .field<String>("name") eq "comic"
    }.size shouldBe 500

    bookDao.search {
      field<Author>("author").field<AuthorGroup>("authorGroup").field<String>("name") eq "comic"
    }.size shouldBe 500
  }

  @Order(600)
  @Test
  fun `should able to search with limit`() {
    bookDao.search(
      {
        stringField("name") eq "zygarde"
      },
      10
    ).size shouldBe 10
  }

  @Order(700)
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

  @Order(800)
  @Test
  fun `should able to perform isNull search`() {
    bookDao.search {
      stringField("name").isNull()
    }.size shouldBe 0
  }

  @Order(900)
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

  @Order(1000)
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
          stringField("name").upper() startsWith "ZYGARDE"
          stringField("name").lower() startsWith "zygarde"
          stringField("name").trim() startsWith "zygarde"
        }
      }
    }.size shouldBeGreaterThan 0
  }

  @Order(1100)
  @Test
  fun `should able to in search with large list`() {
    bookDao.search {
      stringField("name") inList (1..2000).map { "zygarde$it" }
    }.size shouldBe 0
  }

  @Order(1200)
  @Test
  fun `should able to not in search with large list`() {
    bookDao.search {
      stringField("name") notInList (1..2000).map { "zygarde$it" }
    }.size shouldBeGreaterThan 0
  }

  @Order(1300)
  @Test
  fun `should able to count book`() {
    bookDao.searchCount {
      comparableField<Int>("price") gt 100
    } shouldBe 980L
    bookDao.searchCount {
      comparableField<Int>("price") lte 100
    } shouldBe 20L
  }

  @Order(1400)
  @Test
  fun `should able to search one`() {
    bookDao.searchOne {
      field(Book::id) eq 1
    } shouldNotBe null
    bookDao.searchOne {
      field(Book::id) eq 0
    } shouldBe null
  }

  @Order(1500)
  @Test
  fun `should able to search page`() {
    bookDao.searchPage(
      PagingAndSortingRequest()
        .also {
          it.paging = PagingRequest(1, 10).also {
            it.page = 1
            it.pageSize = 10
          }
          it.sorts = listOf(SortField(SortDirection.DESC, "id"))
        }
    ) {
      field(Book::releaseDate) dateRange SearchDateRange(
        from = LocalDate.now().minusDays(1),
        to = LocalDate.now().plusDays(1)
      )
      field(Book::releaseDate) dateRange SearchDateRange().also {
        it.from = LocalDate.now().minusDays(1)
      }
      field(Book::releaseDate) dateRange SearchDateRange().also {
        it.to = LocalDate.now().plusDays(1)
      }
      val field: ComparableConditionAction<Book, Book, LocalDateTime> = field(Book::createdAt)
      field dateTimeRange SearchDateTimeRange(
        from = LocalDate.now().minusDays(1).atStartOfDay(),
        until = LocalDate.now().plusDays(1).atStartOfDay()
      )
      field(Book::createdAt) dateTimeRange SearchDateTimeRange().also {
        it.from = LocalDate.now().minusDays(1).atStartOfDay()
      }
      field(Book::createdAt) dateTimeRange SearchDateTimeRange().also {
        it.until = LocalDate.now().plusDays(1).atStartOfDay()
      }
    }.totalPages shouldBe 100

    bookDao.searchPage(PagingAndSortingRequest().also { it.paging = PagingRequest() }) {}.totalPages shouldBe 100
    bookDao.searchPage(PagingAndSortingRequest().also { it.sorts = emptyList() }) {}.totalPages shouldBe 100
  }

  @Order(1600)
  @Test
  fun `should able to search page with multiple sorts`() {
    val page1 = bookDao.searchPage(
      PagingAndSortingRequest()
        .also {
          it.paging = PagingRequest(1, 10).also {
            it.page = 1
            it.pageSize = 10
          }
          it.sorts = listOf(
            SortField(SortDirection.DESC, "id"),
            SortField(SortDirection.ASC, "name"),
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
          it.sorts = listOf(
            SortField(SortDirection.DESC, "id"),
            SortField(SortDirection.ASC, "name")
          )
        }
    ) {}

    page1.first() shouldBe page2.first()
  }

  @Order(1700)
  @Test
  fun `should able to search concat fields`() {
    bookDao.search {
      concat(
        stringField("name"),
        field<Author>("author").field<AuthorGroup>("authorGroup").stringField("name")
      ) contains "decom" // zygarde+comics => zygardecomics contains 'decom'
    }.size shouldBeGreaterThan 0
  }

  @Order(1800)
  @Test
  fun `cross join test`() {
    bookDao.search {
      crossJoin<Author> {
        it.field<Int>("id") eq field("aId")
        it.stringField("name") eq "Author1"
      }
    }.size shouldBe 100
  }

  @Order(1900)
  @Test
  fun `select for prop`() {
    bookDao.selectOne(Book::name) {
      field(Book::name) eq "zygarde"
    } shouldBe "zygarde"

    bookDao.select(Book::name) { distinct() } shouldContain "zygarde"
  }

  interface IBookProjection {
    val id: Int
    var name: String
  }

  @Order(1901)
  @Test
  fun `select for projection interface`() {
    bookDao
      .select(IBookProjection::class) {
        field(Book::name) eq "zygarde"
      }
      .also { it.size shouldBeGreaterThan 0 }
      .forEach { it.name shouldBe "zygarde" }
  }

  class CBookProjection(val id: Long) {
    var name: String = ""
    var status: BookStatus = BookStatus.ON_SALE
  }

  @Order(1902)
  @Test
  fun `select for projection class`() {
    bookDao
      .select(CBookProjection::class) {
        field(Book::name) eq "zygarde"
      }
      .also { it.size shouldBeGreaterThan 0 }
      .forEach {
        it.name shouldBe "zygarde"
        it.status shouldBe BookStatus.ON_SALE
      }
  }

  @Order(2000)
  @Test
  fun `search by int range`() {
    bookDao.search {
      comparableField<Int>("price") range SearchRange.Number.SearchRangeInt(100, 500)
    }.all { it.price in 100..500 } shouldBe true
    bookDao.search {
      comparableField<Int>("price") range SearchRange.Number.SearchRangeInt(100, 101).also { it.toExclusive = true }
    }.all { it.price == 100 } shouldBe true
    bookDao.search {
      comparableField<Int>("price") range SearchRange.Number.SearchRangeInt(99, 100).also { it.fromExclusive = true }
    }.all { it.price == 100 } shouldBe true
  }

  @Order(9000)
  @Test
  fun `should able to delete`() {
    bookDao.remove {
      field(Book::name) eq "zygarde"
    }

    bookDao.search {
      field(Book::name) eq "zygarde"
    }.size shouldBe 0
  }

  @Order(10000)
  @Test
  fun `should able to count by embedded field`() {
    computerDao.saveAll(
      listOf(
        Computer("Gaming", Gpu()),
        Computer("Work", Gpu()),
      )
    )

    computerDao.search {
      field<Gpu>("gpu").field<Double>("price") eq 1000.0
    }.size shouldBe 2

    computerDao.searchCount {
      field<Gpu>("gpu").field<Double>("price") eq 1000.0
    } shouldBe 2L
  }

  @Order(11000)
  @Test
  fun `should able to search page with oneToMany`() {
    authorDao
      .searchPage(PagingAndSortingRequest().also { it.paging = PagingRequest(page = 1, pageSize = 1) }) {
        stringField("name") contains "Author"
        field<Book>("books")
          .comparableField<Int>("price") gte 100
      }
      .totalPages shouldBeGreaterThan 0
  }

  @Order(11001)
  @Test
  fun `should able to search page with manyToOne then oneToMany`() {
    bookDao
      .searchPage(PagingAndSortingRequest().also { it.paging = PagingRequest(page = 1, pageSize = 1) }) {
        field(Book::author).field<Book>(Author::books.name)
          .comparableField<Int>(Book::price.name) gte 100
      }
      .totalPages shouldBeGreaterThan 0
  }
}
