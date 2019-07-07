package me.lightspeed7.sk8s

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, Uri }
import akka.stream.ActorMaterializer
import akka.util.ByteString
import play.api.libs.json.{ Format, JsValue, Json }
import slack.api.{ ApiError, InvalidResponseError }
import slack.models.User

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

final case class SlackClient(token: String)(implicit system: ActorSystem) extends AutoCloseable with JsonImplicits {

  implicit val mat: ActorMaterializer       = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  lazy val rtmClient: slack.rtm.SlackRtmClient = slack.rtm.SlackRtmClient(token)

  lazy val selfId: User = rtmClient.state.self

  def channelIdFor(channelName: String): String =
    rtmClient.getState().getChannelIdForName(channelName).getOrElse(channelName)

  def userList: Seq[User] = rtmClient.getState().users

  private val apiBaseRequest = HttpRequest(uri = Uri(s"https://slack.com/api/"))

  private val apiBaseWithTokenRequest = {
    apiBaseRequest.withUri(apiBaseRequest.uri.withQuery(Uri.Query(apiBaseRequest.uri.query() :+ ("token" -> token): _*)))
  }

  private def addQueryParams(request: HttpRequest, queryParams: Seq[(String, String)]): HttpRequest =
    request.withUri(request.uri.withQuery(Uri.Query(request.uri.query() ++ queryParams: _*)))

  private def cleanParams(params: Seq[(String, Any)]): Seq[(String, String)] = {
    var paramList = Seq[(String, String)]()
    params.foreach {
      case (k, Some(v)) => paramList :+= (k -> v.toString)
      case (_, None)    => // Nothing - Filter out none
      case (k, v)       => paramList :+= (k -> v.toString)
    }
    paramList
  }

  private def addSegment(request: HttpRequest, segment: String): HttpRequest =
    request.withUri(request.uri.withPath(request.uri.path + segment))

  private def makeApiMethodRequest(apiMethod: String, queryParams: (String, Any)*): Future[JsValue] = {
    val req = addSegment(apiBaseWithTokenRequest, apiMethod)
    makeApiRequest(addQueryParams(req, cleanParams(queryParams)))
  }

  private def makeApiRequest(request: HttpRequest): Future[JsValue] =
    Http().singleRequest(request).flatMap {
      case response if response.status.intValue == 200 =>
        response.entity.toStrict(10.seconds).map { entity =>
          val parsed = Json.parse(entity.data.decodeString("UTF-8"))
          if ((parsed \ "ok").as[Boolean]) {
            parsed
          } else {
            throw ApiError((parsed \ "error").as[String])
          }
        }
      case response =>
        response.entity.toStrict(10.seconds).map { entity =>
          throw InvalidResponseError(response.status.intValue, entity.data.decodeString("UTF-8"))
        }
    }

  private def extract[T](jsFuture: Future[JsValue], field: String)(implicit fmt: Format[T]): Future[T] =
    jsFuture.map(js => (js \ field).as[T])

  def sendMessage(channelName: String, text: String): Future[String] =
    postChatMessage(channelName, Option(text), asUser = Some(true))

  def replaceMessage(channelName: String, ts: String, text: String): Future[String] =
    updateChatMessage(channelName, ts, Option(text), asUser = Some(true)).map(_.ts)

  def postAttachments(channelName: String, attachments: Seq[Attachment]): Future[String] = {
    val att = if (attachments.isEmpty) None else Some(attachments)
    postChatMessage(channelName, None, asUser = Some(true), attachments = att)
  }

  def replaceAttachments(channelName: String, ts: String, attachments: Seq[Attachment]): Future[String] = {
    val att = if (attachments.isEmpty) None else Some(attachments)
    updateChatMessage(channelName, ts, None, asUser = Some(true), attachments = att).map(_.channel)
  }

  protected def postChatMessage(channelName: String,
                                text: Option[String],
                                username: Option[String] = None,
                                asUser: Option[Boolean] = None,
                                parse: Option[String] = None,
                                linkNames: Option[String] = None,
                                attachments: Option[Seq[Attachment]] = None,
                                unfurlLinks: Option[Boolean] = None,
                                unfurlMedia: Option[Boolean] = None,
                                iconUrl: Option[String] = None,
                                iconEmoji: Option[String] = None,
                                replaceOriginal: Option[Boolean] = None,
                                deleteOriginal: Option[Boolean] = None): Future[String] = {

    val res = makeApiMethodRequest(
      "chat.postMessage",
      "channel"          -> channelIdFor(channelName),
      "text"             -> text,
      "username"         -> username,
      "as_user"          -> asUser,
      "parse"            -> parse,
      "link_names"       -> linkNames,
      "attachments"      -> attachments.map(a => Json.stringify(Json.toJson(a))),
      "unfurl_links"     -> unfurlLinks,
      "unfurl_media"     -> unfurlMedia,
      "icon_url"         -> iconUrl,
      "icon_emoji"       -> iconEmoji,
      "replace_original" -> replaceOriginal,
      "delete_original"  -> deleteOriginal
    )

    extract[String](res, "ts")
  }

  protected def updateChatMessage(channelName: String,
                                  ts: String,
                                  text: Option[String],
                                  asUser: Option[Boolean],
                                  attachments: Option[Seq[Attachment]] = None): Future[UpdateResponse] = {
    val params = Seq( //
                     "channel"     -> channelIdFor(channelName),
                     "ts"          -> ts,
                     "text"        -> text,
                     "attachments" -> attachments.map(a => Json.stringify(Json.toJson(a))))

    makeApiMethodRequest("chat.update", asUser.map(b => params :+ ("as_user" -> b)).getOrElse(params): _*)
      .map(_.as[UpdateResponse])
  }

  def getFileFromPermalink(file: SlackFile): Future[ByteString] =
    file.permalink_public
      .map { url =>
        val request = HttpRequest(uri = Uri(url).withQuery(Uri.Query("token" -> token)))
        println("request - " + request)
        Http().singleRequest(request).flatMap {
          case response if response.status.intValue == 200 =>
            response.entity.toStrict(10.seconds).map(_.data)
          case response =>
            response.entity.toStrict(10.seconds).map { entity =>
              println(entity)
              throw InvalidResponseError(response.status.intValue, entity.data.decodeString("UTF-8"))
            }
        }
      }
      .getOrElse(Future failed new Exception("No url provided"))

  def getUploadedFile(fileId: String, count: Option[Int] = None, page: Option[Int] = None): Future[SlackFileResponse] =
    makeApiMethodRequest("files.info", "file" -> fileId, "count" -> count, "page" -> page)
      .map(_.as[SlackFileResponse])
      .flatMap { resp =>
        if (resp.is_truncated) {
          getFileFromPermalink(resp.file).map(rawContent => resp.copy(content = rawContent.utf8String))
        } else {
          Future successful resp
        }
      }

  override def close(): Unit = mat.shutdown()
}
