package dev.tayvs.future.typed

object CovariantCasting {

  trait Aux[E <: Throwable, EE >: E <: Throwable/*, T*/] {
//    type TF[T] = TypedFuture[EE, T]
    def uplift[T](from: TypedFuture[E, T]): TypedFuture[EE, T]
  }

  object Aux {

    implicit def aux[E <: Throwable, EE >: E <: Throwable/*, T*/]: Aux[E, EE/*, T*/] = new Aux[E, EE/*, T*/] {
      def uplift[T](from: TypedFuture[E, T]): TypedFuture[EE, T] = from.asInstanceOf[TypedFuture[EE, T]]
    }

  }

}
