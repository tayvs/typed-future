package dev.tayvs.future


import scala.concurrent.Future
import scala.reflect.ClassTag

package object typed {

  type PureFuture[T] = TypedFutureWrapper[T, Nothing]

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable : ClassTag]: TypedFutureWrapper[T, E] = new TypedFutureWrapper[T, E](fut)

    def toPure: PureFuture[T] = new TypedFutureWrapper[T, Nothing](fut)
  }

  implicit class TypedFutureConstructorFromEither[E <: Throwable, T](val fut: Future[Either[E, T]]) extends AnyVal {
    def toTyped(implicit ct: ClassTag[E]): TypedFutureWrapper[T, E] = TypedFutureWrapper.fromEitherF(fut)
  }

}
