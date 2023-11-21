import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

class TypedFuture[T, +E <: Throwable: ClassTag] private[TypedFuture] (fut: Future[T]) extends AnyVal {

   def failed: Future[Throwable] = fut.failed
  // TODO:
  //    fut.recoverWith { case e: E => TypedFuture.successful[E, Throwable](e) }(ExecutionContext.parasitic)

   def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = fut.foreach(f)

   def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext) = fut.transform(s, f)

   def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[S, E] = new TypedFuture(fut.map(f))

   def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext) = fut.flatMap(f)

   def flatten[S](implicit ev: T <:< Future[S]) = fut.flatten

   def filter(p: T => Boolean)(implicit executor: ExecutionContext) = fut.filter(p)

   def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext) = fut.collect(pf)

   def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext) = fut.recover(pf)

   def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext) = fut.recoverWith(pf)

   def zip[U](that: Future[U]) = fut.zip(that)

   def zipWith[U, R](that: Future[U])(f: (T, U) => R)(implicit executor: ExecutionContext) = fut.zipWith(that)(f)

   def fallbackTo[U >: T](that: Future[U]) = fut.fallbackTo(that)

   def mapTo[S](implicit tag: ClassTag[S]) = fut.mapTo

   def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext) = fut.andThen(pf)

   def onComplete[U](f: Try[T] => U)(implicit executor: ExecutionContext): Unit = fut.onComplete(f)

   def transform[S](f: Try[T] => Try[S])(implicit executor: ExecutionContext) = fut.transform(f)

   def transformWith[S](f: Try[T] => Future[S])(implicit executor: ExecutionContext) = fut.transformWith(f)

  def flatMap[T1, E1 <: Throwable](f: T => TypedFuture[T1, E1])(implicit executor: ExecutionContext): TypedFuture[T1, E1] =
    new TypedFuture[T1, E1](fut.flatMap(f))

  def mapError[E1 <: Throwable: ClassTag](f: E => E1)(implicit executor: ExecutionContext): TypedFuture[T, E1] =
    new TypedFuture[T, E1](fut.recoverWith { case t: E => Future.failed(f(t)) })

}

object TypedFuture {

  implicit def typed2Vanilla[T, E <: Throwable](tf: TypedFuture[T, E]): Future[T] = tf.fut

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



}