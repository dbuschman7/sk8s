package me.lightspeed7.sk8s

import java.time.{Instant, OffsetDateTime, ZoneId}

import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}

import scala.collection.concurrent

class JavaTimeCodecs extends CodecProvider {
  private val codecs = concurrent.TrieMap[Class[_], Codec[Object]]()

  private def addCodec(codec: Codec[Object]) =
    codecs.put(codec.getEncoderClass, codec)

  {
    addCodec(new OffsetDateTimeCodec().asInstanceOf[Codec[Object]])
  }

  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] =
    codecs.get(clazz).map(_.asInstanceOf[Codec[T]]).orNull

  class OffsetDateTimeCodec extends Codec[OffsetDateTime] {

    val parser8601: java.time.format.DateTimeFormatter = java.time.format.DateTimeFormatter.ISO_TIME

    override def getEncoderClass: Class[OffsetDateTime] = classOf[OffsetDateTime]

    override def encode(writer: BsonWriter, value: OffsetDateTime, encoderContext: EncoderContext): Unit =
      writer.writeDateTime(value.toEpochSecond)

    override def decode(reader: BsonReader, decoderContext: DecoderContext): OffsetDateTime =
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), ZoneId.of("UTC"))
  }
}
