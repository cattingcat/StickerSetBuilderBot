import java.util

import scala.collection.mutable.ArrayBuffer

val t = 99
val k = t * 99

// If clause
val p = if (k > 9000) 55 else 66

// def variations
def f(): String = "qwe"
def g: String = "asd"
def f1(){ "sdfsdf" }
def f2() = {"sdfsdf"}

(a: String) => a

// Unit comparison
f1() == ()


class MyList extends Iterable[Int] {
  override def iterator = new MyListIterator

  class MyListIterator extends Iterator[Int] {
    val arr = Array[Int](1,2,3,4,5)
    val iterator = arr.iterator

    override def hasNext = iterator.hasNext

    override def next() = iterator.next()
  }
}


// For basics
for(a <- new MyList) print(a)
val arr = for(a <- new MyList) yield a

// Symbols
val symb = 'qweqweqweq


// Classes
class Rational(val n: Int, val d: Int) {
  def this(n: Int) = this(n, 1)

  def *(other: Rational) = new Rational(n * other.n, d * other.d)

  override def toString: String = s"$n / $d"
}

// Implicit conversions
//implicit def intToRational(a: Int): Rational = new Rational(a)
implicit def iToR(a: Int): Rational = new Rational(a)
2 * new Rational(1, 2)

// Ranges
1 to 55
55 until 1
for(a <- 1 to 50 if a % 3 == 0) yield a
for(
  a <- 1 to 5;
  b <- 2 to 6;
  sum = a + b
  if sum > 5
) yield (a, b, sum)



// Match
5 match {
  case i: Int if i > 2 => ">2"
  case i: Int if i > 1 => ">1"
  case _ => "other"
}

// Partial apply
def f4(a: Int, b: Int, c: Int) = a + b + c
val f5 = f4 _
val f6 = f4(1, 2, _: Int)

// Variadic  args
def f7(args: String*) = args

// named args
f4(c = 1, b = 4, a = 55)

// default
def f8(a: String = "sdfsdf") = a * 3


// currying
def f9(a: Int)(b: Int) = a + b
f9(1) _
f9(1)(2)

// Pass args via curly brackets
def f10(a: Int) = a
f10 { 1 }

def f11(args: String*)(f: String => String) = args.map(f)
f11("1", "2", "3") {
  a => a * 3
}


def f12(f: () => String) = 555
f12(() => "123" * 5)


def f13(f: => String) = 555
f13("123" * 5)


implicit def iToS(i: Int): String = i.toString
55555.substring(3)


trait Queue[T] {
  def put(a: T)
  def get(): T
}

trait RichQueue[T] extends Queue[T] {
  def putAll(items: Iterable[T]): Unit = items.foreach(put)
}

trait Doubling extends Queue[Int] {
  abstract override def put(a: Int) { super.put(a * 2)}
}

class IntQueue extends Queue[Int] with RichQueue[Int] {
  val buf = new ArrayBuffer[Int]()

  override def put(a: Int): Unit = buf.append(a)

  override def get(): Int = buf.remove(buf.length - 1)

  override def toString: String = buf.mkString(" ")
}

val q = new IntQueue() with Doubling
q.put(123)
q



val partFunc: PartialFunction[List[Int], String] = {
  case a :: b :: _ => (a + b).toString
}

partFunc.isDefinedAt(List(1, 2, 3, 4))
partFunc.isDefinedAt(List(1))

partFunc(List(1, 3, 5))


class SimpleGene[T] {}
//val sg1: SimpleGene[Int] = new SimpleGene[AnyVal]   //fail
//val sg2: SimpleGene[AnyVal] = new SimpleGene[Int]   //fail

class CovariantGene[+T] {}
//val cg1: CovariantGene[Int] = new CovariantGene[AnyVal]   //fail
//val cg2: CovariantGene[AnyVal] = new CovariantGene[Int]   //ok

class ContravariantGene[-T] {}
//val cng1: ContravariantGene[Int] = new ContravariantGene[AnyVal]   //ok
//val cng2: ContravariantGene[AnyVal] = new ContravariantGene[Int]   //fail

// Function[-S, +T], S-in, T-out


trait TraitWithFields {
  val a: String
  val b: Int

  def foo = a * b
}

val twf = new TraitWithFields {
  override val a: String = "qweqwe"
  override val b: Int = 3
}

twf.foo

val twf2 = new {val a = "qwe"; val b = 3} with TraitWithFields



trait TraitWithType {
  type SomeType

  def foo(t: SomeType): String
}

class ClassWithType extends TraitWithType {
  type SomeType = String

  override def foo(t: String): String = "qwe " + t
}


object SomeEnumeration extends Enumeration {
  val A = Value
  val B = Value
  val C = Value
  val D, E, F = Value
}


class ClassWithOp {
  def :#%^!:(a: Int): ClassWithOp = new ClassWithOp

//  def unapply(arg: ClassWithOp): Option[Int] = ???
}

//case class :#%^!:(a: Int, b: ClassWithOp) extends ClassWithOp
//
//val qq = new ClassWithOp
//val qqq = 11 :#%^!: qq
//
//qqq match {
//  case 11 :#%^!: _ => "yep"
//}

// View bounds, deprecated.
// You can see all internals fo implicit parameter within method
//def maxList[T <% Ordered[T]](elements: List[T]): T = ???

val q1,q2,q3 = new Object
q1.toString
q2.toString
q3.toString


def fib(a: Int, b: Int): Stream[Int] = a #:: fib(b, a + b)


1 :: 2 :: Nil ++ (1 :: Nil)

val werw = <eert>erert</eert>

