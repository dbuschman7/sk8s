package me.lightspeed7.sk8s.markdown

import org.scalatest.{ FunSuite, Matchers }

final case class TestData(col1: String, col2: Int, col3: String)

class TablesTest extends FunSuite with Matchers {

  test("Basic Table Test ") {

    val data = Seq(
      TestData("asdfasadf ds dfs fdsdfs", 12345, "sasd sadf df dfsd sdfa"),
      TestData("asdfasadf ds dfs", 12, "df dfsd sdfa"),
      TestData("asdfasadf ds fdsdfs", 12345645, "sasd  dfsd sdfa"),
      TestData("asdfasadf  fs fdsdfs", 12345, "sasd sadf df dfsd sdfa"),
      TestData("asdf ds dfs fdsdfs", 4567, "sasd sadf df sdfa")
    )

    val table: PreFormattedBuild[TestData] = PreFormattedBuild[TestData]("Temporary Table")
      .column("column 1", rightJustified = true) { col =>
        col.col1
      }
      .column("column 2", rightJustified = true) { col =>
        col.col2.toString
      }
      .column("col 3") { col =>
        col.col3
      }

    val output: String = table.generate(data)
    println(output)

    output.lines.map(_.indexOf('|')).toSet shouldBe Set(-1, 24)     // first pipe, one row has none
    output.lines.map(_.lastIndexOf('|')).toSet shouldBe Set(-1, 35) // first pipe, one row has none
  }
}
