# TypedFuture
Scala futures with expected (checked) error type. Returned type will better represent errors that could be returned.
Future value just wrapped with safe wrapper with future-like methods that respect expected error type.


```scala
  def divide(a: Double, b: Double): Future[Double] =
    if (b == 0) Future.failed(new ArithmeticException("Division by zero")) else Future.successful(a / b)

  def divideSafe(a: Double, b: Double): TypedFuture[Double, ArithmeticException] =
    if (b == 0) TypedFuture.failed(new ArithmeticException("Division by zero")) else TypedFuture.successful(a / b)
```

### Expected errors vs unexpected
Library treat exception in the similar way as java. Future could have expected (aka checked) error as well as unexpected.
The second one is not represented in type.

### Pure Future??
You can create instance of `PureFuture` which is aliases for `TypedFuture[T, Nothing]`. It used to show that TypedFuture have no expected error.

### How to get an instance
Because TypedFuture is a wrapper it could be instantiated using existing Future or using "pure" values.

```scala
case class MyError(t: Throwable) extends Throwable(t)

val _: TypedFuture[Int, MyError] = TypedFuture.successful[MyError](21)
val _: TypedFuture[Int, MyError] = TypedFuture.failed[Int](MyError(new Exception()))
val _: TypedFuture[Int, MyError] = TypedFuture[MyError](Future.successful(12))
val _: TypedFuture[Int, MyError] = TypedFuture.fromEither[Int, MyError](Right(12))
```

Another way to create an instance is to use implicit conversion

```scala
import dev.tayvs.future.typed._

Future
    .successful(12)
    .withExpectedError[IllegalArgumentException]
```

And PureFuture... well there only two (2,5) ways to get it.

```scala
// 1. Recover TypedFuture
def divideSafe(a: Double, b: Double): TypedFuture[Double, ArithmeticException] = ???

val _: PureFuture[Double] = divideSafe(10, 0).recover(_ => Double.PositiveInfinity)

// 2. fromPure method
val _: PureFuture[Int] = TypedFuture.fromPure(12)

// 2.5. Manually set type for TypedFuture with expected error Nothing
val _: PureFuture[Int] = TypedFuture.successful[Nothing](21)
```

### Complex example

```scala

val _: PureFuture[String] = Future
  .successful(12)                                                                  // Future[Int]
  .withExpectedError[IllegalArgumentException]                                     // TypedFuture[Int, IllegalArgumentException]
  .mapError(MyError(_))                                                            // TypedFuture[Int, MyError]
  .map(_ + 1)                                                                      // TypedFuture[Int, MyError]
  .flatMap(i => TypedFuture.successful[MyError](i.toString))                // TypedFuture[String, MyError]
  .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))   // TypedFuture[String, YourError]
  .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))     // TypedFuture[String, MyError]
  .recover(e => "0")
```