package sync

import scala.util.{ Try, Success, Failure }

/** Promise is an object which can be completed with a value or failed
  *  with an exception.
  *
  *  @define promiseCompletion
  *  If the promise has already been fulfilled, failed or has timed out,
  *  calling this method will throw an IllegalStateException.
  *
  *  @define allowedThrowables
  *  If the throwable used to fail this promise is an error, a control exception
  *  or an interrupted exception, it will be wrapped as a cause within an
  *  `ExecutionException` which will fail the promise.
  *
  *  @define nonDeterministic
  *  Note: Using this method may result in non-deterministic concurrent programs.
  */
trait SyncPromise[T] extends SyncPromiseWriter[T] {

  /** Future containing the value of this promise.
    */
  def future: SyncFuture[T]
}

object SyncPromise {

  /** Creates a promise object which can be completed with a value.
    *
    *  @tparam T       the type of the value in the promise
    *  @return         the newly created `Promise` object
    */
  def apply[T](): SyncPromise[T] = new DefaultSyncPromiseFuture[T]()

  /** Creates an already completed Promise with the specified exception.
    *
    *  @tparam T       the type of the value in the promise
    *  @return         the newly created `Promise` object
    */
  def failed[T](exception: Throwable): SyncPromise[T] = {
    val result = new DefaultSyncPromiseFuture[T]()
    result.failure(exception)
    result
  }

  /** Creates an already completed Promise with the specified result.
    *
    *  @tparam T       the type of the value in the promise
    *  @return         the newly created `Promise` object
    */
  def successful[T](value: T): SyncPromise[T] = {
    val result = new DefaultSyncPromiseFuture[T]()
    result.success(value)
    result
  }
}
