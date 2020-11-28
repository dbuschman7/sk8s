package me.lightspeed7.sk8s.mongo
import java.time.{ OffsetDateTime, ZoneOffset }

import com.mongodb.client.model.{ FindOneAndUpdateOptions, ReturnDocument }
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.UpdateOptions
import play.api.libs.json.Writes

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class QueueItem extends Product {
  def _id: String

  def priority: Int

  def assigned: Option[String] = None

  def createdOn: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

  def startedOn: Option[OffsetDateTime] = None

  def completedOn: Option[OffsetDateTime] = None
}

//
// Priority Queue
// //////////////////////////////////
final class PriorityQueue[T <: QueueItem](collectionName: String)(implicit mCtx: MongoContext, registry: CodecRegistry, ct: ClassTag[T]) {

  import mCtx.sk8s._
  import org.mongodb.scala.model.Filters._
  import org.mongodb.scala.model.Sorts._
  import org.mongodb.scala.model.Updates._
1
  val coll: MongoCollection[T] = {
    import org.bson.codecs.configuration.CodecRegistries.fromRegistries
    import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY

    val codecRegistry            = fromRegistries(registry, DEFAULT_CODEC_REGISTRY)
    val coll: MongoCollection[T] = mCtx.collection[T](collectionName, codecRegistry)
    //
    coll
  }

  def next(assigned: String): Future[Option[T]] = {
    val filter: Bson                     = combine(exists("assigned", exists = false), exists("completedOn", exists = false))
    val sort: Bson                       = orderBy(descending("priority"), ascending("createdOn"))
    val update: Bson                     = combine(currentDate("startedOn"), set("assigned", assigned))
    val options: FindOneAndUpdateOptions = new FindOneAndUpdateOptions().sort(sort).returnDocument(ReturnDocument.AFTER)

    coll.findOneAndUpdate(filter, update, options).toFutureOption()
  }

  def push(obj: T)(implicit read: Writes[T]): Future[Boolean] = coll.insertOne(obj).toFuture().map(_ => true)

  def markDone(obj: T): Future[Boolean] = markDone(obj._id)

  def markDone(id: String): Future[Boolean] = {
    val filter: Bson           = equal("_id", id)
    val update: Bson           = combine(currentDate("completedOn"), unset("assigned"))
    val options: UpdateOptions = new UpdateOptions().upsert(false)
    coll.updateOne(filter, update, options).toFuture().map(_ => true)
  }

  def delete(obj: T): Future[Boolean] = coll.deleteOne(equal("_id", obj._id)).toFuture().map { _.getDeletedCount > 0 }

}
