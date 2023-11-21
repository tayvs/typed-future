import TypedFuture.TypedFutureConstructor

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  case class MyError(t: Throwable) extends Throwable(t)

  case class YourError(t: Throwable) extends Throwable(t)

  val _: TypedFuture[Int, MyError] = TypedFuture.successful[MyError].apply(21)
  val _: TypedFuture[Int, MyError] = TypedFuture.failed[Int](MyError(new Exception("")))

  val r: TypedFuture[String, MyError] = Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
    .mapError(MyError(_))
    .map(_ + 1)
    .flatMap(i => TypedFuture.successful[MyError].apply(i.toString))
    .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))
    .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))
}