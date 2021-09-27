package zygarde.test.dao

import zygarde.data.jpa.dao.ZygardeEnhancedDao
import zygarde.test.entity.Book

interface TestBookDao : ZygardeEnhancedDao<Book, Long>
