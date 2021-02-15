package me.lightspeed7.sk8s.markdown

final case class ColumnData[T](label: String, rightJustified: Boolean, value: T => String) {
  def evaluate(in: T): String = value(in)
}

final case class FormattedColumn[T](column: ColumnData[T], input: Seq[T]) {

  private val rawRows: Seq[String] = input.map(column.value)
  val width: Int                   = math.max(column.label.length, rawRows.map(_.length).max)

  lazy val header: String = padData(column.label)
  lazy val spacer: String = padData("-", '-')

  private def padData(in: String, padChar: Char = ' '): String = {
    val len = width - in.length
    if (column.rightJustified) {
      "".padTo(len, padChar) + in
    } else {
      in + "".padTo(len, padChar)
    }
  }

  lazy val rows: Seq[String] = rawRows.map(padData(_))
}

final case class PreFormattedBuild[T](title: String, columns: Seq[ColumnData[T]] = Seq()) {

  def column(label: String, rightJustified: Boolean = false)(value: T => String): PreFormattedBuild[T] =
    this.copy(columns = columns :+ ColumnData(label, rightJustified, value))

  def collate(in: Seq[Seq[String]]): Seq[String] = {
    val heads: Seq[String] = in.flatMap(_.headOption)
    if (heads.isEmpty) heads
    else {
      val tails: Seq[Seq[String]] = in.map { s =>
        if (s.isEmpty) Seq() else s.tail
      }
      heads.mkString(" | ") +: collate(tails)
    }
  }

  def generate(in: Seq[T]): String = {
    val cols: Seq[FormattedColumn[T]] = columns.map(FormattedColumn(_, in))

    val header: String    = cols.map(_.header).mkString(" | ")
    val spacer: String    = cols.map(_.spacer).mkString(" | ")
    val rows: Seq[String] = collate(cols.map(_.rows))

    title + "\n" + (header +: spacer +: rows).mkString("```\n", "\n", "\n```")
  }

}

final case class CsvTable[T](columns: Seq[ColumnData[T]] = Seq()) {

  def column(label: String)(
    value: T => String
  ): CsvTable[T] =
    this.copy(columns = columns :+ ColumnData(label, rightJustified = false, value))

  def collate(in: Seq[Seq[String]]): Seq[String] = {
    val heads: Seq[String] = in.flatMap(_.headOption)
    if (heads.isEmpty) heads
    else {
      val tails: Seq[Seq[String]] = in.map { s =>
        if (s.isEmpty) Seq() else s.tail
      }
      heads.mkString(", ") +: collate(tails)
    }
  }

  def generate(in: Seq[T]): String = {
    val cols: Seq[FormattedColumn[T]] = columns.map(FormattedColumn(_, in))

    val header: String    = cols.map(in => "\"" + in.header + "\"").mkString(", ")
    val rows: Seq[String] = collate(cols.map(_.rows))

    (header +: rows).mkString("\n", "\n", "\n")
  }

}
