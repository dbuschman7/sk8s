package me.lightspeed7.sk8s

import com.typesafe.scalalogging.LazyLogging
import skuber.ConfigMap
import slack.SlackUtil
import slack.models.{ ChannelDeleted, FileShared, Message, UserTyping }

import scala.concurrent.{ ExecutionContext, Future }

final case class MessageResponse(channelName: String, ts: String, bot: SlackBot) {
  def update(newMsg: String)(implicit ec: ExecutionContext): Future[MessageResponse] = bot.updateMessage(this, newMsg).map(_ => this)
}

final case class SlackContext(bot: SlackBot, kubernetes: Kubernetes, appCtx: Sk8sContext)

object SlackBot {
  type FileUpoadHandler = (SlackContext, FileShared) => Future[ConfigMap]
}

final case class SlackBot(token: String, channelName: String)(handler: Message => Unit)(implicit appCtx: Sk8sContext)
    extends AutoCloseable
    with LazyLogging {

  import appCtx._

  val kubernetes: Kubernetes = new Kubernetes()
  val client: SlackClient    = SlackClient(token)
  val chanId: String         = client.channelIdFor(channelName)

  implicit lazy val ctx: SlackContext = SlackContext(this, kubernetes, appCtx)

  private def processMessage(message: Message): Unit = {
    val mentionedIds = SlackUtil.extractMentionedIds(message.text)
    if (mentionedIds.contains(client.selfId.id)) {
      val channelMatch: Boolean = message.channel == chanId
      if (!channelMatch) {
        client.sendMessage(message.channel, s"I am sorry <@${message.user}>, I am afraid that I cannot do that !") // Think Stanley Kubrick's 2001
      } else {
        handler(message) // process the message
      }
      ()
    }
  }

  // add in custom handlers
  var fileSharedHandler: Option[SlackBot.FileUpoadHandler] = None

  def addHandler(handler: SlackBot.FileUpoadHandler): Unit = fileSharedHandler = Option(handler)

  // event handler
  client.rtmClient.onEvent {
    case e: Message        => processMessage(e)
    case e: UserTyping     => logger.info("onEvent(UserTyping) => " + e)
    case e: ChannelDeleted => logger.info("onEvent(ChannelDeleted) => " + e)
    case fs: FileShared =>
      fileSharedHandler.map(h => h(ctx, fs)).getOrElse(logger.info("onEvent(FileShared) => no handler")); ()
    case other => logger.info("onEvent(Other) => " + other)
  }

  // send a message
  def sendMessage(chanelId: String, message: String): Future[MessageResponse] =
    client
      .sendMessage(chanelId, message)
      .map { ts =>
        MessageResponse(channelName, ts, this)
      }

  def updateMessage(response: MessageResponse, newMsg: String): Future[MessageResponse] =
    client
      .replaceMessage(response.channelName, response.ts, newMsg)
      .map { ts =>
        MessageResponse(channelName, ts, this)
      }

  def postAttachments(channelName: String, attachments: Seq[Attachment]): Future[MessageResponse] =
    client
      .postAttachments(channelName, attachments)
      .map { ts =>
        MessageResponse(channelName, ts, this)
      }

  def updateAttachments(channelName: String, ts: String, attachments: Seq[Attachment]): Future[MessageResponse] =
    client
      .replaceAttachments(channelName, ts, attachments)
      .map { ts =>
        MessageResponse(channelName, ts, this)
      }

  def close(): Unit = client.close()
}
