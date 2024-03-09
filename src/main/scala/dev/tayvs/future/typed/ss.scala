package dev.tayvs.future.typed

/** Implementation of Default Type for type parameter
 * based on https://gist.github.com/cvogt/8672a3fd97bc97714822 */
class :=[T, Q]

trait Default_:= {
  /** Ignore default */
  implicit def useProvided[Provided, Default]: Provided := Default = new:=[Provided, Default]
}

object := extends Default_:= {
  /** Infer type argument to default */
  implicit def useDefault[Default]: Default := Default = new:=[Default, Default]
}