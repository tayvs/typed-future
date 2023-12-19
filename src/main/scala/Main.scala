import dev.tayvs.future.typed.TypedFutureWrapper
import dev.tayvs.future.typed.TypedFutureWrapper.{PureFuture, TypedFutureConstructor}

import scala.concurrent.{ExecutionContext, Future}

object Main extends App {
  implicit val ec: ExecutionContext = ExecutionContext.parasitic

  case class MyError(t: Throwable) extends Throwable(t)

  case class YourError(t: Throwable) extends Throwable(t)

  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper.successful[MyError](21)
  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper.failed[Int](MyError(new Exception()))
  val _: TypedFutureWrapper[Int, MyError] = TypedFutureWrapper[MyError](Future.successful(12))

  val r: PureFuture[String] = Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
    .mapError(MyError(_))
    .map(_ + 1)
    .flatMap(i => TypedFutureWrapper.successful[MyError](i.toString))
    .flatMap(i => TypedFutureWrapper.failed[String](YourError(new Exception(""))))
    .flatMap(i => TypedFutureWrapper.failed[String](MyError(new Exception(""))))
    .recover(e => "0")


  val _: Future[Int] = Future
    .successful(12)
    .recover { case t => 21 }

  def divide(a: Double, b: Double): Future[Double] =
    if (b == 0) Future.failed(new ArithmeticException("Division by zero")) else Future.successful(a / b)

  def divideSafe(a: Double, b: Double): TypedFutureWrapper[Double, ArithmeticException] =
    if (b == 0) TypedFutureWrapper.failed(new ArithmeticException("Division by zero")) else TypedFutureWrapper.successful(a / b)

}