package sync

import scala.util.{Failure, Try, Success}

class DefaultSyncPromiseFuture[T] extends SyncFuture[T] with SyncPromise[T] {
  def call(code: => T): Unit = {
    try {
      complete(Success(code))
    } catch {
      case err: Throwable =>
        complete(Failure(err))
    }
  }

  var pendingListeners: List[(Try[T] => Any)] = List.empty

  /** When this future is completed, either through an exception, or a value,
    * apply the provided function.
    *
    * If the future has already been completed,
    * this will either be applied immediately or be scheduled asynchronously.
    *
    * $multipleCallbacks
    * $callbackInContext
    */
  override def onComplete[U](func: (Try[T]) => U): Unit = {
    if (isCompleted) {
      func(value.get)
    } else {
      pendingListeners = func :: pendingListeners
    }
  }

  /** The value of this `Future`.
    *
    * If the future is not completed the returned value will be `None`.
    * If the future is completed the value will be `Some(Success(t))`
    * if it contains a valid result, or `Some(Failure(error))` if it contains
    * an exception.
    */
  var value: Option[Try[T]] = None

  /** Future containing the value of this promise.
    */
  override def future: SyncFuture[T] = {
    return this
  }

  /** Tries to complete the promise with either a value or the exception.
    *
    * $nonDeterministic
    *
    * @return    If the promise has already been completed returns `false`, or `true` otherwise.
    */
  override def tryComplete(result: Try[T]): Boolean = {
    if (isCompleted) {
      false
    } else {
      value = Some(result)

      pendingListeners.foreach(listener => listener(result))
      pendingListeners = List.empty
      true
    }
  }
}
