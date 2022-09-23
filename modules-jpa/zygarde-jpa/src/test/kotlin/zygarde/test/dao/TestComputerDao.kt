package zygarde.test.dao

import zygarde.data.jpa.dao.ZygardeEnhancedDao
import zygarde.test.entity.Computer

interface TestComputerDao : ZygardeEnhancedDao<Computer, Long>
