import org.scalatest.FunSuite

class SimpleTest extends FunSuite {
  test("basic") {
    assert(2 + 2 == 4)
  }

  test("list unapply") {
    val list = List(1, 2, 3, 4, 5, 6)

    list match {
      case List(1, a @ _*) => assert(a.head == 2)
      case _ => assert(false)
    }

    list match {
      case x :: y :: _ => assert(x == 1 && y == 2)
    }
  }
}
