package zygarde.codegen

class ZygardeKaptOptions {
  companion object {
    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    const val BASE_PACKAGE = "zygarde.codegen.base.package"
    const val DAO_SUFFIX = "zygarde.codegen.dao.suffix"
    const val DAO_PACKAGE = "zygarde.codegen.dao.package"
    const val DAO_ENHANCED_IMPL = "zygarde.codegen.dao.enhanced"
    const val DAO_COMBINE = "zygarde.codegen.dao.combine"
    const val ENTITY_PACKAGE_SEARCH = "zygarde.codegen.entity.search"
    const val API_STATIC_OPTION_PACKAGE = "zygarde.codegen.static.option.api.package"
    const val MODEL_META_GENERATE_PACKAGE = "zygarde.codegen.meta.package"
    const val MODEL_META_GENERATE_TARGET = "zygarde.codegen.meta.target.folder"
  }
}
