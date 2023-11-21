import TypedFuture.TypedFutureConstructor

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  case class MyError(t: Throwable) extends Throwable(t)

  case class YourError(t: Throwable) extends Throwable(t)

  val _: TypedFuture[Int, MyError] = TypedFuture.successful[MyError](21)
  val _: TypedFuture[Int, MyError] = TypedFuture.failed[Int](MyError(new Exception("")))
  val _: TypedFuture[Int, MyError] = TypedFuture[MyError](Future.successful(12))

  val r: TypedFuture[String, MyError] = Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
    .mapError(MyError(_))
    .map(_ + 1)
    .flatMap(i => TypedFuture.successful[MyError](i.toString))
    .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))
    .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))


  val _: Future[Int] = Future
    .successful(12)
    .recover { case t => 21 }


//  Thread.sleep(1000)
}