import dev.tayvs.future.typed.TypedFutureWrapper
import dev.tayvs.future.typed.TypedFutureWrapper.TypedFutureConstructor

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  case class MyError(t: Throwable) extends Throwable(t)

  case class YourError(t: Throwable) extends Throwable(t)

  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper.successful[MyError](21)
  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper.failed[Int](MyError(new Exception("")))
  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper[MyError](Future.successful(12))

  val r: TypedFutureWrapper[String, MyError] = Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
    .mapError(MyError(_))
    .map(_ + 1)
    .flatMap(i => TypedFutureWrapper.successful[MyError](i.toString))
    .flatMap(i => TypedFutureWrapper.failed[String](YourError(new Exception(""))))
    .flatMap(i => TypedFutureWrapper.failed[String](MyError(new Exception(""))))


  val _: Future[Int] = Future
    .successful(12)
    .recover { case t => 21 }

}