package sync

import scala.util.Try

/**
 * Copyright (c) 2014 All Right Reserved, Improbable Worlds Ltd.
 * Date: 05/08/2014
 * Summary: 
 */
class SyncPromiseSystem {

  private var nextId = 0

  var existingPromises: Map[Int, SyncPromise[_]] = Map.empty

  def next[T](): (Int, SyncFuture[T]) = {
    val id = nextId
    nextId = nextId + 1
    val promise = new DefaultSyncPromiseFuture[T]()
    existingPromises += (id -> promise)
    (id, promise.future)
  }

  def complete[T](id: Int, completion: Try[T]): Boolean = {
    existingPromises.get(id) match {
      case Some(promise: SyncPromise[T]) =>
        promise.complete(completion)
        existingPromises -= id
        true
      case None =>
        false
    }
  }
}
