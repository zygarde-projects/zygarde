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
| `modules-model-mapping/zygarde-model-mapping-codegen-dsl` | 提供 `DtoMetaResolver` / `ModelMappingMetadata`,讓 `typeFrom` / `inputFrom` 從 model-mapping DTO 推導型別 |

要使用 DSL,在 codegen 模組加入相依即可:

```kotlin
dependencies {
  implementation(project(":zygarde-graphql-codegen-dsl"))
  // 使用 typeFrom / inputFrom 推導時,額外加入:
  implementation(project(":zygarde-model-mapping-codegen-dsl"))
  implementation(project(":<你的 model-mapping codegen 模組>"))
}
```

只用手動 `type { }` / `input { }` 時,只需要 `zygarde-graphql-codegen-dsl` 一個相依。

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

## 型別定義:`type` / `input` / `enumType` / `scalar` / `union`

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
  value("OPEN", description = "尚未完成")  // 個別 value 可加 description
  value("DONE")
}

enumType<TodoStatus>()             // 由 Kotlin enum 自動推導所有 value,SDL 名稱用 simpleName
```

`enumType<T>()` 由 Kotlin enum 推導時不會帶入 description;需要個別 value 的 description 時請改用手動 `value(name, description)` 寫法。

### `scalar` — 自訂 scalar

```kotlin
scalar("DateTime")
scalar<Long>()                     // SDL 名稱由型別推導,此例為 "Long"
```

`scalar` 只是在 SDL 寫出 `scalar X` 宣告。**它不會註冊 GraphQL Java 的 coercing**;`LocalDateTime`、`BigDecimal`、`UUID` 等真正的序列化邏輯仍需自行透過 `RuntimeWiringConfigurer` 或 `graphql-java-extended-scalars` 註冊。

### `union` — union type

```kotlin
union("SearchResult", "Book", "Author")                       // union SearchResult = Book | Author
union("SearchResult", "Book", "Author", description = "搜尋結果是書或作者")
union("Listed", listOf("Book", "Author"))                      // 成員型別也能用 Iterable 傳入
```

`union` 在 SDL 寫出 `union Name = A | B` 宣告。成員型別應是已宣告(或在其他 schema 片段宣告)的 object `type`。union 至少要有一個成員型別、成員不可重複,否則分別以 `GraphQL union 'X' must declare at least one member type` 與 `GraphQL union 'X' member type 'Y' is already declared` 失敗。產生器只輸出 SDL,**不會產生 union 的型別解析器(`TypeResolver`)**;runtime 仍需自行透過 `RuntimeWiringConfigurer` 註冊,才能在查詢回傳時判斷實際型別。union 與 `scalar` 一樣不產生對應的 Kotlin 型別。

`type` / `input` / `enum` 不可為空 — 沒有任何 field / value 會以 `must declare at least one field/value` 失敗。需要無欄位的型別請改用 `scalar(...)`;需要型別聯集請用 `union(...)`。

## 從 entity property 宣告 GraphQL projection

若 GraphQL API 想直接維持 Zygarde 的 entity-first 心智模型,也可以用 `type<T>()` / `input<T>()` 的 projection DSL 從 entity property 產生 GraphQL `type` / `input`,並同時註冊 Kotlin 型別對應的 GraphQL 名稱:

```kotlin
schema("TodoGraphQl") {
  type<TodoDto>("Todo") {
    fromAutoIntId(Todo::id)
    from(Todo::description)
  }

  input<CreateTodoReq>("TodoInput") {
    applyTo(Todo::description)
  }
  bindGraphQlType<UpdateTodoReq>("TodoInput") // 多個 Kotlin DTO 共用同一個 GraphQL input 時使用

  mutation("createTodo") {
    argument<CreateTodoReq>("input") // 自動解析成 TodoInput
    returns<TodoDto>()               // 自動解析成 Todo
  }
}
```

這個 DSL 是明確 allowlist:只有寫進 `from(...)` / `applyTo(...)` 的欄位會出現在 GraphQL schema,不會掃描 entity 全欄位。欄位名稱、nullability 與 `@Comment` / Hibernate `@Comment` 會從 entity property 帶入;`fromAutoIntId` / `fromAutoLongId` 會映射成 GraphQL 內建 `ID`。

projection DSL 只負責 GraphQL schema 與 operation type binding;實際 DTO class 仍由既有 model-mapping codegen 產生。若已經有 model-mapping DTO metadata,也可以繼續使用下方的 `typeFrom` / `inputFrom`。

使用 `argument<T>()` / `returns<T>()` 的自動型別解析時,請先宣告 `type<T>()` / `input<T>()` / `bindGraphQlType<T>()`,再宣告引用該型別的 operation。

projection DSL 也可以明確宣告 nested relation field。這只會產生 SDL field,不會產生 `@BatchMapping` / `@SchemaMapping`,resolver 仍由 application 手寫。若欄位是 model-mapping provider field,可改用下方的 `lazyProviders` 產生 provider-backed `@BatchMapping`:

```kotlin
schema("BookGraphQl") {
  bindGraphQlType<AuthorDto>("Author")
  bindGraphQlType<BookDto>("Book")

  type<AuthorDto>("Author") {
    fromAutoIntId(GraphQlAuthor::id)
    from(GraphQlAuthor::name)
    refCollection<BookDto>("books")
  }

  type<BookDto>("Book") {
    fromAutoIntId(GraphQlBook::id)
    from(GraphQlBook::title)
    ref<AuthorDto>("author", nullable = true)
  }
}
```

`ref<T>()` / `refCollection<T>()` 會用已註冊的 GraphQL 型別名稱;註冊來源包含 `type<T>()` / `input<T>()` / `bindGraphQlType<T>()`。若沒有 binding 會 fail fast,避免把 Kotlin class name 誤當 GraphQL type name。也可以直接傳明確名稱,例如 `ref("author", graphQlType = "Author", nullable = true)` 或 `refCollection("books", graphQlType = "Book")`。

## 從 model-mapping DTO 自動推導型別:`typeFrom` / `inputFrom`

手動 `type` / `input` 的問題是:同一個 DTO 的欄位形狀已經在 model-mapping 的 `ModelMappingCodegenSpec` 宣告過一次,GraphQL schema 又得照抄一次,兩邊容易漂移。

`typeFrom` / `inputFrom` 直接從 model-mapping metadata 推導 GraphQL 型別宣告:

```kotlin
schema("BookGraphQl") {
  mapScalar<LocalDate>("Date")   // 非內建 scalar 要先告訴推導器怎麼對應
  scalar("Date")

  typeFrom(BookDtos.BookDto, name = "Book")
  inputFrom(BookDtos.CreateBookReq, name = "CreateBookInput")
}
```

`typeFrom` 接受的是 model-mapping 的 `CodegenDto`(就是 `ModelMappingCodegenSpec` 裡宣告 DTO 用的那個物件),不是產生出來的 DTO class。

推導規則:

- 欄位名稱、nullability、collection 與 description 全部來自 model-mapping metadata;`@Comment` / model field 的 comment 會變成 SDL description。
- `fromAutoIntId` / `fromAutoLongId` 標記的 auto-id 欄位推導成 GraphQL 內建的 `ID`。
- 純量欄位依「型別對應」表推導;`LocalDate` 等非內建型別需先用 `mapScalar` 註冊,否則 fail fast。
- `fromRef` / `fromRefCollection` 的 DTO 參照欄位會 **遞迴推導** 被參照的 DTO 型別。
- enum 欄位會自動推導出對應的 GraphQL `enum` 宣告。
- 已經(手動或先前推導)宣告過的型別會被重用,不會重複輸出。

參數:

- `name` — GraphQL 型別名稱;省略時用 `CodegenDto` 的名稱。
- `description` — 型別層級 description。
- `exclude` — 要排除、不出現在推導型別裡的欄位名稱集合。

`mapScalar`:

- `mapScalar<LocalDate>("Date")` 或 `mapScalar(LocalDate::class, "Date")` 註冊「Kotlin 型別 → GraphQL scalar 名稱」對應,讓推導器知道怎麼處理非內建型別。它只負責對應,**不會** 自動寫出 `scalar` 宣告;SDL 上需要的 `scalar X` 仍請另外呼叫 `scalar(...)`。

無法推導的情況會 fail fast:

- 欄位型別既非內建純量、enum,也沒 `mapScalar` 註冊,且不是 DTO 參照 → `cannot map field ... to a GraphQL type`。
- `typeFrom` / `inputFrom` 的 DTO 不在 model-mapping metadata 內 → `... is not part of model-mapping metadata`。

需要更細的客製(改欄位名、加額外欄位)時,仍可改用手動 `type { }` / `input { }`。

### `lazyProviders` 與 generated batch resolver

`typeFrom` 可搭配 model-mapping 的 provider metadata,把 DTO 上由 `provide(...)` 宣告的欄位延後到 GraphQL field resolver 載入:

```kotlin
schema("TodoGraphQl") {
  type("File") {
    field<String>("id")
    field<String>("name")
  }

  typeFrom(TodoDtos.TodoDto, name = "Todo") {
    lazyProviders {
      provider("file") {
        graphQlType("File")
        nullable()
      }
    }
  }
}
```

啟用後,產生器會:

- 產生 `TodoGraphQlSource` 與 assembler,讓 query / mutation 回傳 source object,保留 provider key 欄位(例如 `fileId`)但不把 key 寫進 SDL。
- 在 controller 產生 Spring GraphQL `@BatchMapping(typeName = "Todo", field = "file")`。
- 於 batch resolver 內取得 provider bean、收集 distinct keys,並呼叫 `provider.load(keys, dataProviderContext)`。

`dataProviderContext` 會從可選的 `zygarde.data.provider.DataProviderContextResolver` bean 取得:

```kotlin
val dataProviderContext =
  DiServiceContext.ctx.getBeanProvider(DataProviderContextResolver::class.java).ifAvailable?.resolve()
    ?: DataProviderContext.EMPTY
```

沒有註冊 resolver bean 時,generated resolver 維持舊行為,使用 `DataProviderContext.EMPTY`。若應用需要 tenant、auth 或 request marker,可以自行註冊 request-scoped 或 thread-local backed 的 `DataProviderContextResolver`。

### 推導需要 model-mapping 在 classpath 上

`GraphQlDslCodegenMain` 在執行 GraphQL codegen 前,會先用 ClassGraph 掃描 classpath 上的 `ModelMappingDslCodegen` 子類別、收集所有 DTO 欄位 metadata。因此使用 `typeFrom` / `inputFrom` 時,**model-mapping 的 codegen 模組必須在 GraphQL codegen 的 classpath 上**。單元測試裡也可以直接設定 `GraphQlDslCodegen.modelMappingMetadata`。

## 描述(GraphQL description)

operation 函式、operation 參數、型別定義、`type` / `input` 的個別 field 與 `enum` 的個別 value 都可以加上 SDL description,會在 schema 產出物寫成 GraphQL 的 block string(`"""..."""`),於 introspection / GraphiQL 顯示。

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
- `enumType` 的 `value` 用具名參數 `description = "..."` 設定;會寫在該 value 之前(縮排兩格)。
- `scalar(name)` / `scalar<T>()` 用具名參數 `description = "..."` 設定。
- description 預設為 `null`(不輸出)。設定後不可為空白字串,否則會以 `... description must not be blank` 失敗。
- 單行 description 產出 `"""text"""`;含換行(或以 `"` 結尾)時改用多行 block string,內容會逐行縮排。description 內的 `"""` 會自動跳脫成 `\"""`。

description 支援已涵蓋 operation 函式、operation 參數、型別定義、`type` / `input` field 與 `enum` value 各層級。

## 棄用標記(GraphQL `@deprecated`)

operation field(`query` / `mutation` / `subscription`)、operation `argument` / `collectionArgument`、`type` / `input` 的 `field` / `collectionField`,以及 `enum` 的 `value` 都可以加上 `deprecationReason`,在 SDL 產出物寫成 GraphQL 內建的 `@deprecated` directive,於 introspection / GraphiQL 標示為已棄用。

```kotlin
schema("TodoGraphQl") {
  query("legacyTodos") {
    argument<Int>("limit", nullable = true, deprecationReason = "Use pagination")
    returnsCollection<TodoDto>("Todo")
    deprecationReason = "Use todos"
  }
  type("Todo") {
    field<Int>("id", deprecationReason = "Use uuid instead")
    field<String>("uuid")
  }
  input("TodoInput") {
    field<String>("legacyTag", nullable = true, deprecationReason = "Replaced by tags")
    field<String>("source", defaultValue = GraphQlDefaultValue.string("manual"), deprecationReason = "No longer tracked")
  }
  enumType("TodoStatus") {
    value("OPEN")
    value("ARCHIVED", deprecationReason = "Use DONE")
  }
}
```

產出的 SDL:

```graphql
type Query {
  legacyTodos(limit: Int @deprecated(reason: "Use pagination")): [Todo!]! @deprecated(reason: "Use todos")
}

type Todo {
  id: Int! @deprecated(reason: "Use uuid instead")
  uuid: String!
}

input TodoInput {
  legacyTag: String @deprecated(reason: "Replaced by tags")
  source: String! = "manual" @deprecated(reason: "No longer tracked")
}

enum TodoStatus {
  OPEN
  ARCHIVED @deprecated(reason: "Use DONE")
}
```

- operation field 的 `deprecationReason` 是 `DslGraphQlFunction` 的屬性;`field` / `collectionField` / `argument` / `collectionArgument` / `value` 則用具名參數設定。`@deprecated` 會寫在型別(與 `input` / argument 預設值)之後。
- `reason` 是一般 GraphQL string,引號與控制字元會自動跳脫,不需自行處理。
- `deprecationReason` 預設為 `null`(不輸出)。設定後不可為空白字串,否則會以 `... deprecation reason must not be blank` 失敗。
- **必填的 input 欄位與 operation argument 不可棄用**:GraphQL 規範禁止棄用「non-null 且無 `defaultValue`」的 input 欄位或 argument;這種情況會以 `GraphQL input 'X' field 'Y' cannot be deprecated because it is a required input field` 或 `GraphQL query field 'X' argument 'Y' cannot be deprecated because it is a required argument` 失敗。要棄用 input 欄位或 argument,請讓它 `nullable = true` 或帶 `defaultValue`。operation field、物件 `type` 的欄位與 `enum` value 則無此限制。
- `enumType<T>()` 由 Kotlin enum 推導值,無法帶 per-value 的 `deprecationReason`;要棄用個別 enum value 需改用手動 `value(name, deprecationReason = ...)`。

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

GraphQL 內建 scalar 只有 `Int` / `Float` / `String` / `Boolean` / `ID`。其他型別(含 `Long`)若被引用,務必在某個 schema 內以 `type` / `input` / `enumType` / `scalar` / `union` 宣告,否則 schema 在啟動期會解析失敗。

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
- **唯一性** — `apiName`、operation field 名稱(同一 operation,跨所有 schema)、型別定義名稱(跨所有 schema)、函式參數名稱、型別欄位名稱、enum value、union 成員型別都必須唯一。
- **非空** — `type` / `input` 至少要有一個 field,`enum` 至少要有一個 value,`union` 至少要有一個成員型別。
- **description** — operation 函式、operation 參數、型別定義與 `type` / `input` field 的 `description` 可省略;一旦設定就不可為空白字串。
- **deprecation** — operation field、operation argument、`type` / `input` field 與 `enum` value 的 `deprecationReason` 可省略;一旦設定就不可為空白字串,且不可用在 `input` 的必填欄位或必填 argument(non-null 且無預設值)。
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

DSL 目前涵蓋 query / mutation / subscription、參數、型別定義(`type` / `input` / `enumType` / `scalar` / `union`)、預設值、operation / argument / 型別 / field / enum value 層級的 description、operation field / argument / `type` / `input` field / `enum` value 的 `@deprecated` 標記,以及 `lazyProviders` provider-backed generated `@BatchMapping`。以下尚未由 DSL 支援,需要時請手寫 resolver:

- **`interface` 型別與 union / interface 的 `TypeResolver`** — DSL 可宣告 `union`,但 GraphQL `interface` 尚未支援;且兩者實際的型別解析都需自行以 `RuntimeWiringConfigurer` 註冊。
- **一般巢狀 field resolver / `@SchemaMapping` / `@BatchMapping`** — `lazyProviders` 可為 model-mapping provider field 產生 provider-backed `@BatchMapping`;一般 `ref` / `refCollection` relation、`@SchemaMapping`,以及 GraphQL Java `DataLoaderRegistry` 仍須手寫(可參考 `samples/todo-multimodule-dsl` 內手寫的 `BookGraphQlController`)。
- **subscription 回傳型別** — 產生器只輸出宣告的回傳型別。要串真正的 Spring GraphQL subscription,呼叫端需自行選用 reactive publisher 型別(例如以 `TypeName` 多載傳入 `Flux<T>`)。
- **自訂 scalar coercing、錯誤處理、認證注入、分頁形狀** — 仍屬手寫 / 後續設計範圍,詳見 `doc/graphql-support-investigation.md`。
- **由 model-mapping metadata 自動產生 SDL `type` / `input`** — 已由 `typeFrom` / `inputFrom` 支援(見上節)。尚未支援的:`interface` 推導、operation 回傳型別自動推導、推導時的欄位改名 / 補欄位(這些情況請改用手動 `type { }` / `input { }`)。

## 相關檔案索引

- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDslCodegen.kt` — DSL 基底
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlSchema.kt` — `schema { }` 內可用的宣告
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlFunction.kt` — `argument` / `returns` 等
- `modules-web/zygarde-graphql-codegen-dsl/.../DslGraphQlTypeDefinition.kt` — `type` / `input` / `enumType`
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDtoDeriver.kt` — `typeFrom` / `inputFrom` 的 DTO 推導邏輯
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlTypeMapper.kt` — Kotlin 型別 → GraphQL scalar 對應(`mapScalar`)
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDefaultValue.kt` — 預設值 helper
- `modules-web/zygarde-graphql-codegen-dsl/.../GraphQlDslCodegenMain.kt` — codegen 進入點
- `modules-model-mapping/zygarde-model-mapping-codegen-dsl/.../meta/DtoMetaResolver.kt` — DTO 欄位 metadata 解析(`typeFrom` 推導來源)
- `modules-web/zygarde-web-codegen/.../generator/GraphQlApiGenerator.kt` — 實際產碼器
- `samples/todo-multimodule-dsl/todo-codegen-dsl-graphql/.../TodoGraphQlCodegen.kt` — 可參考的 DSL 範例
