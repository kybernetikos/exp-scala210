package sync

import scala.util.Try

/**
 * Copyright (c) 2014 All Right Reserved, Improbable Worlds Ltd.
 * Date: 05/08/2014
 * Summary: 
 */
class SyncPromiseSystem {

  private var nextId = 0

  var outstandingPromises: Map[Int, SyncPromiseWriter[_]] = Map.empty

  def next[T](): (Int, SyncFuture[T]) = {
    val id = nextId
    nextId = nextId + 1
    val promise = new DefaultSyncPromiseFuture[T]()
    outstandingPromises += (id -> promise)
    (id, promise.future)
  }

  def complete[T](id: Int, completion: Try[T]): Boolean = {
    outstandingPromises.get(id) match {
      case Some(promise: SyncPromiseWriter[T]) =>
        promise.complete(completion)
        outstandingPromises -= id
        true
      case None =>
        false
    }
  }
}