
val stream = 1 #:: 2 #:: Stream.empty
val stream2 = (1 to 10).toStream

val foo = stream2.sliding(4, 1)
val foo2 = foo.map(_.take(3).toList)
val foo3 = foo2.take(3).toList

val foo4 = stream2.find(i => i > 7)

println("**************")
val foo6 = stream2.collect{ case in if in % 2  == 0 =>  in}
foo6.take(5).toList.map(println)

val foo7 = stream2.sortBy(i => i % 2 ==0).toList

import me.lightspeed7.sk8s.util.Time

import scala.math.BigInt

Time.it("Fibs Recursive") {
  lazy val fibS: Stream[BigInt] = BigInt(0) #:: fibS.scan(BigInt(1))(_ + _)
  fibS.take(20).toList
}

//Time.it("naive") {
//  import Math.abs
//  val Error = 0.000001
//  def factorial(n: Int): Int = (1 to n).product
//  def func(n:Int): Int = 1 / factorial(n)
//  val stream = Stream.tabulate(Int.MaxValue)(n => func(n))
//  val seriesSum =
//    (stream zip stream.tail).takeWhile{case(a, b) => abs(abs(a) - abs(b)) > Error}.map(_._1).sum
//
//}


Time.it("Factorial") {
  def factorial(n: BigInt): BigInt = (BigInt(1) to n).product

  def factorials: Stream[BigInt] = Stream.iterate(BigInt(1))(_ + 1).map(factorial)

  def values: Stream[BigDecimal] = factorials.scanLeft(BigDecimal(0))(_ + 1 / BigDecimal(_))

  factorials.take(20).map(println)
}


Time.it("fact2") {
  val stream = Stream.iterate((1, BigInt(1))){case (num, fact) =>  (num + 1, fact * (num+1))   }.map(_._2)
  stream.take(20).toList.map(println)

}

