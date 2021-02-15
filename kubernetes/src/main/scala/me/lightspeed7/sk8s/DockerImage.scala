package me.lightspeed7.sk8s

import scala.util.Try
import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers

case class DockerImage(repository: String, organization: Option[String], image: String, tag: Option[String]) {

  def headerLine: String =
    "" +
    f"${"Deployment"}%35s | ${"Avail"}%5s | ${"Image"}%-25s  |  ${"Tag"}%s" +
    "\n" +
    f"${"--------------------------"}%35s | ${"-----"}%5s | ${"---------------------------"}%-25s  |  ${"-----------------"}%s" +
    "\n"

  def toSummary(deployment: KubernetesDeployment): String =
    f"${deployment.name}%35s | ${deployment.available}%5d | $image%-25s  |  ${tag.getOrElse("unknown")}%s"

  override def toString: String = {
    val orgPart: String = organization.map(o => "/" + o).getOrElse("")
    val tagPart: String = tag.map(t => ":" + t).getOrElse(":latest")
    s"$repository$orgPart/$image$tagPart"
  }

  def withVersion(ver: String): DockerImage = this.copy(tag = Option(ver))
}

object DockerImage extends RegexParsers {

  private[sk8s] val slashRegex: Regex = "/".r
  private val slash: Parser[String]   = slashRegex ^^ (_.trim)

  private val colonRegex: Regex     = ":".r
  private val colon: Parser[String] = colonRegex ^^ (_.trim)

  // //////////////////////////////////////
  // Currently support tags
  // //////////////////////////////////////
  private[sk8s] val tagRegex: Regex     = "[a-zA-Z0-9\\-\\.]+".r // very liberal here
  private val tagParser: Parser[String] = tagRegex ^^ (_.trim)

  // //////////////////////////////////////

  private[sk8s] val nameRegex: Regex     = "[a-zA-Z][a-zA-Z0-9\\-\\.]+".r
  private val nameParser: Parser[String] = nameRegex ^^ (_.trim)

  private val part3: Parser[DockerImage] = nameParser ~ slash ~ nameParser ~ slash ~ nameParser ~ colon ~ tagParser ^^ {
      case registry ~ _ ~ org ~ _ ~ repo ~ _ ~ tag => DockerImage(registry, Option(org), repo, Option(tag))
    }

  private val part2: Parser[DockerImage] = nameParser ~ slash ~ nameParser ~ colon ~ tagParser ^^ {
      case org ~ _ ~ repo ~ _ ~ tag => DockerImage("docker.io", Option(org), repo, Option(tag))
    }

  private val part1: Parser[DockerImage] = nameParser ~ slash ~ nameParser ^^ {
      case org ~ _ ~ repo => DockerImage("docker.io", Option(org), repo, Option("latest"))
    }

  //
  private val grammar: Parser[DockerImage] = part3 | part2 | part1

  //
  // Entry Point
  // //////////////////////////
  def parse(in: String): Try[DockerImage] =
    Try(parse(grammar, in))
      .flatMap { result: DockerImage.ParseResult[DockerImage] =>
        result
          .map(c => scala.util.Success(c))
          .getOrElse(scala.util.Failure(new IllegalStateException("Invalid parse")))
      }

}
