package dev.tayvs.future.typed

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

class TypedFutureSpec extends AnyFunSuite with Matchers with ScalaFutures {
  private implicit val ec: ExecutionContext = ExecutionContext.parasitic


  test("Future successful should create Future under the hood") {
    TypedFuture.successful(42).toClassic.futureValue shouldBe 42
  }

  test("Future failed should create failed Future under the hood") {
    val ex = new ArithmeticException()
    TypedFuture.failed(ex).toClassic.failed.futureValue shouldBe ex
  }

  test("Future apply with successful future should wrap it with same value") {
    TypedFuture[Nothing](Future.successful(42)).toClassic.futureValue shouldBe 42
  }

  test("Future apply with failed future should wrap it with same value") {
    val ex = new ArithmeticException()
    TypedFuture[Nothing](Future.failed(ex)).toClassic.failed.futureValue shouldBe ex
  }

  test("map should map underlying future") {
    TypedFuture.successful(42).map(_ + 1).toClassic.futureValue shouldBe 43
  }

  test("map on failed future should do nothing") {
    val ex = new ArithmeticException()
    TypedFuture.failed[Int](ex).map(_ + 1).toClassic.failed.futureValue shouldBe ex
  }

  test("flatMap should flatMap underlying future") {
    TypedFuture.successful(42).flatMap(i => TypedFuture.successful(i + 1)).toClassic.futureValue shouldBe 43
  }

  test("flatMap with failed future should use failed error from seconds future") {
    val ex = new ArithmeticException()
    TypedFuture.successful(42).flatMap(i => TypedFuture.failed(ex)).toClassic.failed.futureValue shouldBe ex
  }

  test("flatMap on failed future with failed future should use error from the first one") {
    val ex1 = new ArithmeticException("ex1")
    val ex2 = new ArithmeticException("ex2")
    TypedFuture.failed(ex1)
      .flatMap(_ => TypedFuture.failed(ex2))
      .toClassic.failed.futureValue shouldBe ex1
  }

  test("mapError should map expected error") {
    case class MyError(t: Throwable) extends Throwable(t)

    val ex = new ArithmeticException()
    val tFuture: TypedFuture[Nothing, MyError] = TypedFuture.failed(ex).mapError(new Exception(_)).mapError(MyError)
    val outError1: Throwable = tFuture.toClassic.failed.futureValue
    outError1 shouldBe a[MyError]
    val outError2 = outError1.asInstanceOf[MyError].t
    outError2 shouldBe a[Exception]
    val innerError = outError2.asInstanceOf[Exception].getCause
    innerError shouldBe ex
  }

  test("toClassicSafe on successful future should return successful value as a Right value") {
    TypedFuture.successful(42).toClassicSafe.futureValue shouldBe Right(42)
  }

  test("toClassicSafe on failed future should return failed value as a Left value") {
    val ex = new ArithmeticException()
    TypedFuture.failed(ex).toClassicSafe.futureValue shouldBe Left(ex)
  }

  test("recoverUnexpectedError should recover unexpected error") {
    case class MyError(n: Int) extends Throwable
    val ex = MyError(42)
    val failedClassicFuture: Future[Int] = Future.failed(ex)
    val tFuture: TypedFuture[Int, ArithmeticException] = TypedFuture[ArithmeticException](failedClassicFuture).recoverUnexpectedError {
      case e: MyError => Right(e.n)
    }
    tFuture.toClassic.futureValue shouldBe 42
  }

  test("long chain should computes properly") {
    case class MyError(t: Throwable) extends Throwable(t)
    case class YourError(t: Throwable) extends Throwable(t)
    Future
      .successful(12)
      .withExpectedError[IllegalArgumentException]
      .mapError(MyError(_))
      .map(_ + 1)
      .flatMap(i => TypedFuture.successful[MyError](i.toString))
      .flatMap(_ => TypedFuture.failed[String](YourError(new Exception(""))))
      .flatMap(_ => TypedFuture.failed[String](MyError(new Exception(""))))
      .recover(_ => "0")
      .toClassic.futureValue shouldBe "0"
  }

}
