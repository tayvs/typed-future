package dev.tayvs.future.typed


import scala.annotation.nowarn
import scala.collection.BuildFrom
import scala.reflect.classTag
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.parasitic
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class TypedFuture[E <: Throwable /* : ClassTag*/ , +T] private[typed](val fut: Future[T]) extends AnyVal {

  def failed /*[E1 >: E <: Throwable /* : ClassTag*/ ]*/ (implicit ct: ClassTag[E]): TypedFuture[E, Throwable] = new TypedFuture[E, Throwable](fut.failed.flatMap {
    case e: E => Future.successful(e)
    case t => Future.failed(t)
  }(parasitic))

  def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = fut.foreach(f)

  def transform[E1 <: Throwable /* : ClassTag*/ , S](s: T => S, f: E => E1)(implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, S] =
    map(s).mapError(f)

  def transform[T1, E1 <: Throwable /* : ClassTag*/ ](f: Either[E, T] => Either[E1, T1])(implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, T1] =
    fut.transformWith {
      case Success(value) => Future.fromTry(f(Right(value)).toTry)
      case Failure(exception: E) => Future.fromTry(f(Left(exception)).toTry)
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def transformWith[S, E1 <: Throwable /* : ClassTag*/ ](f: Either[E, T] => TypedFuture[E1, S])(implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, S] =
    fut.transformWith {
      case Success(value) => f(Right(value)).toClassic
      case Failure(exception: E) => f(Left(exception)).toClassic
      case Failure(exception) => Future.failed(exception)
    }.withExpectedError[E1]

  def map[S](f: T => S)(implicit executor: ExecutionContext): TypedFuture[E, S] = new TypedFuture(fut.map(f))

  // Breaks for-comprehension??
  //  def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): TypedFutureWrapper[S, Throwable] =
  //    fut.flatMap(f).withExpectedError[Throwable]

  def flatMap[T1, E1 >: E <: Throwable /* : ClassTag*/ ](f: T => TypedFuture[E1, T1])(implicit executor: ExecutionContext): TypedFuture[E1, T1] =
    new TypedFuture[E1, T1](fut.flatMap(f(_).fut))

  def flatten[S, E1 >: E <: Throwable : ClassTag](implicit ev: T <:< TypedFuture[E1, S]): TypedFuture[E1, S] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap[S, E1](e => e)
  }

  def filter[E1 >: E <: Throwable : ClassTag](p: T => Boolean)(orError: E1)(implicit executor: ExecutionContext): TypedFuture[E1, T] =
    this.flatMap(v => if (p(v)) this.upliftError[E1] else TypedFuture.failed[T](orError))

  def collect[S, E1 >: E <: Throwable : ClassTag](pf: PartialFunction[T, S])(orElse: E1)(implicit executor: ExecutionContext): TypedFuture[E1, S] =
    this.flatMap(v => pf.lift.apply(v).map(TypedFuture.successful[E1](_)).getOrElse(TypedFuture.failed[S](orElse)))

  def recover[U >: T](pf: PartialFunction[E, U])(implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E, U] =
    new TypedFuture[E, U](fut.recover { case e: E if pf.isDefinedAt(e) => pf(e) })

  def recover[U >: T](pf: E => U)(implicit executor: ExecutionContext, ct: ClassTag[E]): PureFuture[U] =
    new TypedFuture[Nothing, U](fut.recover { case e: E => pf(e) })

  def recoverWith[U >: T, E1 >: E <: Throwable /* : ClassTag*/ ](pf: PartialFunction[E, TypedFuture[E1, U]])
                                                                (implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, U] =
    new TypedFuture[E1, U](fut.recoverWith { case e: E if pf.isDefinedAt(e) => pf(e).fut })

  //TODO: Unsafe like filter? Should Some orElse approach be used here?
  def mapTo[S: ClassTag]: TypedFuture[E, S] = new TypedFuture(fut.mapTo[S])

  def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): TypedFuture[E, T] = {
    fut.andThen(pf)
    this
  }

  def onComplete[U](f: Either[E, T] => U)(implicit executor: ExecutionContext, ct: ClassTag[E]): Unit =
    fut.onComplete {
      case Success(value) => f(Right(value))
      case Failure(exception: E) => f(Left(exception))
      case Failure(_) => ()
    }

  def zip[T1, E1 >: E <: Throwable : ClassTag](that: TypedFuture[E1, T1]): TypedFuture[E1, (T, T1)] = {
    implicit val ec: ExecutionContext = parasitic
    flatMap(f1 => that.map(f2 => (f1, f2)))
  }

  def zipWith[T1, E1 >: E <: Throwable : ClassTag, R](that: TypedFuture[E1, T1])(f: (T, T1) => R)(implicit executor: ExecutionContext): TypedFuture[E1, R] =
    flatMap(f1 => that.map(f2 => f(f1, f2)))(parasitic)

  def mapError[E1 <: Throwable /* : ClassTag*/ ](f: E => E1)(implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, T] =
    new TypedFuture[E1, T](fut.transform(
      identity,
      {
        case t: E => f(t)
        case t => t
      }
    ))

  def recoverUnexpectedError[T1 >: T, E1 >: E <: Throwable /* : ClassTag*/ ](f: PartialFunction[Throwable, Either[E1, T1]])
                                                                            (implicit executor: ExecutionContext, ct: ClassTag[E]): TypedFuture[E1, T1] =
    new TypedFuture[E1, T1](fut.recoverWith {
      //      case e: E => Future.failed(e)
      // !classTag[E].runtimeClass.isInstance(e) opposite to e:E
      case e if !classTag[E].runtimeClass.isInstance(e) && f.isDefinedAt(e) => Future.fromTry(f(e).toTry)
    })

  def toClassic: Future[T] = fut

  def toClassicSafe(implicit ct: ClassTag[E]): Future[Either[E, T]] =
    fut.transform {
      case Success(value) => Success(Right(value))
      case Failure(exception: E) => Success(Left(exception))
      case Failure(exception) => Failure(exception)
    }(parasitic)

  def upliftError[E1 >: E <: Throwable /* : ClassTag*/ ]: TypedFuture[E1, T] = new TypedFuture[E1, T](fut)
  //  def upliftErrorAux[EE >: E <: Throwable/* : ClassTag*/](implicit aux: Aux[E, EE, T]): TypedFuture[EE, T] = aux.uplift(this)

  //  type TF[EEE <: Throwable] = TypedFuture[EEE, T]
  //  def upliftErrorAux[EE >: E <: Throwable /* : ClassTag*/ ](implicit aux: Aux[E, EE /*, T*/ ]): TypedFuture[EE, T] = aux.uplift(this)
}

object TypedFuture {

  //  implicit def cast[T, E <: Throwable, EE >: E <: Throwable](from: TypedFuture[E, T])
  //  /*(implicit aux: Aux[E, EE])*/ : TypedFuture[EE, T] = from.asInstanceOf[TypedFuture[EE, T]]
  //    aux.uplift(from)

  //  // TODO: not working. AUX pattern would help
  //  implicit def autoErrorUplifting[E <: Throwable, E1 >: E <: Throwable, T](tf: TypedFuture[E, T]): TypedFuture[E1, T] =
  //    new TypedFuture[E1, T](tf.fut)

  class Successful[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    @nowarn("msg=parameter default in method apply is never used")
    def apply[T](v: T)(implicit default: E := Throwable) /*(implicit ct: ClassTag[E])*/ : TypedFuture[E, T] = new TypedFuture[E, T](Future.successful(v))
  }

  class Failed[T] extends AnyRef {
    def apply[E <: Throwable /*, E1 >: E <: Throwable*/
      /* : ClassTag*/ ](e: E): TypedFuture[E, T] = new TypedFuture[E, T](Future.failed(e))
  }

  class Apply[E <: Throwable /*: ClassTag*/ ] extends AnyRef {
    def apply[T](f: Future[T]) /*(implicit ct: ClassTag[E])*/ : TypedFuture[E, T] = new TypedFuture[E, T](f)
  }

  class Try_[E <: Throwable] extends AnyRef {
    def apply[T](t: Try[T]) /*(implicit ct: ClassTag[E])*/ : TypedFuture[E, T] = new TypedFuture[E, T](Future.fromTry(t))
  }

  //  def apply[T, E <: Throwable: ClassTag](fut: Future[T]): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](fut)
  def apply[E <: Throwable /*: ClassTag*/ ] = new Apply[E]

  //  def successful[T, E <: Throwable: ClassTag](value: T): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.successful(value))
  def successful[E <: Throwable]: Successful[E] = new Successful[E]

  //  def failed[E <: Throwable: ClassTag, T](error: E): dev.tayvs.future.typed.TypedFuture[T, E] = new dev.tayvs.future.typed.TypedFuture[T, E](Future.failed(error))
  def failed[T]: Failed[T] = new Failed[T]

  def fromEither[E <: Throwable /* : ClassTag*/ , T](e: Either[E, T]): TypedFuture[E, T] = e match {
    case Left(err) => TypedFuture.failed[T](err)
    case Right(v) => TypedFuture.successful[E].apply(v)
  }

  def fromTry[E <: Throwable]: Try_[E] = new Try_[E]

  def fromEitherF[E <: Throwable /* : ClassTag*/ , T](f: Future[Either[E, T]]): TypedFuture[E, T] =
    new TypedFuture[E, T](f.flatMap {
      case Left(err) => Future.failed[T](err)
      case Right(v) => Future.successful(v)
    }(ExecutionContext.parasitic))

  def fromPure[T](e: T): PureFuture[T] = new TypedFuture(Future.successful(e))

  def sequence[E <: Throwable : ClassTag, T, CC[X] <: IterableOnce[X], To](s: CC[TypedFuture[E, T]])(implicit ec: ExecutionContext, bf: BuildFrom[CC[TypedFuture[E, T]], T, To]): TypedFuture[E, To] = {
    s.iterator.foldLeft(successful[E](bf.newBuilder(s))) { (acc, el) =>
      acc.zipWith(el)(_.addOne(_))
    }.map(_.result())(parasitic)
  }


  def traverse[E <: Throwable : ClassTag, A, B, M[X] <: IterableOnce[X]](in: M[A])(fn: A => TypedFuture[E, B])(implicit bf: BuildFrom[M[A], B, M[B]], executor: ExecutionContext): TypedFuture[E, M[B]] =
    in.iterator.foldLeft(successful[E](bf.newBuilder(in))) {
      (fr, a) => fr.zipWith(fn(a))(_.addOne(_))
    }.map(_.result())(parasitic)

}