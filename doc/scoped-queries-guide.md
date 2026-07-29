# ScopedQueries 使用教學

這份說明整理了 `@ScopeMarker`、`@ScopeOp`、`@NullEquivalent`、`ScopeFilter` 與 codegen 生成 scoped DAO API 的使用方式，適合提供給下游專案或團隊成員快速上手。

> 本文對應目前主幹 (`v2`) 的實作；舊版 `scoped-queries-slack-guide.md` 描述的 fluent `dao.xxxScoped(scope)` 設計已被棄用，請以本文為準。

## 這個功能解決什麼問題

當某些 Entity 必須帶特定 scope 才能查詢時，例如：

- `tenant`
- `platform`
- `brand`
- `visibility`

過去常見寫法是把條件直接散落在每個 DAO 查詢裡：

```kotlin
noteDao.search {
  field<String>("tenantId") eq tenantId
  field<String>("visibility") eq "PUBLIC"
  title() like "%$keyword%"
}
```

這樣的問題是：

- 容易漏寫 scope 條件
- 相同規則會重複散落在各查詢點
- 無法在型別層保證哪些 Entity 必須帶 scope
- 很難靠 code review 穩定維持

`ScopedQueries` 的目標是把這件事收斂成：

```kotlin
noteDao.search(NoteScope(tenantId = tenantId, visibility = "PUBLIC")) {
  title() like "%$keyword%"
}
```

並且讓「漏傳 scope」直接變成編譯錯誤。

## 核心概念

### 1. `@ScopeMarker`

`@ScopeMarker` 用來標記某個 interface 是「scope marker」，它的 property 會被視為 scope 欄位。

```kotlin
@ScopeMarker
interface TenantScoped {
  val tenantId: String
}
```

- 只能標在 **interface** 上 (`@Target(AnnotationTarget.CLASS)`)
- 沒有任何參數
- Retention 為 `BINARY`，KAPT 和 KSP 兩邊都能讀到

一個 Entity 可以實作多個 `@ScopeMarker` interface，它們的 property 會被合併到同一個 `{Entity}Scope` 裡。

### 2. `@ScopeOp`

`@get:ScopeOp` 用來選擇 scope property 產生的 predicate；未標註時預設為 `IN`。

```kotlin
@ScopeMarker
interface OrderScoped {
  val status: OrderStatus

  @get:ScopeOp(ScopeOperator.NOT_IN)
  val excludedStatus: OrderStatus

  @get:ScopeOp(ScopeOperator.IS_NOT_NULL)
  val demandLinked: Boolean
}
```

| Operator | Generated scope value | `true`／values 的效果 | `false`／`null` 的效果 |
| --- | --- | --- | --- |
| `IN` | `ScopeFilter<T>` | 欄位值包含在集合內 | `All` 或 optional `null` 略過 |
| `NOT_IN` | `ScopeFilter<T>` | 排除集合中的欄位值 | `All` 或 optional `null` 略過 |
| `IS_NULL` | `Boolean` | `IS NULL` | 略過 |
| `IS_NOT_NULL` | `Boolean` | `IS NOT NULL` | 略過 |

### 3. `@NullEquivalent`

`@NullEquivalent("SENTINEL")` 標在 scope interface 的 property getter 上，代表「這個值等同於 NULL，查詢時要額外 OR IS NULL」。

```kotlin
@ScopeMarker
interface VisibilityScoped {
  @get:NullEquivalent("PUBLIC")
  val visibility: String?
}
```

當 scope collection 裡出現 `"PUBLIC"`，生成的條件會變成：

```
visibility IN (...) OR visibility IS NULL
```

**限制：** sentinel 比較是用 `it.toString() == "SENTINEL"`，所以欄位型別只能是：

- `String`
- 未覆寫 `toString()` 的 `Enum`（預設 `toString()` 等於 `name()`）

value class、data class、或自定義 `toString()` 的型別會安靜地錯過 sentinel，不會觸發 `OR IS NULL`。

`@NullEquivalent` 只影響 `IN` 和 `NOT_IN`。搭配 `NOT_IN` 時：

- 排除集合包含 sentinel：生成 `NOT IN (...) AND IS NOT NULL`，SQL NULL 也被排除。
- 排除集合不含 sentinel：生成 `NOT IN (...) OR IS NULL`，SQL NULL 保持包含。

### 4. Generated `{Entity}Scope` data class

只要 Entity 實作任何有 property 的 `@ScopeMarker` interface，codegen 就會在 DAO 所在 package 產出一個 `{Entity}Scope` data class：

```kotlin
// Generated: NoteScope.kt
public data class NoteScope(
  public val tenantId: ScopeFilter<String>,
  public val visibility: ScopeFilter<String> = ScopeFilter.All,
) {
  public constructor(tenantId: Collection<String>, visibility: Collection<String>? = null) : this(
    tenantId = ScopeFilter.Of(tenantId),
    visibility = visibility?.let { ScopeFilter.Of(it) } ?: ScopeFilter.All,
  )

  public constructor(tenantId: String, visibility: String? = null) : this(
    tenantId = ScopeFilter.Of(listOf(tenantId)),
    visibility = visibility?.let { ScopeFilter.Of(listOf(it)) } ?: ScopeFilter.All,
  )
}
```

重點：

- Membership 欄位使用 `ScopeFilter<T>`，明確區分「略過」與「使用集合篩選」
- `ScopeFilter.All` 明確略過該欄位的 predicate
- `ScopeFilter.Of(values)` 使用集合篩選；空集合刻意 match nothing
- Entity property 可為 null 時，對應 membership scope 欄位預設 `ScopeFilter.All`，代表略過該欄位；`IS_NULL`／`IS_NOT_NULL` 的 Boolean 欄位仍以 `null` 略過
- 仍附上接受 `Collection<T>` 與單一 `T` 的 compatibility constructors
- nullable membership 的 literal `null` 會明確解析到 collection compatibility constructor，並轉成 `ScopeFilter.All`；`Scope()`、`Scope(field = null)` 與 `Scope(null)` 都不會產生 overload ambiguity

### 5. Generated scoped DAO extensions

同時會產出一份 `{Entity}DaoExtensions.kt`，提供以下 **top-level extension functions**：

```kotlin
fun NoteDao.search(scope: NoteScope, searchContent: EnhancedSearch<Note>.() -> Unit = {}): List<Note>
fun NoteDao.search(scope: NoteScope, sorts: List<SortField>?, searchContent: EnhancedSearch<Note>.() -> Unit): List<Note>
fun NoteDao.search(scope: NoteScope, searchContent: EnhancedSearch<Note>.() -> Unit, limit: Int): List<Note>
fun NoteDao.searchCount(scope: NoteScope, searchContent: EnhancedSearch<Note>.() -> Unit = {}): Long
fun NoteDao.searchOne(scope: NoteScope, searchContent: EnhancedSearch<Note>.() -> Unit = {}): Note?
fun NoteDao.searchOneOrThrow(scope: NoteScope, errorCode: ErrorCode, searchContent: EnhancedSearch<Note>.() -> Unit = {}): Note
fun NoteDao.searchPage(scope: NoteScope, req: PagingAndSortingRequest, searchContent: EnhancedSearch<Note>.() -> Unit = {}): Page<Note>

// Only if DAO extends ZygardeEnhancedDao:
fun NoteDao.remove(scope: NoteScope, searchContent: EnhancedSearch<Note>.() -> Unit = {}): Int
```

不帶 scope 的同名函式只會生成 `DeprecationLevel.ERROR` placeholder，用來讓 compiler 顯示具體的 scope 類別與 `ScopeFilter.All` 修法；其函式內容也會直接 `error(...)`，不會執行 unscoped query。

## 一般使用流程

### Step 1. 定義 marker interface

```kotlin
@ScopeMarker
interface TenantScoped {
  val tenantId: String
}

@ScopeMarker
interface VisibilityScoped {
  @get:NullEquivalent("PUBLIC")
  val visibility: String?
}
```

### Step 2. 讓需要受限制的 Entity 實作它

```kotlin
@ZyModel
@Entity
class Note(
  var title: String = "",
  override var tenantId: String = "",
  override var visibility: String? = null,
) : AutoLongIdEntity(), TenantScoped, VisibilityScoped
```

不需要受 scope 限制的 Entity 不要實作 — 它們的 DAO 會產出不帶 scope 的原本版本。

### Step 3. 直接使用 generated scoped API

```kotlin
// 基本查詢
noteDao.search(NoteScope(tenantId = "t1")) {
  title() like "%$keyword%"
}

// 多值 scope
noteDao.search(
  NoteScope(
    tenantId = listOf("t1", "t2"),
    visibility = listOf("PUBLIC", "PRIVATE"),
  ),
)

// 分頁
noteDao.searchPage(NoteScope(tenantId = "t1"), pagingReq) {
  createdAt().dateRange(dateRange)
}

// Count
noteDao.searchCount(NoteScope(tenantId = "t1", visibility = "PUBLIC"))

// 找一筆或丟錯
noteDao.searchOneOrThrow(NoteScope(tenantId = "t1"), NoteError.NOT_FOUND) {
  id() eq noteId
}

// 排序
noteDao.search(
  NoteScope(tenantId = "t1"),
  sorts = listOf(SortField(field = "title")),
  searchContent = {},
)
```

## 型別安全保證

### 可保證的情況

**漏傳 scope 不能編譯：**

```kotlin
noteDao.search { title() like "%$keyword%" }
```

會編譯失敗，並收到類似以下的指引：

```text
此實體已啟用 ScopedQueries：請以第一參數傳入 NoteScope
（全放行請顯式傳 NoteScope(tenantId = ScopeFilter.All)）
```

**傳錯 scope 類別不能編譯：**

```kotlin
noteDao.search(OrderScope(...)) { }
```

`OrderScope` 型別不符，編譯失敗。

**無 scope 的 Entity 不能用 scope：**

```kotlin
productDao.search(ProductScope(...))  // ProductScope 根本不存在
```

如果 `Product` 沒實作任何 `@ScopeMarker` interface，`ProductScope` 不會被生成，直接 unresolved reference。

### 安全邊界與框架無法保證的情況

ScopedQueries 的 compiler 保證只涵蓋 generated `search`／`searchOne`／`searchCount`／`searchPage`／`remove` extensions。
DAO 仍繼承的 Spring Data API（例如 `JpaSpecificationExecutor<T>.findAll(spec)`、`findOne(spec)`）可以直接呼叫，
`ScopeFilter.All` 也能刻意略過 predicate。因此 ScopedQueries **不是 authorization 或資料隔離的安全邊界**。

這項限制是刻意保留的架構邊界：generator 不會移除 Spring Data API，也不會改寫 `ZygardeEnhancedDao` 的
`select`／`selectOne`。若 scope 涉及租戶隔離或存取控制，請另外以 service/repository facade 限制 DAO 暴露面，
並依團隊需求用 code review 或 detekt 自定規則阻止直接呼叫原生 API；不要只依賴 generated extension signature。

## 執行時重要細節

### Scope 的空 Collection 會 match nothing

Scope compatibility constructor 會把 collection 包成 `ScopeFilter.Of(values)`。空集合因此是明確的「沒有任何允許值」，結果為零筆：

```kotlin
noteDao.search(NoteScope(tenantId = emptyList()))
// → 0 筆
```

需要刻意略過某個 scope predicate 時，必須顯式使用 `ScopeFilter.All`：

```kotlin
noteDao.search(NoteScope(tenantId = ScopeFilter.All))
```

一般 search DSL 的既有 `inList(emptyList())` 仍會略過條件，沒有改變。若一般 DSL 的空集合也應 match nothing，請改用 `inListStrict(values)`；兩者傳入 `null` 時都會略過條件。

### `@NullEquivalent` 必須是 String 或 default-toString Enum

如前所述，sentinel 比較用 `toString()`。以下寫法會 **靜默失效**：

```kotlin
@ScopeMarker
interface CurrencyScoped {
  @get:NullEquivalent("USD")
  val currency: Money?   // Money 有自訂 toString() → sentinel 永遠對不上
}
```

請改成 `String` 或確認 `Enum` 沒有 override `toString()`。

### 排序 overload 的 fallback

`search(scope, sorts, searchContent)` 當 `sorts = null` 時會直接 fallback 到 `findAll(SearchSpecBuilder.buildSpec(searchContent))`，**不會** re-delegate 到 `search(scope, searchContent)` — 這樣可以避免 scope predicate 被包兩次。

## KSP vs KAPT

兩邊的處理器產出的 API 完全一致：

| 模組 | 位置 | 適用 |
| --- | --- | --- |
| KAPT | `modules-jpa/zygarde-jpa-codegen` | 傳統 annotation processor |
| KSP | `modules-jpa/zygarde-jpa-codegen-ksp` | Kotlin Symbol Processing，編譯較快 |

Sample 專案：

- `samples/todo-legacy` — KAPT 範例 + `@DataJpaTest`／H2 scoped runtime smoke test
- `samples/todo-ksp` — KSP 範例 + `@DataJpaTest` 整合測試（`NoteScopedQueryTest`）

同一份 marker interface + entity 在兩邊的生成結果是等價的；專案可以只選其中一邊接入。

## 建議團隊規範

建議把這個功能當成固定約定，而不是選用功能：

- 所有帶 `@ScopeMarker` 的 entity，都應該只透過生成的 `search(scope, ...)` 查詢
- 不要在業務代碼裡直接手寫對應 scope 條件（`field<...>("tenantId") eq ...` 這類）
- 共用的 scope 組合建議包成 factory function 或 `object`：

```kotlin
object NoteScopes {
  fun currentTenant(): NoteScope = NoteScope(tenantId = TenantContext.current())
  fun publicOnly(tenantId: String): NoteScope = NoteScope(
    tenantId = tenantId,
    visibility = "PUBLIC",
  )
}
```

- 不要以空集合表示全放行；scope 必須使用 `ScopeFilter.All` 明確表達
- 一般 DSL 對使用者輸入集合需要「空集合＝零筆」時使用 `inListStrict`
- `@NullEquivalent` 欄位一律用 `String` 或 default-toString `Enum`

## 最推薦的實作範例

```kotlin
@ScopeMarker
interface TenantScoped {
  val tenantId: String
}

@ScopeMarker
interface VisibilityScoped {
  @get:NullEquivalent("PUBLIC")
  val visibility: String?
}

@ZyModel
@Entity
class Note(
  var title: String = "",
  override var tenantId: String = "",
  override var visibility: String? = null,
) : AutoLongIdEntity(), TenantScoped, VisibilityScoped
```

使用端：

```kotlin
// 查 t1 租戶底下所有公開筆記
noteDao.search(NoteScope(tenantId = "t1", visibility = "PUBLIC"))

// 翻頁
noteDao.searchPage(NoteScope(tenantId = "t1"), pagingReq) {
  createdAt().dateRange(dateRange)
}

// 計數
noteDao.searchCount(NoteScope(tenantId = "t1"))

// 精準單筆
noteDao.searchOneOrThrow(NoteScope(tenantId = "t1"), NoteError.NOT_FOUND) {
  id() eq noteId
}
```

## 總結

`ScopedQueries` 這套設計的核心價值是：

- 把 scope 條件從查詢細節抽離成共用資料結構
- 用 codegen 產生一致的 scoped API，不需手刻
- 利用 scoped overload 與 error-deprecated placeholder，讓 compiler 直接擋下漏傳 scope 並提供修法
- 用 `ScopeFilter.All`／`Of` 區分顯式全放行、正常集合與空集合
- 以 `ScopeOp` 表達 `IN`、`NOT_IN`、`IS_NULL`、`IS_NOT_NULL`
- `@NullEquivalent` 處理 sentinel ↔ NULL 的常見模式

如果你的專案有類似：

- 租戶隔離
- 平台隔離
- 品牌隔離
- 通路隔離
- 可見性控制

這套模式都可以直接套用。
