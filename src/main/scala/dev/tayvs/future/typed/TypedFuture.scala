package dev.tayvs.future.typed

import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.Try

final class TypedFuture[E <: Throwable, T](typedFut: TypedFutureWrapper[T, E]) extends Future[T] {

  override def failed: TypedFuture[Throwable, E] = new TypedFuture[Throwable, E](TypedFutureWrapper(typedFut.fut.failed))

  override def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = typedFut.foreach(f)

  override def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): TypedFuture[Throwable, S] =
    new TypedFuture(typedFut.transform(s, f))

  override def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[E, S] = new TypedFuture(typedFut.map(f))

  override def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): TypedFuture[Throwable, S] = new TypedFuture(typedFut.flatMap(f))

  override def flatten[S](implicit ev: T <:< Future[S]): TypedFuture[Throwable, S] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.flatten))

  override def filter(p: T => Boolean)(implicit executor: ExecutionContext): TypedFuture[Throwable, T] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.filter(p)))

  override def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext): TypedFuture[Throwable, S] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.collect(pf)))

  override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): TypedFuture[E, U] =
    new TypedFuture(TypedFutureWrapper[E](typedFut.fut.recover(pf)))

  override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): TypedFuture[Throwable, U] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.recoverWith(pf)))

  override def zip[U](that: Future[U]): TypedFuture[Throwable, (T, U)] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.zip(that)))

  override def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext): TypedFuture[Throwable, R] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.zipWith(that)(f)))

  override def fallbackTo[U >: T](that: Future[U]): TypedFuture[Throwable, U] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.fallbackTo(that)))

  override def mapTo[S](implicit tag: ClassTag[S]): TypedFuture[E, S] = new TypedFuture(typedFut.mapTo[S])

  override def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): TypedFuture[E, T] = {
    typedFut.fut.andThen(pf)
    this
  }

  override def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = typedFut.fut.onComplete(f)

  override def isCompleted: Boolean = typedFut.fut.isCompleted

  override def value: Option[Try[T]] = typedFut.fut.value

  override def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext): TypedFuture[Throwable, S] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.transform(f)))

  override def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext): TypedFuture[Throwable, S] =
    new TypedFuture(TypedFutureWrapper[Throwable](typedFut.fut.transformWith(f)))

  override def ready(atMost: Duration)(implicit permit: CanAwait): TypedFuture[E, T] =
    new TypedFuture(TypedFutureWrapper(typedFut.fut.ready(atMost)))

  override def result(atMost: Duration)(implicit permit: CanAwait): T = typedFut.fut.result(atMost)
}
