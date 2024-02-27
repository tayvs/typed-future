# TypedFuture (Experimental/Alpha/Beta/ApiMayChange)
Scala futures with expected (checked) error type. Returned type will better represent errors that could be returned.
Future value just wrapped with safe wrapper with future-like methods that respect expected error type.


```scala
  def divide(a: Double, b: Double): Future[Double] =
    if (b == 0) Future.failed(new ArithmeticException("Division by zero")) else Future.successful(a / b)

  def divideSafe(a: Double, b: Double): TypedFuture[ArithmeticException, Double] =
    if (b == 0) TypedFuture.failed(new ArithmeticException("Division by zero")) else TypedFuture.successful(a / b)
```

### Expected errors vs unexpected
Library treat exception in the similar way as java. Future could have expected (aka checked) error as well as unexpected.
The second one is not represented in type.

### Pure Future??
You can create instance of `PureFuture` which is aliases for `TypedFuture[Nothing, T]`. It used to show that TypedFuture have no expected error.

### How to get an instance
Because TypedFuture is a wrapper it could be instantiated using existing Future or using "pure" values.

```scala
case class MyError(t: Throwable) extends Throwable(t)

val _: TypedFuture[MyError, Int] = TypedFuture.successful[MyError](21)
val _: TypedFuture[MyError, Int] = TypedFuture.failed[Int](MyError(new Exception()))
val _: TypedFuture[MyError, Int] = TypedFuture[MyError](Future.successful(12)) // Type is required
val _: TypedFuture[MyError, Int] = TypedFuture.fromEither[Int, MyError](Right(12))
val _: TypedFuture[MyError, Int] = TypedFuture.fromTry[MyError](Try(42))
```

**Note**: TypeFuture's apply method should contain type of expected error. If you don't specify it, you will have compilation error.

Another way to create an instance is to use implicit conversion

```scala


Future
  .successful(12)
  .withExpectedError[IllegalArgumentException]
```

And PureFuture... well there only two (2,5) ways to get it.

```scala
// 1. Recover TypedFuture
def divideSafe(a: Double, b: Double): TypedFuture[ArithmeticException, Double] = ???

val _: PureFuture[Double] = divideSafe(10, 0).recover(_ => Double.PositiveInfinity)

// 2. fromPure method
val _: PureFuture[Int] = TypedFuture.fromPure(12)

// 2.5. Manually set type for TypedFuture with expected error Nothing
val _: PureFuture[Int] = TypedFuture.successful[Nothing](21)
```

### Complex example

```scala

val _: PureFuture[String] = Future
  .successful(12)                                                           // Future[Int]
  .withExpectedError[IllegalArgumentException]                              // TypedFuture[IllegalArgumentException, Int]
  .mapError(MyError(_))                                                     // TypedFuture[MyError, Int]
  .map(_ + 1)                                                               // TypedFuture[MyError, Int]
  .flatMap(i => TypedFuture.successful[MyError](i.toString))                // TypedFuture[MyError, String]
  .flatMap(i => TypedFuture.failed[String](YourError(new Exception(""))))   // TypedFuture[YourError, String]
  .flatMap(i => TypedFuture.failed[String](MyError(new Exception(""))))     // TypedFuture[MyError, String]
  .recover(e => "0")
```
