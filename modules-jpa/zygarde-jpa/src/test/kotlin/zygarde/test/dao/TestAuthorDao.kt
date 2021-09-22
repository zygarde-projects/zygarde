package zygarde.test.dao

import zygarde.data.jpa.dao.BaseDao
import zygarde.test.entity.Author

interface TestAuthorDao : BaseDao<Author, Int>
