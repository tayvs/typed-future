import scala.concurrent.ExecutionContext.parasitic
import scala.concurrent.{BatchingExecutor, CanAwait, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

//trait FutureDecorator[T] extends Future[T] {
//
//  /* Callbacks */
//
//  /**
//    * When this future is completed, either through an exception, or a value,
//    *  apply the provided function.
//    *
//    *  If the future has already been completed,
//    *  this will either be applied immediately or be scheduled asynchronously.
//    *
//    *  Note that the returned value of `f` will be discarded.
//    *
//    *  $swallowsExceptions
//    *  $multipleCallbacks
//    *  $callbackInContext
//    *
//    * @tparam U    only used to accept any return type of the given callback function
//    * @param f     the function to be executed when this `Future` completes
//    * @group Callbacks
//    */
//  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit
//
//  /* Miscellaneous */
//
//  /**
//    * Returns whether the future had already been completed with
//    *  a value or an exception.
//    *
//    *  $nonDeterministic
//    *
//    *  @return    `true` if the future was completed, `false` otherwise
//    * @group Polling
//    */
//  def isCompleted: Boolean
//
//  /**
//    * The current value of this `Future`.
//    *
//    *  $nonDeterministic
//    *
//    *  If the future was not completed the returned value will be `None`.
//    *  If the future was completed the value will be `Some(Success(t))`
//    *  if it contained a valid result, or `Some(Failure(error))` if it contained
//    *  an exception.
//    *
//    * @return    `None` if the `Future` wasn't completed, `Some` if it was.
//    * @group Polling
//    */
//  def value: Option[Try[T]]
//
//  /* Projections */
//
//  /**
//    * The returned `Future` will be successfully completed with the `Throwable` of the original `Future`
//    *  if the original `Future` fails.
//    *
//    *  If the original `Future` is successful, the returned `Future` is failed with a `NoSuchElementException`.
//    *
//    *  $caughtThrowables
//    *
//    * @return a failed projection of this `Future`.
//    * @group Transformations
//    */
//  def failed: Future[Throwable] = transform(Future.failedFun)(parasitic)
//
//  /* Monadic operations */
//
//  /**
//    * Asynchronously processes the value in the future once the value becomes available.
//    *
//    *  WARNING: Will not be called if this future is never completed or if it is completed with a failure.
//    *
//    *  $swallowsExceptions
//    *
//    * @tparam U     only used to accept any return type of the given callback function
//    * @param f      the function which will be executed if this `Future` completes with a result,
//    *               the return value of `f` will be discarded.
//    * @group Callbacks
//    */
//  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = onComplete { _ foreach f }
//
//  /**
//    * Creates a new future by applying the 's' function to the successful result of
//    *  this future, or the 'f' function to the failed result. If there is any non-fatal
//    *  exception thrown when 's' or 'f' is applied, that exception will be propagated
//    *  to the resulting future.
//    *
//    * @tparam S  the type of the returned `Future`
//    * @param  s  function that transforms a successful result of the receiver into a successful result of the returned future
//    * @param  f  function that transforms a failure of the receiver into a failure of the returned future
//    * @return    a `Future` that will be completed with the transformed value
//    * @group Transformations
//    */
//  def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): Future[S] =
//    transform { t =>
//      if (t.isInstanceOf[Success[T]]) t map s
//      else throw f(t.asInstanceOf[Failure[T]].exception) // will throw fatal errors!
//    }
//
//  /**
//    * Creates a new Future by applying the specified function to the result
//    * of this Future. If there is any non-fatal exception thrown when 'f'
//    * is applied then that exception will be propagated to the resulting future.
//    *
//    * @tparam S  the type of the returned `Future`
//    * @param  f  function that transforms the result of this future
//    * @return    a `Future` that will be completed with the transformed value
//    * @group Transformations
//    */
//  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): Future[S]
//
//  /**
//    * Creates a new Future by applying the specified function, which produces a Future, to the result
//    * of this Future. If there is any non-fatal exception thrown when 'f'
//    * is applied then that exception will be propagated to the resulting future.
//    *
//    * @tparam S  the type of the returned `Future`
//    * @param  f  function that transforms the result of this future
//    * @return    a `Future` that will be completed with the transformed value
//    * @group Transformations
//    */
//  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): Future[S]
//
//  /**
//    * Creates a new future by applying a function to the successful result of
//    *  this future. If this future is completed with an exception then the new
//    *  future will also contain this exception.
//    *
//    *  Example:
//    *
//    *  {{{
//    *  val f = Future { "The future" }
//    *  val g = f map { x: String => x + " is now!" }
//    *  }}}
//    *
//    *  Note that a for comprehension involving a `Future`
//    *  may expand to include a call to `map` and or `flatMap`
//    *  and `withFilter`.  See [[scala.concurrent.Future#flatMap]] for an example of such a comprehension.
//    *
//    * @tparam S  the type of the returned `Future`
//    * @param f   the function which will be applied to the successful result of this `Future`
//    * @return    a `Future` which will be completed with the result of the application of the function
//    * @group Transformations
//    */
//  def map[S](f: T => S)(implicit executor: ExecutionContext): Future[S] = transform(_ map f)
//
//  /**
//    * Creates a new future by applying a function to the successful result of
//    *  this future, and returns the result of the function as the new future.
//    *  If this future is completed with an exception then the new future will
//    *  also contain this exception.
//    *
//    *  $forComprehensionExamples
//    *
//    * @tparam S  the type of the returned `Future`
//    * @param f   the function which will be applied to the successful result of this `Future`
//    * @return    a `Future` which will be completed with the result of the application of the function
//    * @group Transformations
//    */
//  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S] = transformWith { t =>
//    if (t.isInstanceOf[Success[T]]) f(t.asInstanceOf[Success[T]].value)
//    else this.asInstanceOf[Future[S]] // Safe cast
//  }
//
//  /**
//    * Creates a new future with one level of nesting flattened, this method is equivalent
//    *  to `flatMap(identity)`.
//    *
//    * @tparam S  the type of the returned `Future`
//    * @group Transformations
//    */
//  def flatten[S](implicit ev: T <:< Future[S]): Future[S] = flatMap(ev)(parasitic)
//
//  /**
//    * Creates a new future by filtering the value of the current future with a predicate.
//    *
//    *  If the current future contains a value which satisfies the predicate, the new future will also hold that value.
//    *  Otherwise, the resulting future will fail with a `NoSuchElementException`.
//    *
//    *  If the current future fails, then the resulting future also fails.
//    *
//    *  Example:
//    *  {{{
//    *  val f = Future { 5 }
//    *  val g = f filter { _ % 2 == 1 }
//    *  val h = f filter { _ % 2 == 0 }
//    *  g foreach println // Eventually prints 5
//    *  Await.result(h, Duration.Zero) // throw a NoSuchElementException
//    *  }}}
//    *
//    * @param p   the predicate to apply to the successful result of this `Future`
//    * @return    a `Future` which will hold the successful result of this `Future` if it matches the predicate or a `NoSuchElementException`
//    * @group Transformations
//    */
//  def filter(p: T => Boolean)(implicit executor: ExecutionContext): Future[T] =
//    transform { t =>
//      if (t.isInstanceOf[Success[T]]) {
//        if (p(t.asInstanceOf[Success[T]].value)) t
//        else Future.filterFailure
//      } else t
//    }
//
//  /**
//    * Used by for-comprehensions.
//    * @group Transformations
//    */
//  final def withFilter(p: T => Boolean)(implicit executor: ExecutionContext): Future[T] = filter(p)(executor)
//
//  /**
//    * Creates a new future by mapping the value of the current future, if the given partial function is defined at that value.
//    *
//    *  If the current future contains a value for which the partial function is defined, the new future will also hold that value.
//    *  Otherwise, the resulting future will fail with a `NoSuchElementException`.
//    *
//    *  If the current future fails, then the resulting future also fails.
//    *
//    *  Example:
//    *  {{{
//    *  val f = Future { -5 }
//    *  val g = f collect {
//    *    case x if x < 0 => -x
//    *  }
//    *  val h = f collect {
//    *    case x if x > 0 => x * 2
//    *  }
//    *  g foreach println // Eventually prints 5
//    *  Await.result(h, Duration.Zero) // throw a NoSuchElementException
//    *  }}}
//    *
//    * @tparam S    the type of the returned `Future`
//    * @param pf    the `PartialFunction` to apply to the successful result of this `Future`
//    * @return      a `Future` holding the result of application of the `PartialFunction` or a `NoSuchElementException`
//    * @group Transformations
//    */
//  def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext): Future[S] =
//    transform { t =>
//      if (t.isInstanceOf[Success[T]])
//        Success(pf.applyOrElse(t.asInstanceOf[Success[T]].value, Future.collectFailed))
//      else t.asInstanceOf[Failure[S]]
//    }
//
//  /**
//    * Creates a new future that will handle any matching throwable that this
//    *  future might contain. If there is no match, or if this future contains
//    *  a valid result then the new future will contain the same.
//    *
//    *  Example:
//    *
//    *  {{{
//    *  Future (6 / 0) recover { case e: ArithmeticException => 0 } // result: 0
//    *  Future (6 / 0) recover { case e: NotFoundException   => 0 } // result: exception
//    *  Future (6 / 2) recover { case e: ArithmeticException => 0 } // result: 3
//    *  }}}
//    *
//    * @tparam U    the type of the returned `Future`
//    * @param pf    the `PartialFunction` to apply if this `Future` fails
//    * @return      a `Future` with the successful value of this `Future` or the result of the `PartialFunction`
//    * @group Transformations
//    */
//  override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): this.type [T] =
//    transform { _ recover pf }
//
//  /**
//    * Creates a new future that will handle any matching throwable that this
//    *  future might contain by assigning it a value of another future.
//    *
//    *  If there is no match, or if this future contains
//    *  a valid result then the new future will contain the same result.
//    *
//    *  Example:
//    *
//    *  {{{
//    *  val f = Future { Int.MaxValue }
//    *  Future (6 / 0) recoverWith { case e: ArithmeticException => f } // result: Int.MaxValue
//    *  }}}
//    *
//    * @tparam U    the type of the returned `Future`
//    * @param pf    the `PartialFunction` to apply if this `Future` fails
//    * @return      a `Future` with the successful value of this `Future` or the outcome of the `Future` returned by the `PartialFunction`
//    * @group Transformations
//    */
//  def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] =
//    transformWith { t =>
//      if (t.isInstanceOf[Failure[T]]) {
//        val result = pf.applyOrElse(t.asInstanceOf[Failure[T]].exception, Future.recoverWithFailed)
//        if (result ne Future.recoverWithFailedMarker) result
//        else this
//      } else this
//    }
//
//  /**
//    * Zips the values of `this` and `that` future, and creates
//    *  a new future holding the tuple of their results.
//    *
//    *  If either input future fails, the resulting future is failed with the same
//    *  throwable, without waiting for the other input future to complete.
//    *
//    *  If the application of `f` throws a non-fatal throwable, the resulting future
//    *  is failed with that throwable.
//    *
//    * @tparam U      the type of the other `Future`
//    * @param that    the other `Future`
//    * @return        a `Future` with the results of both futures or the failure of the first of them that failed
//    * @group Transformations
//    */
//  def zip[U](that: Future[U]): Future[(T, U)] =
//    zipWith(that)(Future.zipWithTuple2Fun)(parasitic)
//
//  /**
//    * Zips the values of `this` and `that` future using a function `f`,
//    *  and creates a new future holding the result.
//    *
//    *  If either input future fails, the resulting future is failed with the same
//    *  throwable, without waiting for the other input future to complete.
//    *
//    *  If the application of `f` throws a non-fatal throwable, the resulting future
//    *  is failed with that throwable.
//    *
//    * @tparam U      the type of the other `Future`
//    * @tparam R      the type of the resulting `Future`
//    * @param that    the other `Future`
//    * @param f       the function to apply to the results of `this` and `that`
//    * @return        a `Future` with the result of the application of `f` to the results of `this` and `that`
//    * @group Transformations
//    */
//  def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext): Future[R] = {
//    // This is typically overriden by the implementation in DefaultPromise, which provides
//    // symmetric fail-fast behavior regardless of which future fails first.
//    //
//    // TODO: remove this implementation and make Future#zipWith abstract
//    //  when we're next willing to make a binary incompatible change
//    flatMap(r1 => that.map(r2 => f(r1, r2)))(if (executor.isInstanceOf[BatchingExecutor]) executor else parasitic)
//  }
//
//  /**
//    * Creates a new future which holds the result of this future if it was completed successfully, or, if not,
//    *  the result of the `that` future if `that` is completed successfully.
//    *  If both futures are failed, the resulting future holds the throwable object of the first future.
//    *
//    *  Using this method will not cause concurrent programs to become nondeterministic.
//    *
//    *  Example:
//    *  {{{
//    *  val f = Future { throw new RuntimeException("failed") }
//    *  val g = Future { 5 }
//    *  val h = f fallbackTo g
//    *  h foreach println // Eventually prints 5
//    *  }}}
//    *
//    * @tparam U     the type of the other `Future` and the resulting `Future`
//    * @param that   the `Future` whose result we want to use if this `Future` fails.
//    * @return       a `Future` with the successful result of this or that `Future` or the failure of this `Future` if both fail
//    * @group Transformations
//    */
//  def fallbackTo[U >: T](that: Future[U]): Future[U] =
//    if (this eq that) this
//    else {
//      implicit val ec = parasitic
//      transformWith { t =>
//        if (t.isInstanceOf[Success[T]]) this
//        else that transform { tt => if (tt.isInstanceOf[Success[U]]) tt else t }
//      }
//    }
//
//  /**
//    * Creates a new `Future[S]` which is completed with this `Future`'s result if
//    *  that conforms to `S`'s erased type or a `ClassCastException` otherwise.
//    *
//    * @tparam S     the type of the returned `Future`
//    * @param tag    the `ClassTag` which will be used to cast the result of this `Future`
//    * @return       a `Future` holding the casted result of this `Future` or a `ClassCastException` otherwise
//    * @group Transformations
//    */
//  def mapTo[S](implicit tag: ClassTag[S]): Future[S] = {
//    implicit val ec = parasitic
//    val boxedClass = {
//      val c = tag.runtimeClass
//      if (c.isPrimitive) Future.toBoxed(c) else c
//    }
//    require(boxedClass ne null)
//    map(s => boxedClass.cast(s).asInstanceOf[S])
//  }
//
//  /**
//    * Applies the side-effecting function to the result of this future, and returns
//    *  a new future with the result of this future.
//    *
//    *  This method allows one to enforce that the callbacks are executed in a
//    *  specified order.
//    *
//    *  Note that if one of the chained `andThen` callbacks throws
//    *  an exception, that exception is not propagated to the subsequent `andThen`
//    *  callbacks. Instead, the subsequent `andThen` callbacks are given the original
//    *  value of this future.
//    *
//    *  The following example prints out `5`:
//    *
//    *  {{{
//    *  val f = Future { 5 }
//    *  f andThen {
//    *    case r => throw new RuntimeException("runtime exception")
//    *  } andThen {
//    *    case Failure(t) => println(t)
//    *    case Success(v) => println(v)
//    *  }
//    *  }}}
//    *
//    * $swallowsExceptions
//    *
//    * @tparam U     only used to accept any return type of the given `PartialFunction`
//    * @param pf     a `PartialFunction` which will be conditionally applied to the outcome of this `Future`
//    * @return       a `Future` which will be completed with the exact same outcome as this `Future` but after the `PartialFunction` has been executed.
//    * @group Callbacks
//    */
//  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): this.type =
//    transform { result =>
//      try pf.applyOrElse[Try[T], Any](result, Future.id[Try[T]])
//      catch { case t if NonFatal(t) => executor.reportFailure(t) }
//      // TODO: use `finally`?
//      result
//    }
//}

class TypedFuture[T, +E <: Throwable: ClassTag] private (fut: Future[T]) extends Future[T] {

  override def failed: Future[Throwable] = super.failed
  // TODO:
//    fut.recoverWith { case e: E => TypedFuture.successful[E, Throwable](e) }(ExecutionContext.parasitic)

  override def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = super.foreach(f)

  override def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext) = super.transform(s, f)

  override def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[S, E] = new TypedFuture(fut.map(f))

  override def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext) = super.flatMap(f)

  override def flatten[S](implicit ev: T <:< Future[S]) = super.flatten

  override def filter(p: T => Boolean)(implicit executor: ExecutionContext) = super.filter(p)

  override def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext) = super.collect(pf)

  override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext) = super.recover(pf)

  override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext) = super.recoverWith(pf)

  override def zip[U](that: Future[U]) = super.zip(that)

  override def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext) = super.zipWith(that)(f)

  override def fallbackTo[U >: T](that: Future[U]) = super.fallbackTo(that)

  override def mapTo[S](implicit tag: ClassTag[S]) = super.mapTo

  override def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext) = super.andThen(pf)

  override def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = fut.onComplete(f)

  override def isCompleted = fut.isCompleted

  override def value = fut.value

  override def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext) = fut.transform(f)

  override def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext) = fut.transformWith(f)

  override def ready(atMost: Duration)(implicit permit: CanAwait) = fut.ready(atMost)

  override def result(atMost: Duration)(implicit permit: CanAwait) = fut.result(atMost)

  def flatMap[T1, E1 <: Throwable](f: T => TypedFuture[T1, E1])(implicit executor: ExecutionContext): TypedFuture[T1, E1] =
    new TypedFuture[T1, E1](fut.flatMap(f))

  def mapError[E1 <: Throwable: ClassTag](f: E => E1)(implicit executor: ExecutionContext): TypedFuture[T, E1] =
    new TypedFuture[T, E1](fut.recoverWith { case t: E => Future.failed(f(t)) })

}

object TypedFuture {

  class Successful[E <: Throwable /*: ClassTag*/ ] extends Any {
    def apply[T](v: T)(implicit ct: ClassTag[E]): TypedFuture[T, E] = new TypedFuture[T, E](Future.successful(v))
  }

  class Failed[T] extends Any {
    def apply[E <: Throwable: ClassTag](e: E): TypedFuture[T, E] = new TypedFuture[T, E](Future.failed(e))
  }

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable: ClassTag]: TypedFuture[T, E] = new TypedFuture[T, E](fut)
  }

  def apply[T, E <: Throwable: ClassTag](fut: Future[T]): TypedFuture[T, E] = new TypedFuture[T, E](fut)

//  def successful[T, E <: Throwable: ClassTag](value: T): TypedFuture[T, E] = new TypedFuture[T, E](Future.successful(value))
  def successful[E <: Throwable: ClassTag]: Successful[E] = new Successful[E]
//  def failed[E <: Throwable: ClassTag, T](error: E): TypedFuture[T, E] = new TypedFuture[T, E](Future.failed(error))
  def failed[T]: Failed[T] = new Failed[T]

  def fromEither[E <: Throwable: ClassTag, T](e: Either[E, T]): TypedFuture[T, E] = e match {
    case Left(err)  => TypedFuture.failed[T](err)
    case Right(v) => TypedFuture.successful[E].apply(v)
  }

  {
    implicit val ec: ExecutionContext = ExecutionContext.parasitic

    case class MyError(t: Throwable) extends Throwable(t)
    case class YourError(t: Throwable) extends Throwable(t)

    val _: TypedFuture[Int, MyError] = TypedFuture.successful[MyError].apply(21)
    val _: TypedFuture[Int, MyError] = TypedFuture.failed[Int](MyError(new Exception("")))

    val r: TypedFuture[String, MyError] = Future
      .successful(12)
      .withExpectedError[IllegalArgumentException]
      .mapError(MyError(_))
      .map(_ + 1)
      .flatMap(i => TypedFuture.successful[MyError].apply(i.toString))
      .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))
      .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))
  }

}
