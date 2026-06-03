package zygarde.codegen.generator.impl

import zygarde.codegen.StaticOptionApi
import zygarde.codegen.ZygardeApiGeneratorKaptOptions.API_STATIC_OPTION_PACKAGE
import zygarde.codegen.ZygardeStaticOptionApiGeneratorTargetFolder
import zygarde.codegen.extension.kotlinpoet.ElementExtensions.name
import zygarde.codegen.extension.kotlinpoet.kotlinTypeName
import zygarde.codegen.generator.AbstractZygardeGenerator
import zygarde.codegen.generator.StaticOptionApiGeneratorSupport
import zygarde.codegen.generator.StaticOptionApiMeta
import zygarde.data.option.OptionEnum
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

class ZygardeStaticOptionApiGenerator(
  processingEnv: ProcessingEnvironment,
  val targetFolderConfig: ZygardeStaticOptionApiGeneratorTargetFolder?,
) : AbstractZygardeGenerator(processingEnv) {
  private val optionPackage: String by lazy {
    packageName(processingEnv.options.getOrDefault(API_STATIC_OPTION_PACKAGE, "api.option"))
  }

  fun generateStaticOptionApi(elements: Collection<Element>) {
    val filtered = elements
      .filter { it.isTypeOf<OptionEnum>() }
      .sortedBy { it.name() }
    if (filtered.isEmpty()) {
      return
    }

    val getFolderToGenerate = fun(resolveFoldertarget: (ZygardeStaticOptionApiGeneratorTargetFolder) -> String?): File {
      val defaultKaptFolder = folderToGenerate()
      return targetFolderConfig
        ?.let { resolveFoldertarget(it) }
        ?.let { File(it) }
        ?: defaultKaptFolder
    }

    val metas = filtered.map { it.toStaticOptionApiMeta() }
    val generator = StaticOptionApiGeneratorSupport(optionPackage)

    generator.generateStaticOptionDto(metas)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateDtosTo))
    generator.generateStaticOptionApiInterface(metas)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateFeignApiInterfacesTo))
    generator.generateStaticOptionController(metas)
      .writeTo(getFolderToGenerate(ZygardeStaticOptionApiGeneratorTargetFolder::generateControllersTo))
  }

  private fun Element.toStaticOptionApiMeta(): StaticOptionApiMeta {
    val staticOptionApi = getAnnotation(StaticOptionApi::class.java)
    return StaticOptionApiMeta(
      enumName = name(),
      enumTypeName = asType().kotlinTypeName(false),
      comment = staticOptionApi.comment,
      key = staticOptionApi.key,
      path = staticOptionApi.path,
      sourceName = toString(),
    )
  }
}
