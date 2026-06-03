package zygarde.core.crud

interface CrudCreateHook<E, REQ> {
  fun beforeCreate(entity: E, req: REQ) {}

  fun afterCreate(saved: E, req: REQ) {}
}

interface CrudUpdateHook<E, ID, REQ> {
  fun beforeUpdate(entity: E, id: ID, req: REQ) {}

  fun afterUpdate(saved: E, id: ID, req: REQ) {}
}

interface CrudDeleteHook<E, ID> {
  fun beforeDelete(entity: E, id: ID) {}

  fun afterDelete(entity: E, id: ID) {}
}

interface CrudPageHook<REQ> {
  fun beforePage(req: REQ) {}

  fun afterPage(req: REQ) {}
}
