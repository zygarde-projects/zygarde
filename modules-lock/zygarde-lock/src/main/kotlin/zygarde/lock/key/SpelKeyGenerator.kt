package zygarde.lock.key

import zygarde.lock.exception.EvaluationConvertException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import org.springframework.context.expression.AnnotatedElementKey
import org.springframework.context.expression.CachedExpressionEvaluator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import org.springframework.expression.Expression

open class SpelKeyGenerator(
  private val conversionService: ConversionService
) : CachedExpressionEvaluator(), KeyGenerator {
  protected val conditionCache = ConcurrentHashMap<ExpressionKey, Expression>()

  override fun resolveKeys(
    lockKeyPrefix: String,
    expression: String,
    obj: Any,
    method: Method,
    args: Array<Any?>
  ): List<String> {
    val expressionValue = evaluateExpression(expression, obj, method, args)
    val keys = convertResultToList(expressionValue)

    if (lockKeyPrefix.isEmpty()) {
      return keys
    }

    return keys.map { key -> "$lockKeyPrefix$key" }
  }

  private fun evaluateExpression(expression: String, obj: Any, method: Method, args: Array<Any?>): Any {
    val context = MethodBasedEvaluationContext(obj, method, args, parameterNameDiscoverer)
    context.setVariable("executionPath", "${method.declaringClass.canonicalName}.${method.name}")

    val evaluatedExpression = getExpression(conditionCache, AnnotatedElementKey(method, obj.javaClass), expression)
    return evaluatedExpression.getValue(context)
      ?: throw EvaluationConvertException("Expression evaluated in a null")
  }

  protected open fun convertResultToList(expressionValue: Any?): List<String> {
    if (expressionValue == null) {
      throw EvaluationConvertException("Expression evaluated in a null")
    }
    val list: List<Any?>? = when {
      expressionValue is Iterable<*> -> {
        val genericCollection = TypeDescriptor.collection(Collection::class.java, TypeDescriptor.valueOf(Any::class.java))
        toList(expressionValue, genericCollection)
      }
      expressionValue.javaClass.isArray -> {
        val genericArray = TypeDescriptor.array(TypeDescriptor.valueOf(Any::class.java))
        toList(expressionValue, genericArray!!)
      }
      else -> listOf(expressionValue.toString())
    }

    if (list.isNullOrEmpty()) {
      throw EvaluationConvertException("Expression evaluated in an empty list")
    }

    if (list.any { it == null }) {
      throw EvaluationConvertException("null keys are not supported: $list")
    }

    return list.map { it.toString() }
  }

  private fun toList(expressionValue: Any, from: TypeDescriptor): List<Any?>? {
    val listTypeDescriptor = TypeDescriptor.collection(List::class.java, TypeDescriptor.valueOf(Any::class.java))
    @Suppress("UNCHECKED_CAST")
    return conversionService.convert(expressionValue, from, listTypeDescriptor) as List<Any?>?
  }
}
