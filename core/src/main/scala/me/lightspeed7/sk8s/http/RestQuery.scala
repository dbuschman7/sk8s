package me.lightspeed7.sk8s.http

import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate

import com.typesafe.scalalogging.LazyLogging
import javax.net.ssl._
import me.lightspeed7.sk8s.{Endpoints, ErrorMessage, ServiceAccount, Sk8s}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

object RestQuery extends LazyLogging {

  //
  // A bit ugly but good enough for now.
  //
  // TODO: Use the ca.cert from inside the container
  // /////////////////////////////////////////
  def queryForEndpoints(svcAccount: ServiceAccount, serviceName: String, namespace: String = "default")(implicit
    ec: ExecutionContext
  ): Future[Either[ErrorMessage, Endpoints]] =
    Future {
      val trustAll = Array[TrustManager](new TrustManager() {
        def checkServerTrusted(certs: Array[Nothing], authType: String): Unit = {}

        def checkClientTrusted(certs: Array[Nothing], authType: String): Unit = {}

        def getAcceptedIssuers: Array[Nothing] = null
      })

      val trustAllHosts = new HostnameVerifier {
        override def verify(s: String, sslSession: SSLSession): Boolean = true
      }

      object NoCheckX509TrustManager extends X509TrustManager {
        override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

        override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

        override def getAcceptedIssuers: Array[X509Certificate] = Array[X509Certificate]()
      }

      val trustfulSslContext: SSLContext = {
        val context = SSLContext.getInstance("TLS")
        context.init(Array[KeyManager](), Array(NoCheckX509TrustManager), null)
        context
      }

      val path =
        s"https://${Sk8s.Kubernetes.host}:${Sk8s.Kubernetes.port}/api/v1/namespaces/$namespace/endpoints/$serviceName"

      val ctx = SSLContext.getInstance("SSL")
      ctx.init(null, trustAll, new SecureRandom())

      val url = new URL(path)
      logger.info("Getting endpoints from " + url)
      val conn = url.openConnection.asInstanceOf[HttpsURLConnection]

      conn.setHostnameVerifier(trustAllHosts)
      conn.setSSLSocketFactory(trustfulSslContext.getSocketFactory)
      conn.addRequestProperty("Authorization", "Bearer " + svcAccount.token)

      val outData   = Try(Source.fromInputStream(conn.getInputStream, "UTF-8").mkString).toOption
      val errData   = Try(Source.fromInputStream(conn.getErrorStream, "UTF-8").mkString).toOption
      val code: Int = conn.getResponseCode
      (errData, outData) match {
        case (None, Some(data)) =>
          logger.debug("Endpoints - " + data)
          import me.lightspeed7.sk8s.EndpointJson._
          Json.fromJson[Endpoints](Json.parse(data)) match {
            case JsError(errs) =>
              Left(ErrorMessage("Status", "v0", "Failure", errs.mkString("\n"), "Endpoints.JsonParseError", None, 500))
            case JsSuccess(pts, _) => Right(pts)
          }
        case (Some(err), _) =>
          logger.debug("ErrorMessage - " + err)
          Json.fromJson[ErrorMessage](Json.parse(err)) match {
            case JsError(errs) =>
              Left(ErrorMessage("Status", "v0", "Failure", errs.mkString("\n"), "ErroMessage.JsonParseError", None, 500))
            case JsSuccess(pts, _) => Left(pts)
          }
        case (None, None) =>
          Left(ErrorMessage("Status", "v0", "Failure", "No response from API server", "Connection.noResponse", None, 500))

      }
    }
}
