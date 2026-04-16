# Sealed Interface Codegen 使用教學

這份說明整理了 Model Mapping DSL 中 `sealedInterface()` 功能的使用方式，適合需要產生多型 API 回應型別（polymorphic response）的場景。

## 這個功能解決什麼問題

當 API 回應需要根據不同情境回傳不同結構的資料時，常見做法是：

```kotlin
data class PaymentResultDto(
  val type: String,
  val successData: SuccessData?,
  val failureData: FailureData?,
)
```

這樣的問題是：

- 每種 type 都有一堆不相關的 nullable 欄位
- 前端要靠 `type` 字串判斷哪些欄位有值，容易出錯
- OpenAPI schema 無法精確表達各 subtype 的結構
- 沒有型別安全保證，容易漏填或填錯欄位

`sealedInterface()` 的目標是讓你在 DSL 中宣告一次，自動產生：

```kotlin
sealed interface PaymentResult  // 帶 @JsonTypeInfo + @JsonSubTypes + @Schema

data class SuccessDto(...) : PaymentResult
data class FailureDto(...) : PaymentResult
```

## 核心概念

### 1. `sealedInterface()` DSL

在 `ModelMappingCodegenSpec` 中呼叫 `sealedInterface()` 定義多型介面：

```kotlin
sealedInterface("PaymentResult") {
  discriminatorProperty = "type"  // 預設就是 "type"，可省略
  subtype("success", Dtos.SuccessDto)
  subtype("failure", Dtos.FailureDto)
}
```

### 2. `SealedInterfaceSpec`

`SealedInterfaceSpec` 是建立 sealed interface 的 DSL builder：

- `name` — 產生的 sealed interface 名稱
- `discriminatorProperty` — JSON 判別欄位名稱（預設 `"type"`）
- `subtype(discriminatorValue, dto)` — 註冊一個 subtype 對應

### 3. 產生的 annotations

每個 sealed interface 會自動帶上三個 annotation：

- `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")` — Jackson 多型反序列化
- `@JsonSubTypes(...)` — 列出所有 subtype 的 class 與 discriminator 值
- `@Schema(oneOf = [...], discriminatorProperty = "type")` — OpenAPI 文件描述

### 4. Subtype 自動處理

- **有欄位定義的 DTO**（在 DSL 中有 `from`/`applyTo` 等 mapping）→ 產生 `data class`，自動加上 sealed interface 作為 superinterface
- **沒有欄位定義的 DTO** → 產生 `data object`，帶 `@Schema` 和 `Serializable`

## 一般使用流程

### Step 1. 定義 subtype DTO 的欄位

```kotlin
class PaymentDsl : ModelMappingCodegenSpec({
  Dtos.SuccessDto {
    from(Payment::transactionId)
    from(Payment::amount)
  }

  Dtos.FailureDto {
    from(Payment::errorCode)
    from(Payment::errorMessage)
  }
})
```

### Step 2. 宣告 sealed interface

在同一個或另一個 `ModelMappingCodegenSpec` 中：

```kotlin
class PaymentDsl : ModelMappingCodegenSpec({
  Dtos.SuccessDto {
    from(Payment::transactionId)
    from(Payment::amount)
  }

  Dtos.FailureDto {
    from(Payment::errorCode)
    from(Payment::errorMessage)
  }

  sealedInterface("PaymentResult") {
    subtype("success", Dtos.SuccessDto)
    subtype("failure", Dtos.FailureDto)
  }
})
```

### Step 3. 執行 codegen 並檢查產出

產生的檔案結構：

```
PaymentResult.kt       → sealed interface（帶 Jackson + Schema annotations）
SuccessDto.kt          → data class SuccessDto(...) : PaymentResult
FailureDto.kt          → data class FailureDto(...) : PaymentResult
```

## Generated Code 長什麼樣

### Sealed Interface

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  value = [
    JsonSubTypes.Type(value = SuccessDto::class, name = "success"),
    JsonSubTypes.Type(value = FailureDto::class, name = "failure"),
  ]
)
@Schema(
  oneOf = [SuccessDto::class, FailureDto::class],
  discriminatorProperty = "type",
)
sealed interface PaymentResult
```

### 有欄位的 Subtype（data class）

```kotlin
@Schema
data class SuccessDto(
  val transactionId: String,
  val amount: BigDecimal,
) : Serializable, PaymentResult
```

### 無欄位的 Subtype（data object）

如果某個 DTO 沒有在 DSL 中定義任何欄位映射，會產生 `data object`：

```kotlin
@Schema
data object EmptyResultDto : Serializable, PaymentResult
```

這適合用來表示不帶額外資料的狀態，例如 `PendingDto`、`CancelledDto` 等。

## 進階用法

### 自訂 discriminator 欄位

```kotlin
sealedInterface("NotificationEvent") {
  discriminatorProperty = "kind"
  subtype("email", Dtos.EmailNotificationDto)
  subtype("sms", Dtos.SmsNotificationDto)
}
```

產生的 JSON 會用 `kind` 而非 `type` 作為判別欄位：

```json
{ "kind": "email", "to": "user@example.com", "subject": "Hello" }
```

### 一個 DTO 實作多個 sealed interface

同一個 DTO 可以同時是多個 sealed interface 的 subtype：

```kotlin
sealedInterface("ResultA") {
  subtype("shared", Dtos.SharedDto)
}

sealedInterface("ResultB") {
  subtype("shared", Dtos.SharedDto)
}
```

產生的 code：

```kotlin
data object SharedDto : Serializable, ResultA, ResultB
```

注意：即使 DTO 出現在多個 sealed interface 中，也只會產生一個檔案，不會重複。

## 型別安全保證

### 可保證的情況

Jackson 序列化/反序列化時：

- `@JsonTypeInfo` 確保 JSON 一定帶 discriminator 欄位
- `@JsonSubTypes` 確保只有已註冊的 subtype 能被反序列化
- Kotlin `sealed interface` 確保 `when` 表達式能窮舉所有分支

OpenAPI 文件：

- `@Schema(oneOf = [...])` 讓 Swagger UI 能正確顯示各 subtype 的 schema
- `discriminatorProperty` 讓前端 codegen 工具能自動產生對應的型別

### 使用端的 when 表達式

```kotlin
fun handleResult(result: PaymentResult): String = when (result) {
  is SuccessDto -> "paid: ${result.transactionId}"
  is FailureDto -> "failed: ${result.errorCode}"
  // 編譯器保證窮舉，新增 subtype 時會強制更新
}
```

## 最推薦的實作範例

```kotlin
enum class Dtos : CodegenDtoSimple {
  SuccessDto,
  FailureDto,
}

class PaymentDsl : ModelMappingCodegenSpec({
  Dtos.SuccessDto {
    from(Payment::transactionId)
    from(Payment::amount)
  }

  Dtos.FailureDto {
    from(Payment::errorCode)
    from(Payment::errorMessage)
  }

  sealedInterface("PaymentResult") {
    subtype("success", Dtos.SuccessDto)
    subtype("failure", Dtos.FailureDto)
  }
})
```

API 端使用：

```kotlin
@GetMapping("/payments/{id}/result")
fun getPaymentResult(@PathVariable id: Long): PaymentResult {
  val payment = paymentService.findById(id)
  return when {
    payment.isSuccess -> payment.toSuccessDto()
    else -> payment.toFailureDto()
  }
}
```

## 總結

`sealedInterface()` 這個 DSL 功能的核心價值是：

- 在 Model Mapping DSL 中宣告一次，自動產生完整的多型型別結構
- 自動加上 `@JsonTypeInfo`、`@JsonSubTypes`、`@Schema` annotations
- 有欄位的 subtype 產生 `data class`，無欄位的產生 `data object`
- 一個 DTO 可以同時實作多個 sealed interface
- 用 Kotlin `sealed interface` 保證 `when` 表達式窮舉

如果你的 API 有類似：

- 多型回應（不同 type 回傳不同結構）
- 事件系統（不同 event kind 有不同 payload）
- 通知類型（email / sms / push 各有不同欄位）

這套模式都可以直接套用。
