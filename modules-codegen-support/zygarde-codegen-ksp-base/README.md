# zygarde-codegen-ksp-base

Shared KSP (Kotlin Symbol Processing) extension utilities for Zygarde code generators.

## Features

This module provides common extension functions used by all KSP-based code generators:

- **TypeName Extensions**: Converting Java types to Kotlin types, generic type handling
- **KSAnnotation Extensions**: Safe extraction of annotation argument values
- **KSClassDeclaration Extensions**: Helper methods for class processing
- **KSPropertyDeclaration Extensions**: Property inspection utilities

## Usage

This module is automatically included as a dependency when using any Zygarde KSP code generator:

- `zygarde-jpa-codegen-ksp`
- `zygarde-model-mapping-codegen-ksp`
- `zygarde-webmvc-codegen-ksp`

## Extension Functions

### TypeName Extensions
- `TypeName.kotlin()` - Converts Java types to Kotlin equivalents
- `ClassName.generic()` - Creates parameterized types
- `KClass<*>.generic()` - Creates parameterized types from KClass

### KSAnnotation Extensions
- `getArgumentValue()` - Get raw argument value
- `getArgumentValueAsString()` - Get string argument
- `getArgumentValueAsBoolean()` - Get boolean argument
- `getArgumentValueAsLong()` - Get long argument
- `getArgumentValueAsStringArray()` - Get string array argument
- `getArgumentValueAsAnnotationList()` - Get nested annotations
- `getArgumentValueAsType()` - Get KSType argument
- `getArgumentValueAsTypeName()` - Get TypeName argument
- `getArgumentValueAsEnumEntry()` - Get enum entry name

### KSClassDeclaration Extensions
- `name()` - Get simple name
- `fieldName()` - Get camelCase field name
- `getAllPropertiesIncludingSuper()` - Get all properties including inherited
- `implementsInterface()` - Check if class implements an interface
