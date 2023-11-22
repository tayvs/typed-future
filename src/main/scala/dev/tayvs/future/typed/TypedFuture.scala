package dev.tayvs.future.typed

import dev.tayvs.future.typed.TypedFuture.TypedFutureConstructor

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

class TypedFuture[T, +E <: Throwable : ClassTag] private(val fut: Future[T]) /*extends AnyVal*/ {

  def failed: Future[Throwable] =
    fut.failed.flatMap {
      case e: E => Future.successful(e)
      case t => Future.failed(t)
    }(ExecutionContext.parasitic)

  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = fut.foreach(f)

  def transform[S, E1 <: Throwable : ClassTag](s: T => S, f: E => E1)(implicit executor: ExecutionContext): TypedFuture[S, E1] =
    this.map(s).mapError(f)

  def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[S, E] = new TypedFuture(fut.map(f))

  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): TypedFuture[S, Throwable] =
    fut.flatMap(f).withExpectedError[Throwable]

  //  def flatten[S](implicit ev: T <:< Future[S]) = fut.flatten

  //  def filter(p: T => Boolean)(implicit executor: ExecutionContext) = fut.filter(p)

  //  def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext) = fut.collect(pf)

  def recover[U >: T](pf: PartialFunction[E, U])(implicit executor: ExecutionContext) =
    new TypedFuture[U, E](fut.recover { case e: E if pf.isDefinedAt(e) => pf(e) })

  //  def recoverWith[U >: T, E1 <: Throwable : ClassTag](pf: PartialFunction[E, dev.tayvs.future.typed.TypedFuture[U, E1]])(implicit executor: ExecutionContext) = fut.recoverWith(pf)

//  def zip[U](that: Future[U]) = fut.zip(that)

//  def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext) = fut.zipWith(that)(f)

//  def fallbackTo[U >: T](that: Future[U]) = fut.fallbackTo(that)

  def mapTo[S](implicit tag: ClassTag[S]): TypedFuture[S, E] = new TypedFuture(fut.mapTo[S])

  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext) = fut.andThen(pf)

  def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = fut.onComplete(f)

//  def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext) = fut.transform(f)
//
//  def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext) = fut.transformWith(f)

  def flatMap[T1, E1 <: Throwable : ClassTag](f: T => TypedFuture[T1, E1])(implicit executor: ExecutionContext): TypedFuture[T1, E1] =
    new TypedFuture[T1, E1](fut.flatMap(f(_)).withExpectedError[E1])

  def mapError[E1 <: Throwable : ClassTag](f: E => E1)(implicit executor: ExecutionContext): TypedFuture[T, E1] =
    new TypedFuture[T, E1](fut.recoverWith { case t: E => Future.failed(f(t)) })

}

object TypedFuture {

  implicit def typed2Vanilla[T, E <: Throwable](tf: TypedFuture[T, E]): Future[T] = tf.fut

  class Successful[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](v: T)(implicit ct: ClassTag[E]): TypedFuture[T, E] = new TypedFuture[T, E](Future.successful(v))
  }

  class Failed[T] extends AnyRef {
    def apply[E <: Throwable : ClassTag](e: E): TypedFuture[T, E] = new TypedFuture[T, E](Future.failed(e))
  }

  class Apply[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](f: Future[T])(implicit ct: ClassTag[E]): TypedFuture[T, E] = new TypedFuture[T, E](f)
  }

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable : ClassTag]: TypedFuture[T, E] = new TypedFuture[T, E](fut)
  }

  //  def apply[T, E <: Throwable: ClassTag](fut: Future[T]): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](fut)
  def apply[E <: Throwable /*: ClassTag*/ ] = new Apply[E]

  //  def successful[T, E <: Throwable: ClassTag](value: T): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.successful(value))
  def successful[E <: Throwable /*: ClassTag*/ ]: Successful[E] = new Successful[E]

  //  def failed[E <: Throwable: ClassTag, T](error: E): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.failed(error))
  def failed[T]: Failed[T] = new Failed[T]

  def fromEither[E <: Throwable : ClassTag, T](e: Either[E, T]): TypedFuture[T, E] = e match {
    case Left(err) => TypedFuture.failed[T](err)
    case Right(v) => TypedFuture.successful[E].apply(v)
  }

}