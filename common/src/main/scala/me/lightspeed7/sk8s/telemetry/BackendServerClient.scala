package me.lightspeed7.sk8s.telemetry

import akka.http.scaladsl.model.MediaTypes
import com.softwaremill.sttp._
import me.lightspeed7.sk8s.Sk8sContext

final case class BackendServerClient(host: String = "localhost", port: Int = 8999)(implicit ctx: Sk8sContext) {

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  def ping: Id[Response[String]] = sttp.get(uri"http://$host:$port/ping").send()

  def health: Id[Response[String]] = sttp.get(uri"http://localhost:8999/health").send()

  def metrics: Id[Response[Array[Byte]]] = sttp.response(asByteArray).get(uri"http://localhost:8999/metrics").send()

  def ip: Id[Response[String]] = sttp.get(uri"http://localhost:8999/ip").send()

  def configAsJson: Id[Response[String]] = {
    val request = emptyRequest
      .header("Accept", MediaTypes.`application/json`.toString())
      .get(uri"http://localhost:8999/config")
    request.send()
  }

  def configAsText: Id[Response[String]] = {
    val request = emptyRequest
      .get(uri"http://localhost:8999/config")
      .header("Accept", MediaTypes.`text/plain`.toString())
    request.send()
  }

}
