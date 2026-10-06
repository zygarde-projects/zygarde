package zygarde.core.extension.reflect

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/*
 * Kotlin 2.3+ compilers emit property references for intersection overrides (e.g. `name` of an enum implementing an
 * interface that declares `val name: String`) whose metadata kotlin-reflect cannot resolve: returnType, javaField,
 * javaGetter, etc. throw [KotlinReflectionInternalError]. The same property obtained through the owner class's
 * [memberProperties] resolves fine, so these helpers fall back to it.
 */

/**
 * Returns this property if kotlin-reflect can resolve it, otherwise the property with the same name looked up
 * from the owner class's [memberProperties]. Returns this property unchanged if no fallback is available.
 */
fun KProperty<*>.resolvable(): KProperty<*> =
  try {
    returnType
    this
  } catch (e: KotlinReflectionInternalError) {
    val owner = (this as? CallableReference)?.owner as? KClass<*> ?: throw e
    owner.memberProperties.firstOrNull { it.name == name } ?: throw e
  }

/**
 * Same as [javaField], but tolerates properties kotlin-reflect cannot resolve (see [resolvable]); returns null when
 * the backing field still cannot be determined.
 */
fun KProperty<*>.javaFieldOrNull(): Field? =
  try {
    resolvable().javaField
  } catch (e: KotlinReflectionInternalError) {
    null
  }

/**
 * Same as [javaGetter], but tolerates properties kotlin-reflect cannot resolve (see [resolvable]); returns null when
 * the getter still cannot be determined.
 */
fun KProperty<*>.javaGetterOrNull(): Method? =
  try {
    resolvable().javaGetter
  } catch (e: KotlinReflectionInternalError) {
    null
  }
