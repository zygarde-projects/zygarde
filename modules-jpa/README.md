### Usage

* setup build.gradle.kts

```build.gradle.kts

repositories {
  maven("https://nexus.puni.tw/repository/maven-releases/")
}

dependencies {
  implementation("zygarde:zygarde-jpa:$zygardeVersion")
  kapt("zygarde:zygarde-jpa-codegen:$zygardeVersion")
}
```

* write your entity

```
@ZyModel
@Entity
class Book(
  @Id
  var id: Long
  var name: String = ""
)
```

* run kapt

```
./gradlew kaptKotlin
```

* you will able to use generated BookDao with enhanced dsl style query

```
bookDao.search {
  name() eq "MyBook"
}
```

### Use Enhanced Repository

```build.gradle.kts
kapt {
  arguments {
    arg("zygarde.codegen.base.package", "my_packge.codegen")
    arg("zygarde.codegen.dao.inherit", "zygarde.data.jpa.dao.ZygardeEnhancedDao")
  }
}
```

```
@Configuration
@EnableJpaRepositories(
  basePackages = ["my_packge.codegen.dao"],
  repositoryBaseClass = ZygardeJpaRepository::class
)
class MyJpaConfig
```

####  then generated Dao now extends `ZygardeEnhancedDao`

* use extension function `remove`
```
bookDao.remove {
  name() eq "MyBook"
}
```
