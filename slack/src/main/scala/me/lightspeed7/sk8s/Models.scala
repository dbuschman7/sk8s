package me.lightspeed7.sk8s

final case class UpdateResponse(ok: Boolean, channel: String, ts: String, text: String)

final case class Attachment(
    fallback: Option[String] = None,
    callback_id: Option[String] = None,
    color: Option[String] = None,
    pretext: Option[String] = None,
    author_name: Option[String] = None,
    author_link: Option[String] = None,
    author_icon: Option[String] = None,
    title: Option[String] = None,
    title_link: Option[String] = None,
    text: Option[String] = None,
    fields: Seq[AttachmentField] = Seq.empty,
    image_url: Option[String] = None,
    thumb_url: Option[String] = None,
    footer: Option[String] = None,
    footer_icon: Option[String] = None,
    actions: Seq[ActionField] = Seq.empty,
    mrkdwn_in: Seq[String] = Seq.empty,
    ts: Option[Int] = None
)

final case class AttachmentField(title: String, value: String, short: Boolean)

final case class ActionField(name: String,
                             text: String,
                             `type`: String,
                             style: Option[String] = None,
                             value: Option[String] = None,
                             confirm: Option[ConfirmField] = None)

final case class ConfirmField(text: String, title: Option[String] = None, ok_text: Option[String] = None, cancel_text: Option[String] = None)

case class SlackFile(
    id: String,
    created: Long,
    timestamp: Long,
    name: Option[String],
    title: String,
    mimetype: String,
    filetype: String,
    pretty_type: String,
    user: String,
    //    editable: Boolean,
    size: Long,
    mode: String,
    is_external: Boolean,
    external_type: String,
    is_public: Boolean,
    public_url_shared: Boolean,
    //    display_as_bot: Boolean,
    //    username: String,

    url_private: Option[String],
    url_private_download: Option[String],
    permalink: Option[String],
    permalink_public: Option[String],
    //    edit_link: Option[String],

    //    preview: Option[String],
    //    preview_highlight: Option[String],
    lines: Option[Int],
    //    lines_more: Option[Int],
    //    preview_is_truncated: Boolean,
    //    comments_count: Int,
    is_starred: Option[Boolean],
    channels: Seq[String]
//
)

final case class SlackFileResponse(
    ok: Boolean,
    file: SlackFile,
    content: String,
    is_truncated: Boolean
)

trait JsonImplicits {
  import play.api.libs.json._
  implicit val _updateResponse: OFormat[UpdateResponse]   = Json.format[UpdateResponse]
  implicit val _actionField: OFormat[ActionField]         = Json.format[ActionField]
  implicit val _confirmField: OFormat[ConfirmField]       = Json.format[ConfirmField]
  implicit val _attachmentField: OFormat[AttachmentField] = Json.format[AttachmentField]
  implicit val _attachment: OFormat[Attachment]           = Json.format[Attachment]

  implicit val _slackFile: OFormat[SlackFile]                 = Json.format[SlackFile]
  implicit val _slackFileResponse: OFormat[SlackFileResponse] = Json.format[SlackFileResponse]
}
