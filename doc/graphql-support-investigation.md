# Zygarde 加入 GraphQL 支援 — 可行性調查與建議

> 本文為調查報告，盤點 Zygarde 既有 model-mapping / web / jpa codegen 架構，評估加入 GraphQL 支援的可行性、整合切入點、技術難點與分階段路線圖。對應分支:`v3`(已遷移至 Spring Boot 3)。
>
> 撰寫日期:2026-05-17。本文僅為設計探討,尚未進入實作。

## 結論先講

**高度可行,Zygarde 的架構出奇地適合。** 關鍵在於三點:

1. `v3` 分支已遷移到 **Spring Boot 3.5.14** → 可直接使用官方 `spring-boot-starter-graphql`(Spring for GraphQL)。
2. Zygarde 已有成熟的 **codegen 基礎設施**(KAPT / KSP / DSL 三套入口 + KotlinPoet 產碼),GraphQL 只是「再多一個產出目標」,不需重造輪子。
3. 最有價值的綜效:**JPA search DSL(`EnhancedSearch`)幾乎是為 GraphQL 的 filter argument 量身打造的** — 巢狀欄位 `author().age()` 天生對應 GraphQL 巢狀查詢與 JPA join。

唯一真正的工程難題是 **N+1 problem**(詳見「技術難點」一節)。

## 現況盤點

Zygarde 目前所有產出都走同一條管線,GraphQL 可以原樣複用:

| 層 | 來源 metadata | 產出 | 產出工具 |
|---|---|---|---|
| model-mapping | `@ZyModel`/`@ApiProp` 或 `ModelMappingCodegenSpec` DSL | DTO data class、`toDto()`/`applyFromDto()` extension | KotlinPoet |
| jpa | `@ZyModel` entity | `BaseDao`、type-safe search DSL extension(`name()`、`author()`) | KotlinPoet |
| web | `@ZyApi`/`@GenApi` 或 `WebMvcDslCodegen` DSL | API interface、Controller、Service interface、Feign client | KotlinPoet(`WebMvcApiGenerator`) |

每一層都同時提供 **annotation(KAPT/KSP)** 與 **DSL** 兩種入口,並一律以 KotlinPoet 建構 AST 後寫檔,無字串樣板。

備註:目前 `modules-web/` 只有 `webmvc` 系列,**沒有 webflux 模組**。GraphQL 走 Spring MVC(blocking)即可,單純化執行模型。

### 關鍵類別索引

model-mapping(DSL codegen):

- `ModelMappingCodegenSpec` / `ModelMappingDslCodegen` — DSL 基底
- `ModelMappingSpec` — `from` / `fromAutoIntId` / `applyTo` / `field` 等 builder
- `DtoFieldMapping`(sealed)— 欄位映射中介模型
- `DtoFieldMappingCodeGenerator` — 以 KotlinPoet 產 DTO 與 extension
- `ValueProvider<E, T>` — 欄位值轉換介面

jpa:

- `EnhancedSearch<T>` / `EnhancedSearchImpl<T>` — 查詢 DSL 介面與實作
- `ConditionAction` / `StringConditionAction` / `ComparableConditionAction` — 運算子 action
- `SearchSpecBuilder.buildSpec()` — 把 DSL lambda 轉成 JPA `Specification<T>`
- `ConditionActionImpl.columnNameToPath()` — 點分路徑累積 + 惰性 LEFT JOIN 解析(`joinMap` / `fetchMap`)
- `ZygardeJpaProcessor` / `ZygardeEntityFieldGenerator` / `ZygardeJpaDaoGenerator` — KAPT 產碼

web:

- `WebMvcDslCodegen` / `DslApi` / `DslApiFunction` — DSL 基底與 builder
- `WebMvcApiGenerator` — 產 API interface / Feign / Controller / Service interface
- `ApiToGenerateVo` / `ApiFunctionToGenerateVo` — codegen 中介模型
- `ApiExceptionHandler`(`@ControllerAdvice`)、`PageDto<T>`、`ApiErrorResponse`

## GraphQL 的天然對應點

這是本次調查最重要的發現 — Zygarde 既有概念幾乎一對一對應 GraphQL 構件:

| Zygarde 既有 | GraphQL 對應 | 說明 |
|---|---|---|
| model-mapping DTO(`from`) | `type`(object type) | 直接從 DTO meta 產 SDL |
| Request DTO(`applyTo`) | `input` type | mutation 輸入 |
| `EnhancedSearch` 條件 DSL | query field 的 `filter` argument | **核心綜效** |
| 巢狀走訪 `author().age()` | 巢狀 field resolver + JPA join | `joinMap`/`fetchMap` 已有 join 解析 |
| `WebMvcDslCodegen` | `GraphQlDslCodegen`(平行新增) | 產 `@Controller` 而非 `@RestController` |
| `WebMvcApiGenerator` | `GraphQlControllerGenerator` | 產 `@QueryMapping`/`@MutationMapping` |
| `PageDto<T>` | Relay connection 或 offset page type | 分頁 |
| `fromRef`/`fromRefCollection` | 巢狀 `@SchemaMapping` resolver | 需要 DataLoader(N+1) |
| `ApiExceptionHandler`(`@ControllerAdvice`) | `DataFetcherExceptionResolver` | **不能複用**,需新寫 |
| `authenticationDetail` 注入 | `@ContextValue` / `Principal` | 機制不同,需新寫 |
| sealed interface codegen | GraphQL `interface` / `union` | 既有 sealed 支援可映射 |

## 建議架構

維持 Zygarde「runtime 模組 + codegen 模組」的既有切法,新增:

```
modules-web/
├── zygarde-graphql                  # runtime:自訂 scalar、connection 型別、
│                                    #         DataFetcherExceptionResolver、共用基底
├── zygarde-graphql-codegen-dsl      # GraphQlDslCodegen DSL(對標 WebMvcDslCodegen)
└── zygarde-graphql-codegen-ksp      # (選配)KSP/KAPT annotation 入口

modules-model-mapping/
└── (擴充) DTO meta → SDL type/input 產生器
```

模組會透過 `settings.gradle.kts` 的 `registerModules()` 自動納入,只要放上 `build.gradle.kts` 即可。

### 關鍵設計決策:schema-first vs code-first

建議走 **「Zygarde 同時產 SDL + resolver」** — 等於 code-first 的開發體驗、schema-first 的執行模型,與框架既有哲學一致。**不引入** ExpediaGroup `graphql-kotlin` 這類外部 annotation 框架,以免與 Zygarde 自有 codegen 哲學衝突、並維持「產生碼與手寫碼嚴格隔離」的原則。

DSL 形式對標既有 `WebMvcDslCodegen`,概念草圖:

```kotlin
class TodoGraphQlDsl : GraphQlDslCodegen({
  query("books") {
    filter<BookFilter>()       // → GraphQL input type,bridge 到 EnhancedSearch<Book>
    returnsList<BookDto>()
    service("BookGraphQlService.findBooks")
  }
  mutation("createBook") {
    input<CreateBookReq>()     // 對應 model-mapping 的 applyTo Request DTO
    returns<BookDto>()
    service("BookGraphQlService.createBook")
  }
})
```

產出對標 `WebMvcApiGenerator`:`@Controller` + `@QueryMapping`/`@MutationMapping` 函式、Service interface,以及 `.graphqls` SDL 檔。

## 技術難點與風險

1. **N+1 problem(最大難題)** — 巢狀 `fromRef` resolver 若逐筆查 DB 會爆量。兩條解法並存:
   - 用 Spring for GraphQL 的 `@BatchMapping` + `DataLoader`(需 codegen 產 batch loader);
   - 或在頂層 query 就用 search DSL 既有的 `fetchMap` 做 fetch join。
2. **錯誤處理要重寫** — `ApiExceptionHandler` 是 `@ControllerAdvice`,對 GraphQL 的 data fetcher 無效;需新做 `DataFetcherExceptionResolver`,把 `BusinessException` 轉成 GraphQL error(含 `extensions` 帶 code)。
3. **自訂 Scalar** — `LocalDateTime`、`LocalDate`、`BigDecimal`、`UUID` 等需註冊 scalar;可直接引 `graphql-java-extended-scalars`。
4. **filter input 表達力取捨** — `EnhancedSearch` 支援 AND/OR/巢狀,要決定 GraphQL input type 暴露多少;全暴露會讓 schema 過於肥大。建議先支援欄位等值 + 比較(gt/lt/between)+ keyword,巢狀 AND/OR 後續再加。
5. **認證注入機制不同** — web codegen 的 `authenticationDetail` 注入無法沿用,需改用 `@ContextValue` / `Principal`,並重新設計 DSL 上對應的 `auth` 宣告。
6. **分頁模型抉擇** — `PageDto<T>` 是 offset-based;GraphQL 慣例為 Relay cursor connection。建議先支援 offset(對應既有 `PagingAndSortingRequest`),cursor connection 列為後續選項。

## 建議分階段路線圖

| 階段 | 範圍 | 產出 |
|---|---|---|
| **P0 PoC** | 在 `samples/todo-multimodule-dsl` 手寫一支 GraphQL `@Controller`,直接用既有 DTO + `bookDao.search` | 驗證 Boot 3 starter 串通、確認開發手感、實測 N+1 行為 |
| **P1** | `zygarde-graphql` runtime + `GraphQlDslCodegen` DSL,從 DSL 產 query/mutation resolver,SDL 暫時手寫 | 可用的 codegen 管線 |
| **P2** | model-mapping DTO meta → 自動產 SDL `type`/`input` | 不用手寫 schema |
| **P3** | filter input type + bridge 到 `EnhancedSearch` | GraphQL 端取得 type-safe 過濾能力 |
| **P4** | DataLoader/batch 解 N+1、cursor 分頁、錯誤/認證整合 | production-ready |

## 建議下一步

先做 **P0 PoC**:不寫任何 codegen,只在 sample 專案加一支手寫 GraphQL controller,證明「既有 DTO + search DSL」串到 Spring for GraphQL 是順的,並親手感受 N+1 行為。這能用很小成本驗證最大假設,再決定是否投入 P1 的模組建置。
