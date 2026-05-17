# GraphQL DSL Codegen 使用教學

這份說明整理了 `zygarde-graphql-codegen-dsl` 模組中 `GraphQlDslCodegen` 的使用方式，適合需要從一份 DSL 宣告同時產生 GraphQL `@Controller`、Service interface 與 `.graphqls` SDL 檔的場景。

> 對應分支:`v3`(已遷移至 Spring Boot 3,可直接使用官方 `spring-boot-starter-graphql`)。
> GraphQL 整體可行性與路線圖請見 `doc/graphql-support-investigation.md`,本文聚焦在「目前已可用的 DSL」實際怎麼寫。

## 這個功能解決什麼問題

手寫 Spring for GraphQL 的 resolver 時,同一份 API 形狀要在三個地方各寫一次:

- `.graphqls` SDL — 給 GraphQL runtime 看的 schema
- `@Controller` 上的 `@QueryMapping` / `@MutationMapping` 函式 — HTTP 端點接線
- Service interface — 商業邏輯契約

三者只要其中一處改了名稱或 nullability,其他兩處沒同步就會在啟動期或 query 期才爆。

`GraphQlDslCodegen` 的目標是讓你只宣告一次:

```kotlin
class TodoGraphQlCodegen : GraphQlDslCodegen() {
  override fun codegen() {
    schema("TodoGraphQl") {
      query("todo") {
        argument<Int>("id")
        returns<TodoDto>("Todo", nullable = true)
        serviceName = "TodoGraphQlService"
      }
      type("Todo") {
        field<Int>("id")
        field<String>("description")
      }
    }
  }
}
```

執行 codegen 後,會自動產出對應的 `TodoGraphQlController`、`TodoGraphQlService` 介面,以及 `todoGraphQl.graphqls`。產生碼與手寫碼維持嚴格隔離(寫到專屬模組),手寫的只有 Service 實作。

## 模組與相依

| 模組 | 角色 |
|---|---|
| `modules-web/zygarde-web-codegen` | `GraphQlApiGenerator` 與 codegen 中介模型(`GraphQlApiToGenerateVo` 等) |
| `modules-web/zygarde-graphql-codegen-dsl` | DSL 入口 `GraphQlDslCodegen`、`GraphQlDslCodegenMain` |

要使用 DSL,在 codegen 模組加入相依即可:

```kotlin
dependencies {
  implementation(project(":zygarde-graphql-codegen-dsl"))
}
```

## 核心概念

### 1. `GraphQlDslCodegen` — DSL 進入點

繼承 `GraphQlDslCodegen`,覆寫 `codegen()`,在裡面呼叫 `schema(...)`。類別必須有 **無參數建構子**,因為產生器是用 reflection 實例化的(見「執行 codegen」)。

```kotlin
class TodoGraphQlCodegen : GraphQlDslCodegen() {
  override fun codegen() {
    schema("TodoGraphQl") { /* ... */ }
  }
}
```

一個 codegen 類別可以宣告多個 `schema(...)`;多個 codegen 類別也會一起被掃描處理。

### 2. `schema(name) { }` — 一份 schema

`schema` 的名稱會變成 `apiName`,並決定三個產出物的命名:

| 產出 | 命名規則 | 範例(`schema("TodoGraphQl")`) |
|---|---|---|
| Controller | `${apiName}Controller` | `TodoGraphQlController` |
| Service interface | `${apiName}Service`(可被 `serviceName` 覆寫) | `TodoGraphQlService` |
| SDL 檔 | `apiName` 首字小寫 + `.graphqls` | `todoGraphQl.graphqls` |

`apiName` 在整個 codegen 執行中必須唯一,否則會以 `GraphQL API 'X' is already declared` 失敗,避免產出物互相覆蓋。

### 3. operation:`query` / `mutation` / `subscription`

```kotlin
schema("TodoGraphQl") {
  query("todos") {
    collectionArgument<Int>("ids")
    returnsCollection<TodoDto>("Todo")
    serviceName = "TodoGraphQlService"
  }
  mutation("deleteTodo") {
    argument<Int>("id")
    returns<Boolean>()
  }
  subscription("todoEvents") {
    argument<Int>("id", nullable = true)
    returns<TodoDto>("Todo")
  }
}
```

每個 operation 函式都會在 SDL 對應到 `type Query` / `type Mutation` / `type Subscription` 底下的一個 field,並在 Controller 產生帶 `@QueryMapping` / `@MutationMapping` / `@SubscriptionMapping` 的函式。

## 宣告參數

### `argument` — 純量 / 物件參數

三種等價寫法:

```kotlin
argument<Int>("id")                                  // reified,graphQlType 由型別推導
argument("id", Int::class)                           // KClass
argument("filter", typeName, graphQlType = "TodoFilter")  // KotlinPoet TypeName
```

可選參數:

- `graphQlType` — SDL 上的型別名稱;省略時由 Kotlin 型別推導(見「型別對應」)。
- `nullable` — `false`(預設)產生 SDL `T!`;`true` 產生 `T`。
- `defaultValue` — SDL 預設值字面值,建議用 `GraphQlDefaultValue` 產生(見下)。

### `collectionArgument` — list 參數

```kotlin
collectionArgument<Int>("ids")                              // [Int!]!
collectionArgument<Int>("ids", nullable = true)             // [Int!]
collectionArgument<Int>("ids", itemNullable = true)         // [Int]!
collectionArgument<Int>("ids", nullable = true, itemNullable = true)  // [Int]
```

- `nullable` 控制「整個 list」可否為 null。
- `itemNullable` 控制「list 內元素」可否為 null。

同一個函式內參數名稱必須唯一,重複會以 `GraphQL argument 'X' is already declared` 失敗。

## 宣告回傳型別

每個 operation 函式 **必須** 宣告回傳型別,否則 codegen 會以 `GraphQL function X must declare a response type` 失敗。

```kotlin
returns<TodoDto>("Todo")                       // Todo!
returns<TodoDto>("Todo", nullable = true)      // Todo
returns<Boolean>()                             // Boolean!,graphQlType 由型別推導

returnsCollection<TodoDto>("Todo")                                  // [Todo!]!
returnsCollection<TodoDto>("Todo", nullable = true)                 // [Todo!]
returnsCollection<TodoDto>("Todo", nullable = true, itemNullable = true)  // [Todo]
```

`returns` / `returnsCollection` 同樣有 reified、`KClass`、`TypeName` 三種多載。Kotlin 端的回傳型別會與 nullability / collection 設定一致(例如 `Collection<TodoDto?>?`)。

## 型別定義:`type` / `input` / `enumType` / `scalar`

DSL 同時負責產生 SDL 的型別宣告,讓 schema 不必手寫。

### `type` — object type

```kotlin
type("Todo") {
  field<Int>("id")
  field<String>("description")
  collectionField<String>("tags")
  collectionField<String>("previousDescriptions", nullable = true, itemNullable = true)
}
```

### `input` — input type

```kotlin
input("TodoFilter") {
  field<String>("descriptionContains", nullable = true, defaultValue = GraphQlDefaultValue.string("open"))
  collectionField<Int>("ids", defaultValue = GraphQlDefaultValue.list())
}
```

`field` / `collectionField` 的 nullability 規則與參數相同。`defaultValue` **只允許用在 `input` 的欄位**;用在 `type` 上會以 `GraphQL field default values are only supported on input fields` 失敗。

### `enumType` — enum

兩種寫法:

```kotlin
enumType("TodoStatus") {           // 手動列出 enum value
  value("OPEN")
  value("DONE")
}

enumType<TodoStatus>()             // 由 Kotlin enum 自動推導所有 value,SDL 名稱用 simpleName
```

### `scalar` — 自訂 scalar

```kotlin
scalar("DateTime")
scalar<Long>()                     // SDL 名稱由型別推導,此例為 "Long"
```

`scalar` 只是在 SDL 寫出 `scalar X` 宣告。**它不會註冊 GraphQL Java 的 coercing**;`LocalDateTime`、`BigDecimal`、`UUID` 等真正的序列化邏輯仍需自行透過 `RuntimeWiringConfigurer` 或 `graphql-java-extended-scalars` 註冊。

`type` / `input` / `enum` 不可為空 — 沒有任何 field / value 會以 `must declare at least one field/value` 失敗。需要無欄位的型別請改用 `scalar(...)`。

## 描述(GraphQL description)

operation 函式、operation 參數、型別定義與 `type` / `input` 的個別 field 都可以加上 SDL description,會在 schema 產出物寫成 GraphQL 的 block string(`"""..."""`),於 introspection / GraphiQL 顯示。

```kotlin
schema("TodoGraphQl") {
  query("todo") {
    argument<Int>("id", description = "The todo id")
    returns<TodoDto>("Todo", nullable = true)
    description = "Find a single todo by id"
  }
  type("Todo") {
    description = "A todo item"
    field<Int>("id", description = "Unique identifier")
    collectionField<String>("tags", description = "Free-form labels")
  }
  scalar<Long>(description = "A 64-bit integer scalar")
}
```

- `query` / `mutation` / `subscription` 區塊用 `description = "..."` 屬性設定;會寫在對應 operation field 之前。
- `type` / `input` / `enumType` 區塊同樣用 `description = "..."` 屬性;會寫在型別宣告之前。
- `type` / `input` 的 `field` / `collectionField` 用具名參數 `description = "..."` 設定;會寫在該 field 之前(縮排兩格)。
- operation 的 `argument` / `collectionArgument` 用具名參數 `description = "..."` 設定。只要有任一參數帶 description,該 operation field 的參數會改用多行排版,每個參數各自一行並寫上 block string。
- `scalar(name)` / `scalar<T>()` 用具名參數 `description = "..."` 設定。
- description 預設為 `null`(不輸出)。設定後不可為空白字串,否則會以 `... description must not be blank` 失敗。
- 單行 description 產出 `"""text"""`;含換行(或以 `"` 結尾)時改用多行 block string,內容會逐行縮排。description 內的 `"""` 會自動跳脫成 `\"""`。

目前 description 支援 operation 函式、operation 參數、型別定義與 `type` / `input` field 層級;個別 enum value 的 description 尚未支援。

## 型別對應(`defaultGraphQlType`)

省略 `graphQlType` 時,Kotlin 型別會這樣推導成 SDL 型別:

| Kotlin | SDL |
|---|---|
| `Int` | `Int` |
| `Long` | `Long`(注意:非 GraphQL 內建 scalar,需自行 `scalar<Long>()` 宣告) |
| `String` | `String` |
| `Boolean` | `Boolean` |
| `Float` / `Double` | `Float` |
| 其他 | 該類別的 `simpleName` |

GraphQL 內建 scalar 只有 `Int` / `Float` / `String` / `Boolean` / `ID`。其他型別(含 `Long`)若被引用,務必在某個 schema 內以 `type` / `input` / `enumType` / `scalar` 宣告,否則 schema 在啟動期會解析失敗。

## 預設值:`GraphQlDefaultValue`

直接手寫 SDL 預設值字面值容易在跳脫字元上出錯。`GraphQlDefaultValue` 提供安全的 helper,產出的是 **SDL 字面值字串**:

```kotlin
GraphQlDefaultValue.string("open")                 // "open"(自動處理引號 / 控制字元跳脫)
GraphQlDefaultValue.int(1)                          // 1
GraphQlDefaultValue.long(2L)                        // 2
GraphQlDefaultValue.float(3.5f)                     // 3.5(拒絕 NaN / Infinity)
GraphQlDefaultValue.double(4.25)                    // 4.25
GraphQlDefaultValue.boolean(true)                   // true
GraphQlDefaultValue.enum(TodoStatus.OPEN)           // OPEN
GraphQlDefaultValue.nullValue()                     // null
GraphQlDefaultValue.list(
  GraphQlDefaultValue.int(1),
  GraphQlDefaultValue.nullValue(),
)                                                   // [1, null]
GraphQlDefaultValue.objectValue(
  "descriptionContains" to GraphQlDefaultValue.string("open"),
  "status" to GraphQlDefaultValue.enum(TodoStatus.OPEN),
)                                                   // { descriptionContains: "open", status: OPEN }
```

`enum(name)` 與 `objectValue` 的欄位名都會做 GraphQL name 驗證,非法名稱會即時失敗。

## 接線到 Service

### `serviceName` 與 `serviceFunctionName`

- `serviceName` — 覆寫此函式所屬的 Service interface 名稱;省略時用 `${apiName}Service`。同一個 schema 內的多個函式可指向同一個 `serviceName`,集中成一個介面。
- `serviceFunctionName` — 覆寫 Service interface 上的方法名稱;省略時與 Controller 函式同名。

```kotlin
query("todo") {
  argument<Int>("id")
  returns<TodoDto>("Todo", nullable = true)
  serviceName = "TodoGraphQlService"
}
```

### 產生的 Controller 如何取得 Service

產生的 Controller **不使用建構子注入**,而是透過 `zygarde.core.di.DiServiceContext.bean<T>()` 取得 Service:

```kotlin
@Controller
public class TodoGraphQlController {
  @QueryMapping(name = "todo")
  public fun todo(@Argument(name = "id") id: Int): TodoDto? {
    val service = bean<TodoGraphQlService>()
    return service.todo(id)
  }
}
```

因此你要手寫的只有 Service 實作,並讓它能被 `DiServiceContext` 解析(一般是標 `@Service` 的 Spring bean):

```kotlin
@Service
class TodoGraphQlServiceImpl(
  @Autowired private val todoDao: TodoDao,
) : TodoGraphQlService {
  override fun todo(id: Int): TodoDto? =
    todoDao.findById(id).orElse(null)?.let(TodoDtoBuilder::build)
}
```

## 執行 codegen

`GraphQlDslCodegenMain` 是進入點。它用 ClassGraph 掃描 classpath 上所有 **非 abstract** 的 `GraphQlDslCodegen` 子類別,實例化後執行,再把結果寫檔。

行為由 system property 控制:

| Property | 用途 | 預設 |
|---|---|---|
| `zygarde.codegen.dsl.graphql.controller.package` | 產生 Controller 的 package | `zygarde.generated.graphql` |
| `zygarde.codegen.dsl.graphql.service-interface.package` | 產生 Service interface 的 package | `zygarde.generated.graphql.service` |
| `zygarde.codegen.dsl.graphql.controller.write-to` | Controller 輸出目錄 | 未設定 → 印到 stdout |
| `zygarde.codegen.dsl.graphql.service-interface.write-to` | Service interface 輸出目錄 | 未設定 → 印到 stdout |
| `zygarde.codegen.dsl.graphql.schema.write-to` | `.graphqls` 輸出目錄 | 未設定 → 印到 stdout |

> 注意:每個 `write-to` 目錄在寫入前會被 **遞迴刪除再重建**,請指向專屬的產生碼目錄,不要與手寫碼混放。

`samples/todo-multimodule-dsl` 的做法是替 codegen 模組套用 `application` plugin,把 `mainClass` 設成 `zygarde.codegen.dsl.graphql.GraphQlDslCodegenMainKt`,並以 `applicationDefaultJvmArgs` 帶入上述 property,接著:

```bash
./gradlew :todo-codegen-dsl-graphql:run
```

產出會分別寫入 `todo-dsl-generated-graphql-controller`、`todo-dsl-generated-graphql-service-interface`、`todo-dsl-generated-graphql-schema` 三個專屬模組。

## 驗證規則一覽

DSL 與產生器都會把錯誤擋在「產出檔案之前」,讓問題 fail fast:

- **名稱合法性** — 所有 GraphQL 名稱必須符合 `[_A-Za-z][_0-9A-Za-z]*`,且不可用 `__` 開頭(GraphQL 保留給 introspection)。
- **唯一性** — `apiName`、operation field 名稱(同一 operation,跨所有 schema)、型別定義名稱(跨所有 schema)、函式參數名稱、型別欄位名稱、enum value 都必須唯一。
- **非空** — `type` / `input` 至少要有一個 field,`enum` 至少要有一個 value。
- **description** — operation 函式、operation 參數、型別定義與 `type` / `input` field 的 `description` 可省略;一旦設定就不可為空白字串。
- **預設值** — 只允許出現在 `input` 的欄位。
- **回傳型別** — 每個 operation 函式都必須宣告。
- **schema-only** — 一份 schema 只有型別定義、沒有任何 operation 時,只產生 SDL,不產生空的 Controller / Service interface,適合放共用的 scalar / 共用 type 片段。

### 同名 field 跨 operation root

GraphQL 允許 `query` 與 `mutation` 有同名 field(例如都叫 `todo`)。此時 SDL 維持原名,但 Kotlin 方法會自動加上 operation 前綴避免衝突:

```
query  todo  → fun queryTodo(...)
mutation todo → fun mutationTodo(...)
```

`@QueryMapping(name = "todo")` 等註解仍會帶上原始 GraphQL 名稱,所以 runtime 綁定不受 Kotlin 方法改名影響。

### 多份 schema 的 operation root

多份 schema 各自產生獨立的 `.graphqls` 檔。Spring for GraphQL 會把它們載入同一個 schema,因此產生器在第一次出現某個 root type 時寫 `type Query { }`,之後改寫 `extend type Query { }`,避免重複定義。

## 目前限制與後續

DSL 目前涵蓋 query / mutation / subscription、參數、型別定義、預設值與 operation / argument / 型別 / field 層級的 description。以下尚未由 DSL 支援,需要時請手寫 resolver:

- **enum value 的 description** — 目前 description 支援 operation 函式、operation 參數、型別定義與 `type` / `input` field;個別 enum value 尚未支援。

- **巢狀 field resolver / `@SchemaMapping` / `@BatchMapping`** — 解決 N+1 的 DataLoader / batch resolver 仍須手寫(可參考 `samples/todo-multimodule-dsl` 內手寫的 `BookGraphQlController`)。
- **subscription 回傳型別** — 產生器只輸出宣告的回傳型別。要串真正的 Spring GraphQL subscription,呼叫端需自行選用 reactive publisher 型別(例如以 `TypeName` 多載傳入 `Flux<T>`)。
- **自訂 scalar coercing、錯誤處理、認證注入、分頁形狀** — 仍屬手寫 / 後續設計範圍,詳見 `doc/graphql-support-investigation.md`。
- **由 model-mapping metadata 自動產生 SDL `type` / `input`** — 規劃中;目前型別定義需在 DSL 明確宣告。

## 相關檔案索引

- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDslCodegen.kt` — DSL 基底
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlSchema.kt` — `schema { }` 內可用的宣告
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlFunction.kt` — `argument` / `returns` 等
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlTypeDefinition.kt` — `type` / `input` / `enumType`
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDefaultValue.kt` — 預設值 helper
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDslCodegenMain.kt` — codegen 進入點
- `modules-web/zygarde-web-codegen/.../generator/GraphQlApiGenerator.kt` — 實際產碼器
- `samples/todo-multimodule-dsl/todo-codegen-dsl-graphql/.../TodoGraphQlCodegen.kt` — 可參考的 DSL 範例
