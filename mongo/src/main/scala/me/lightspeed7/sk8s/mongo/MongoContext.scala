package me.lightspeed7.sk8s.mongo

import com.typesafe.scalalogging.LazyLogging
import me.lightspeed7.sk8s.{ Constant, Sk8sContext, Sources, Variable, Variables }
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.{ Document, MongoClient, MongoCollection, MongoDatabase }

import scala.reflect.ClassTag
import scala.util.Try

final class MongoContext private (mongoClient: MongoClient, dbName: String)(implicit val sk8s: Sk8sContext) extends AutoCloseable {

  val database: MongoDatabase = mongoClient.getDatabase(dbName)

  def collection[T](collection: String, registry: CodecRegistry)(implicit ct: ClassTag[T]): MongoCollection[T] = {
    val coll: MongoCollection[T] = database.withCodecRegistry(registry).getCollection[T](collection)
    coll
  }

  def collectionGeneric(collection: String, registry: CodecRegistry): MongoCollection[Document] = {
    val coll: MongoCollection[Document] = database.withCodecRegistry(registry).getCollection(collection)
    coll
  }

  override def close(): Unit = ???
}

object MongoContext extends LazyLogging {

  /**
   * use admin
   * db.createUser(
   * {
   * user: "user",
   * pwd: "password",
   * roles: [ { "role" : "readWriteAnyDatabase", "db" : "admin" }  ]
   * }
   * )
   */
  lazy val mongoUrl: Variable[String] =
    Variables.source[String](Sources.env, "SK8S_MONGO_URL", Constant("mongodb://user:password@localhost:27017/admin"))

  //
  // mongodb://[username:password@]host1[:port1][,...hostN[:portN]][/[defaultauthdb][?options]]
  // mongodb://myDBReader:D1fficultP%40ssw0rd@mongodb0.example.com:27017/?authSource=admin
  // mongodb://mongodb1.example.com:27317,mongodb2.example.com:27017/?replicaSet=mySet&authSource=authDB
  // ////////////////////////////////
  def fromEnv[T <: Product](database: String)(implicit sk8s: Sk8sContext): Try[MongoContext] = Try {
    val url = mongoUrl.value
    logger.debug(("URL - " + url))
    val client = sk8s.registerCloseable("MongoClient", MongoClient(url))
    new MongoContext(client, database)
  }

}
