package me.lightspeed7.sk8s.telemetry

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.softwaremill.sttp._

import scala.concurrent.ExecutionContext

final case class BackendServerClient(host: String = "localhost", port: Int = 8999)(implicit akka: ActorSystem) extends AutoCloseable {

  implicit val ec: ExecutionContext = akka.dispatcher

  implicit val mat: ActorMaterializer = ActorMaterializer()

  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  override def close(): Unit =
    mat.shutdown()

  def ping: Id[Response[String]] = {
    val response = sttp.get(uri"http://$host:$port/ping").send()
    println(response)
    response
  }

  def health: Id[Response[String]] = {
    var response = sttp.get(uri"http://localhost:8999/health").send()
    println(response)
    response
  }

  def metrics: Id[Response[Array[Byte]]] = {
    val response: Id[Response[Array[Byte]]] = sttp.response(asByteArray).get(uri"http://localhost:8999/metrics").send()
    println(response)
    response
  }

}
