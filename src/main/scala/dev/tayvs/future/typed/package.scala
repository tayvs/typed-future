package dev.tayvs.future


import scala.concurrent.Future
import scala.reflect.ClassTag

package object typed {

  type PureFuture[T] = TypedFuture[T, Nothing]

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable : ClassTag]: TypedFuture[T, E] = new TypedFuture[T, E](fut)

    def toPure: PureFuture[T] = new TypedFuture[T, Nothing](fut)
  }

  implicit class TypedFutureConstructorFromEither[E <: Throwable, T](val fut: Future[Either[E, T]]) extends AnyVal {
    def toTyped(implicit ct: ClassTag[E]): TypedFuture[T, E] = TypedFuture.fromEitherF(fut)
  }

}
