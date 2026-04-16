# ScopedQueries 使用教學

這份說明整理了 `@ScopeMarker`、`@NullEquivalent` 與 codegen 生成 scoped DAO API 的使用方式，適合提供給下游專案或團隊成員快速上手。

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

### 2. `@NullEquivalent`

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

### 3. Generated `{Entity}Scope` data class

只要 Entity 實作任何有 property 的 `@ScopeMarker` interface，codegen 就會在 DAO 所在 package 產出一個 `{Entity}Scope` data class：

```kotlin
// Generated: NoteScope.kt
public data class NoteScope(
  public val tenantId: Collection<String>,
  public val visibility: Collection<String>? = null,
) {
  public constructor(tenantId: String, visibility: String? = null) : this(
    tenantId = listOf(tenantId),
    visibility = visibility?.let { listOf(it) },
  )
}
```

重點：

- 每個 scope 欄位都是 `Collection<T>`，支援多值 `IN` 查詢
- 可為 null 的欄位會變成 `Collection<T>?` 並預設 `null`（= 不加條件）
- 自動附上「單值」便利 constructor

### 4. Generated scoped DAO extensions

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

注意：**沒有**不帶 scope 的 `search(...)` overload — 這就是「強制」的本質。

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

會編譯失敗 — generator 沒有產出不帶 scope 的 overload。

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

### 框架無法保證的情況

`JpaSpecificationExecutor<T>` 的原生 `findAll(spec)` 仍然可呼叫。如果團隊要完全封掉繞路寫法，請在 code review 或 detekt 自定規則中補一層規範。

## 執行時重要細節

### 空 Collection 等於「不加條件」

Zygarde 的 `ConditionActionImpl.inList` 對空集合會 **silently skip**，不會產出 `WHERE field IN ()`：

```kotlin
noteDao.search(NoteScope(tenantId = emptyList()))
// → 回傳所有 Note，而非 0 筆
```

如果呼叫端需要「空集合就回傳 0 筆」的語意，必須自行在呼叫前檢查：

```kotlin
if (tenantIds.isEmpty()) return emptyList()
noteDao.search(NoteScope(tenantId = tenantIds))
```

這個行為是 DSL 層的既有約定，scope 沿用同一個 inList，所以也繼承這個陷阱。測試 `NoteScopedQueryTest` 有一個 case 把這個不變量 lock 住。

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

- `samples/todo-legacy` — KAPT 範例
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

- 呼叫端必須先檢查空集合再傳進 scope，避免誤判為 no-filter
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
- 利用「只生成帶 scope 的 overload」讓 compiler 直接擋下漏傳 scope 的寫法
- `@NullEquivalent` 處理 sentinel ↔ NULL 的常見模式

如果你的專案有類似：

- 租戶隔離
- 平台隔離
- 品牌隔離
- 通路隔離
- 可見性控制

這套模式都可以直接套用。
