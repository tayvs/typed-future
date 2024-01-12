import dev.tayvs.future.typed._

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  case class MyError(t: Throwable) extends Throwable(t)

  case class YourError(t: Throwable) extends Throwable(t)

  val un1: TypedFuture[Int, MyError] = TypedFuture.successful[MyError](21)
  val un2: TypedFuture[Int, MyError] = TypedFuture.failed[Int](MyError(new Exception()))
  val un3: TypedFuture[Int, MyError] = TypedFuture[MyError](Future.successful(12))
  val un4: TypedFuture[Int, MyError] = TypedFuture.fromTry[MyError](util.Try(12))

  val r: PureFuture[String] = Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
    .mapError(MyError(_))
    .map(_ + 1)
    .flatMap(i => TypedFuture.successful[MyError](i.toString))
    .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))
    .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))
    .recover(e => "0")


  val un4: Future[Int] = Future
    .successful(12)
    .recover { _ => 21 }

  def divide(a: Double, b: Double): Future[Double] =
    if (b == 0) Future.failed(new ArithmeticException("Division by zero")) else Future.successful(a / b)

  def divideSafe(a: Double, b: Double): TypedFuture[Double, ArithmeticException] =
    if (b == 0) TypedFuture.failed(new ArithmeticException("Division by zero")) else TypedFuture.successful(a / b)

  val res: TypedFuture[Int, Throwable] = TypedFuture
    .successful[Throwable](42)
    .flatMap(_ => TypedFuture.successful[Nothing](12))

}