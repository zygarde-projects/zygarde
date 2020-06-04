
* An entity with annotation `@ZyModel` will generate Dao file named `{EntityName}Dao`

```
@ZyModel
@Entity
class SimpleBook(
  @Id
  var id: Long
)
```

will generate

```
interface SimpleBookDao : JpaRepository<SimpleBook, Long>, JpaSpecificationExecutor<SimpleBook>
```
