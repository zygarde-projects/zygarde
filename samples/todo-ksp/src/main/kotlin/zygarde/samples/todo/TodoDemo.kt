package zygarde.samples.todo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import zygarde.codegen.ZyModel
import zygarde.data.jpa.dao.search
import zygarde.data.jpa.entity.AutoIntIdEntity
import zygarde.samples.todo.generated.dao.TodoDao
import zygarde.samples.todo.generated.search.description
import javax.persistence.Entity

/**
 * Sample entity demonstrating KSP code generation.
 * The @ZyModel annotation triggers:
 * - Dao interface generation (TodoDao)
 * - Search extension function generation (description())
 */
@ZyModel
@Entity
class Todo(
  var description: String = "",
  var completed: Boolean = false,
  var priority: Int = 0
) : AutoIntIdEntity()

/**
 * Sample service using generated code.
 */
@Service
class TodoService(
  @Autowired val todoDao: TodoDao
) {
  fun createTodo(description: String, priority: Int = 0): Todo {
    return todoDao.save(Todo(description = description, priority = priority))
  }

  fun searchByDescription(description: String): List<Todo> {
    return todoDao.search {
      description() eq description
    }
  }

  fun findAll(): List<Todo> {
    return todoDao.findAll()
  }

  fun markCompleted(id: Int): Todo? {
    return todoDao.findById(id).orElse(null)?.apply {
      completed = true
      todoDao.save(this)
    }
  }
}
