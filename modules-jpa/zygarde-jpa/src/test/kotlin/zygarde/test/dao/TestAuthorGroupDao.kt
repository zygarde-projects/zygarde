package zygarde.test.dao

import zygarde.data.jpa.dao.BaseDao
import zygarde.test.entity.AuthorGroup

interface TestAuthorGroupDao : BaseDao<AuthorGroup, Int>
