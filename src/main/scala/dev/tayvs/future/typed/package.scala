package dev.tayvs.future


import scala.concurrent.Future
import scala.reflect.ClassTag

package object typed {

  type PureFuture[T] = TypedFuture[Nothing, T]

  implicit class TypedFutureConstructor[T](val fut: Future[T]) extends AnyVal {
    def withExpectedError[E <: Throwable : ClassTag]: TypedFuture[E, T] = new TypedFuture[E, T](fut)

    def toPure: PureFuture[T] = new TypedFuture[Nothing, T](fut)
  }

  implicit class TypedFutureConstructorFromEither[E <: Throwable, T](val fut: Future[Either[E, T]]) extends AnyVal {
    def toTyped(implicit ct: ClassTag[E]): TypedFuture[E, T] = TypedFuture.fromEitherF(fut)
  }

}
