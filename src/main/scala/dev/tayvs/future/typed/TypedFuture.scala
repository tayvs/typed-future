package dev.tayvs.future.typed


import scala.reflect.classTag

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.parasitic
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

// TODO: classTag could be moved from constructor to methods that actually require classTag.
//  This will allow to extend AnyVAl and make implementation only compile time wrapper
class TypedFuture[+E <: Throwable : ClassTag, +T] private[typed](val fut: Future[T]) /*extends AnyVal*/ {

  def failed[E1 >: E <: Throwable : ClassTag]: TypedFuture[E1, Throwable] = new TypedFuture[E1, Throwable](fut.failed.flatMap {
    case e: E => Future.successful(e)
    case t => Future.failed(t)
  }(parasitic))

  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = fut.foreach(f)

  def transform[E1 <: Throwable : ClassTag, S](s: T => S, f: E => E1)(implicit executor: ExecutionContext): TypedFuture[E1, S] =
    map(s).mapError(f)

  def transform[T1, E1 <: Throwable : ClassTag](f: Either[E, T] => Either[E1, T1])(implicit executor: ExecutionContext): TypedFuture[E1, T1] =
    fut.transformWith {
      case Success(value) => Future.fromTry(f(Right(value)).toTry)
      case Failure(exception: E) => Future.fromTry(f(Left(exception)).toTry)
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def transformWith[S, E1 <: Throwable : ClassTag](f: Either[E, T] => TypedFuture[E1, S])(implicit executor: ExecutionContext): TypedFuture[E1, S] =
    fut.transformWith {
      case Success(value) => f(Right(value)).toClassic
      case Failure(exception: E) => f(Left(exception)).toClassic
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[E, S] = new TypedFuture(fut.map(f))

  // Breaks for-comprehension??
  //  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): TypedFutureWrapper[S, Throwable] =
  //    fut.flatMap(f).withExpectedError[Throwable]

  def flatMap[T1, E1 >: E <: Throwable : ClassTag](f: T => TypedFuture[E1, T1])(implicit executor: ExecutionContext): TypedFuture[E1, T1] =
    new TypedFuture[E1, T1](fut.flatMap(f(_).fut))

  def flatten[S, E1 >: E <: Throwable : ClassTag](implicit ev: T <:< TypedFuture[E1, S]): TypedFuture[E1, S] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap[S, E1](e => e)
  }

  def filter[E1 >: E <: Throwable : ClassTag](p: T => Boolean)(orError: E1)(implicit executor: ExecutionContext): TypedFuture[E1, T] =
    this.flatMap(v => if (p(v)) this else TypedFuture.failed[T](orError))

  def collect[S, E1 >: E <: Throwable : ClassTag](pf: PartialFunction[T, S])(orElse: E1)(implicit executor: ExecutionContext): TypedFuture[E1, S] =
    this.flatMap(v => pf.lift.apply(v).map(TypedFuture.successful[E](_)).getOrElse(TypedFuture.failed[S](orElse)))

  def recover[U >: T](pf: PartialFunction[E, U])(implicit executor: ExecutionContext): TypedFuture[E, U] =
    new TypedFuture[E, U](fut.recover { case e: E if pf.isDefinedAt(e) => pf(e) })

  def recover[U >: T](pf: E => U)(implicit executor: ExecutionContext): PureFuture[U] =
    new TypedFuture[Nothing, U](fut.recover { case e: E => pf(e) })

  def recoverWith[U >: T, E1 >: E <: Throwable : ClassTag](pf: PartialFunction[E, TypedFuture[E1, U]])(implicit executor: ExecutionContext): TypedFuture[E1, U] =
    new TypedFuture[E1, U](fut.recoverWith { case e: E if pf.isDefinedAt(e) => pf(e).fut })

  def mapTo[S](implicit tag: ClassTag[S]): TypedFuture[E, S] = new TypedFuture(fut.mapTo[S])

  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): TypedFuture[E, T] = {
    fut.andThen(pf)
    this
  }

  def onComplete[U](f: Either[E, T] => U)(implicit executor: ExecutionContext): Unit =
    fut.onComplete {
      case Success(value) => f(Right(value))
      case Failure(exception: E) => f(Left(exception))
      case Failure(_) => ()
    }

  def zip[T1, E1 >: E <: Throwable : ClassTag](that: TypedFuture[E1, T1]): TypedFuture[E1, (T, T1)] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap(f1 => that.map(f2 => (f1, f2)))
  }

  def zipWith[T1, E1 >: E <: Throwable : ClassTag, R](that: TypedFuture[E1, T1])(f: (T, T1) => R): TypedFuture[E1, R] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap(f1 => that.map(f2 => f(f1, f2)))
  }

  def mapError[E1 <: Throwable : ClassTag](f: E => E1)(implicit executor: ExecutionContext): TypedFuture[E1, T] =
    new TypedFuture[E1, T](fut.transform(
      identity,
      {
        case t: E => f(t)
        case t => t
      }
    ))

  def recoverUnexpectedError[T1 >: T, E1 >: E <: Throwable : ClassTag](f: PartialFunction[Throwable, Either[E1, T1]])(implicit executor: ExecutionContext): TypedFuture[E1, T1] =
    new TypedFuture[E1, T1](fut.recoverWith {
      //      case e: E => Future.failed(e)
      // !classTag[E].runtimeClass.isInstance(e) opposite to e:E
      case e if !classTag[E].runtimeClass.isInstance(e) && f.isDefinedAt(e) => Future.fromTry(f(e).toTry)
    })

  def toClassic: Future[T] = fut

  def toClassicSafe: Future[Either[E, T]] =
    fut.transform {
      case Success(value) => Success(Right(value))
      case Failure(exception: E) => Success(Left(exception))
      case Failure(exception) => Failure(exception)
    }(parasitic)
}

object TypedFuture {

  class Successful[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](v: T)(implicit ct: ClassTag[E]): TypedFuture[E, T] = new TypedFuture[E, T](Future.successful(v))
  }

  class Failed[T] extends AnyRef {
    def apply[E <: Throwable : ClassTag](e: E): TypedFuture[E, T] = new TypedFuture[E, T](Future.failed(e))
  }

  class Apply[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](f: Future[T])(implicit ct: ClassTag[E]): TypedFuture[E, T] = new TypedFuture[E, T](f)
  }

  class Try_[E <: Throwable] extends AnyRef {
    def apply[T](t: Try[T])(implicit ct: ClassTag[E]): TypedFuture[E, T] = new TypedFuture[E, T](Future.fromTry(t))
  }

  //  def apply[T, E <: Throwable: ClassTag](fut: Future[T]): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](fut)
  def apply[E <: Throwable /*: ClassTag*/ ] = new Apply[E]

  //  def successful[T, E <: Throwable: ClassTag](value: T): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.successful(value))
  def successful[E <: Throwable /*: ClassTag*/ ]: Successful[E] = new Successful[E]

  //  def failed[E <: Throwable: ClassTag, T](error: E): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.failed(error))
  def failed[T]: Failed[T] = new Failed[T]

  def fromEither[E <: Throwable : ClassTag, T](e: Either[E, T]): TypedFuture[E, T] = e match {
    case Left(err) => TypedFuture.failed[T](err)
    case Right(v) => TypedFuture.successful[E].apply(v)
  }

  def fromTry[E <: Throwable]: Try_[E] = new Try_[E]

  def fromEitherF[E <: Throwable : ClassTag, T](f: Future[Either[E, T]]): TypedFuture[E, T] =
    new TypedFuture[E, T](f.flatMap {
      case Left(err) => Future.failed[T](err)
      case Right(v) => Future.successful(v)
    }(ExecutionContext.parasitic))

  def fromPure[T](e: T): PureFuture[T] = new TypedFuture(Future.successful(e))

}