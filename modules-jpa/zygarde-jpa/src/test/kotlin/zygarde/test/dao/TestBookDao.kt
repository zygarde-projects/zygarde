package zygarde.test.dao

import zygarde.data.jpa.dao.BaseDao
import zygarde.test.entity.Book

interface TestBookDao : BaseDao<Book, Long>
