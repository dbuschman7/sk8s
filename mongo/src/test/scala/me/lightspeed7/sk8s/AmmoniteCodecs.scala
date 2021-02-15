package me.lightspeed7.sk8s

import org.bson.codecs.configuration.{CodecProvider, CodecRegistry}
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import os.{Path, RelPath}

import scala.collection.concurrent

class AmmoniteCodecs extends CodecProvider {

  private val codecs = concurrent.TrieMap[Class[_], Codec[Object]]()

  private def addCodec(codec: Codec[Object]) =
    codecs.put(codec.getEncoderClass, codec)

  {
    addCodec(new AmmonitePathCodec().asInstanceOf[Codec[Object]])
    addCodec(new AmmoniteRelPathCodec().asInstanceOf[Codec[Object]])
  }

  override def get[T](clazz: Class[T], registry: CodecRegistry): Codec[T] =
    codecs.get(clazz).map(_.asInstanceOf[Codec[T]]).orNull

  class AmmonitePathCodec extends Codec[os.Path] {

    override def getEncoderClass: Class[Path] = classOf[os.Path]

    override def encode(writer: BsonWriter, value: Path, encoderContext: EncoderContext): Unit =
      writer.writeString(value.toString())

    override def decode(reader: BsonReader, decoderContext: DecoderContext): Path =
      os.Path(reader.readString())
  }

  class AmmoniteRelPathCodec extends Codec[os.RelPath] {

    override def getEncoderClass: Class[RelPath] = classOf[os.RelPath]

    override def encode(writer: BsonWriter, value: RelPath, encoderContext: EncoderContext): Unit =
      writer.writeString(value.toString())

    override def decode(reader: BsonReader, decoderContext: DecoderContext): RelPath =
      os.RelPath(reader.readString())
  }

}
