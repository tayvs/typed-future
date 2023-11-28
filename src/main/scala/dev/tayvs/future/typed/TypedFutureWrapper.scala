package dev.tayvs.future.typed

import dev.tayvs.future.typed.TypedFutureWrapper.TypedFutureConstructor

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.parasitic
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class TypedFutureWrapper[T, E <: Throwable : ClassTag] private(val fut: Future[T]) /*extends AnyVal*/ {

  def failed: TypedFutureWrapper[E, Throwable] = TypedFutureWrapper(fut.failed.flatMap {
    case e: E => Future.successful(e)
    case t => Future.failed(t)
  }(parasitic))

  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = fut.foreach(f)

  def transform[S, E1 <: Throwable : ClassTag](s: T => S, f: E => E1)(implicit executor: ExecutionContext): TypedFutureWrapper[S, E1] =
    map(s).mapError(f)

  def transform[T1, E1 <: Throwable : ClassTag](f: Either[E, T] => Either[E1, T1])(implicit executor: ExecutionContext): TypedFutureWrapper[T1, E1] =
    fut.transformWith {
      case Success(value) => Future.fromTry(f(Right(value)).toTry)
      case Failure(exception: E) => Future.fromTry(f(Left(exception)).toTry)
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def transformWith[S, E1 <: Throwable : ClassTag](f: Either[E, T] => TypedFutureWrapper[S, E1])(implicit executor: ExecutionContext): TypedFutureWrapper[S, E1] =
    fut.transformWith {
      case Success(value) => f(Right(value)).toClassic
      case Failure(exception: E) => f(Left(exception)).toClassic
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFutureWrapper[S, E] = new TypedFutureWrapper(fut.map(f))

  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): TypedFutureWrapper[S, Throwable] =
    fut.flatMap(f).withExpectedError[Throwable]

  def flatten[S](implicit ev: T <:< TypedFutureWrapper[S, E]): TypedFutureWrapper[S, E] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap[S, E](e => e)
  }

  def filter(p: T => Boolean)(orError: E)(implicit executor: ExecutionContext): TypedFutureWrapper[T, E] =
    this.flatMap(v => if (p(v)) this else TypedFutureWrapper.failed[T](orError))

  def collect[S](pf: PartialFunction[T, S])(orElse: E)(implicit executor: ExecutionContext): TypedFutureWrapper[S, E] =
    this.flatMap(v => pf.lift.apply(v).map(TypedFutureWrapper.successful[E](_)).getOrElse(TypedFutureWrapper.failed[S](orElse)))

  def recover[U >: T](pf: PartialFunction[E, U])(implicit executor: ExecutionContext): TypedFutureWrapper[U, E] =
    new TypedFutureWrapper[U, E](fut.recover { case e: E if pf.isDefinedAt(e) => pf(e) })

  def recoverWith[U >: T, E1 >: E <: Throwable : ClassTag](pf: PartialFunction[E, TypedFutureWrapper[U, E1]])(implicit executor: ExecutionContext): TypedFutureWrapper[U, E1] =
    new TypedFutureWrapper[U, E1](fut.recoverWith { case e: E if pf.isDefinedAt(e) => pf(e) })

  def mapTo[S](implicit tag: ClassTag[S]): TypedFutureWrapper[S, E] = new TypedFutureWrapper(fut.mapTo[S])

  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): TypedFutureWrapper[T, E] = {
    fut.andThen(pf)
    this
  }

  def onComplete[U](f: Either[E, T] => U)(implicit executor: ExecutionContext): Unit =
    fut.onComplete {
      case Success(value) => f(Right(value))
      case Failure(exception: E) => f(Left(exception))
      case Failure(_) => ()
    }

  def flatMap[T1, E1 <: Throwable : ClassTag](f: T => TypedFutureWrapper[T1, E1])(implicit executor: ExecutionContext): TypedFutureWrapper[T1, E1] =
    new TypedFutureWrapper[T1, E1](fut.flatMap(f(_)).withExpectedError[E1])

  def zip[T1, E1 >: E <: Throwable](that: TypedFutureWrapper[T1, E1]): TypedFutureWrapper[(T, T1), E1] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap(f1 => that.map(f2 => (f1, f2)))
  }

  def zipWith[T1, E1 >: E <: Throwable, R](that: TypedFutureWrapper[T1, E1])(f: (T, T1) => R): TypedFutureWrapper[R, E1] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap(f1 => that.map(f2 => f(f1, f2)))
  }

  def mapError[E1 <: Throwable : ClassTag](f: E => E1)(implicit executor: ExecutionContext): TypedFutureWrapper[T, E1] =
    new TypedFutureWrapper[T, E1](fut.transform(
      identity,
      {
        case t: E => f(t)
        case t => t
      }
    ))

  def recoverUnexpectedError[T1 >: T, E1 >: E <: Throwable : ClassTag](f: PartialFunction[Throwable, Either[E, T]])(implicit executor: ExecutionContext): TypedFutureWrapper[T1, E1] =
    new TypedFutureWrapper(fut.recoverWith {
//      case e: E => Future.failed(e)
      case e if !e.isInstanceOf[E] && f.isDefinedAt(e) => Future.fromTry(f(e).toTry)
    })

  def toClassic: Future[T] = fut

  def toClassicSafe: Future[Either[E, T]] =
    fut.transform {
      case Success(value) => Success(Right(value))
      case Failure(exception: E) => Success(Left(exception))
      case Failure(exception) => Failure(exception)
    }(parasitic)
}

object TypedFutureWrapper {

  implicit def typed2Vanilla[T, E <: Throwable](tf: TypedFutureWrapper[T, E]): Future[T] = tf.fut

  class Successful[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](v: T)(implicit ct: ClassTag[E]): TypedFutureWrapper[T, E] = new TypedFutureWrapper[T, E](Future.successful(v))
  }

  class Failed[T] extends AnyRef {
    def apply[E <: Throwable : ClassTag](e: E): TypedFutureWrapper[T, E] = new TypedFutureWrapper[T, E](Future.failed(e))
  }

  class Apply[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](f: Future[T])(implicit ct: ClassTag[E]): TypedFutureWrapper[T, E] = new TypedFutureWrapper[T, E](f)
  }

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable : ClassTag]: TypedFutureWrapper[T, E] = new TypedFutureWrapper[T, E](fut)
  }

  //  def apply[T, E <: Throwable: ClassTag](fut: Future[T]): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](fut)
  def apply[E <: Throwable /*: ClassTag*/ ] = new Apply[E]

  //  def successful[T, E <: Throwable: ClassTag](value: T): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.successful(value))
  def successful[E <: Throwable /*: ClassTag*/ ]: Successful[E] = new Successful[E]

  //  def failed[E <: Throwable: ClassTag, T](error: E): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.failed(error))
  def failed[T]: Failed[T] = new Failed[T]

  def fromEither[E <: Throwable : ClassTag, T](e: Either[E, T]): TypedFutureWrapper[T, E] = e match {
    case Left(err) => TypedFutureWrapper.failed[T](err)
    case Right(v) => TypedFutureWrapper.successful[E].apply(v)
  }

}