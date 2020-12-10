package me.lightspeed7.sk8s

import java.time.{ OffsetDateTime, ZoneOffset }

import me.lightspeed7.sk8s.mongo.{ MongoContext, PriorityQueue, QueueItem }
import org.bson.codecs.configuration.{ CodecProvider, CodecRegistry }
import org.mongodb.scala.bson.codecs.Macros
import org.scalatest.matchers.must.Matchers
import os.{ Path, RelPath }
import play.api.libs.json.{ Json, OFormat }

class PriorityQueueTest extends Sk8sFunSuite with Matchers {

  val outputBase = Path("/prod/dump/uncurated")

  val parentDir: RelPath = outputBase.relativeTo(Path("/prod"))

  test("insert then fetch") {

    implicit val mCtx: MongoContext    = MongoContext.fromEnv("tenor-converter").get
    val queue: PriorityQueue[WorkItem] = WorkItemQueue.createQueue("work")

    // setup the test
    val items = Seq( //
      WorkItem.create("last", 10, outputBase / "foo.ext", parentDir),
      WorkItem.create("middle", 20, outputBase / "bar.ext", parentDir),
      WorkItem.create("first", 30, outputBase / "baz.ext", parentDir) //
    )

    // empty our items form the queue
    items.foreach { i =>
      await(queue.delete(i))
    }

    // fill the queue
    items.foreach { i =>
      Thread.sleep(100)
      await(queue.push(i))
    }

    // fetch items from the queue
    val item1: WorkItem = await(queue.next("foo")).get
    println(item1)
    val item2: WorkItem = await(queue.next("bar")).get
    println(item2)
    val item3: WorkItem = await(queue.next("baz")).get
    println(item3)
    //
    await(queue.next("dave")).isDefined mustBe false

    await(queue.markDone(item1)) mustBe true
    await(queue.markError(item2, "test error 1")) mustBe true
    await(queue.markDone(item3)) mustBe true
    //
    // now we can get the next one
    val item2b: WorkItem = await(queue.next("dave")).get
    await(queue.markError(item2b, "test error 2")) mustBe true
    val item2c: WorkItem = await(queue.next("dave")).get // last time to get this task
    await(queue.markError(item2c, "test error 3")) mustBe true

    // now the task is not handed out
    await(queue.next("dave")).isDefined mustBe false
    await(queue.next("dave")).isDefined mustBe false
  }
}

final case class WorkItem private[sk8s] (_id: String,
                                         priority: Int,
                                         baseDir: Path,
                                         filePath: os.RelPath,
                                         attempts: Int = 0,
                                         errors: Seq[String] = Seq(),
                                         override val createdOn: OffsetDateTime,
                                         override val startedOn: Option[OffsetDateTime],
                                         override val completedOn: Option[OffsetDateTime])
    extends QueueItem {}

object WorkItem extends JsonImplicitsAmmonite {
  implicit val _json: OFormat[WorkItem] = Json.format[WorkItem]

  def create(_id: String, priority: Int, baseDir: Path, filePath: os.RelPath) =
    WorkItem(_id, priority, baseDir, filePath, 0, Seq(), OffsetDateTime.now(ZoneOffset.UTC), None, None)
}

object WorkItemQueue {

  import org.bson.codecs.configuration.CodecRegistries.fromProviders
  val codec: CodecProvider             = Macros.createCodecProviderIgnoreNone[WorkItem]
  implicit val registry: CodecRegistry = fromProviders(new AmmoniteCodecs(), new JavaTimeCodecs(), codec)

  def createQueue(collection: String)(implicit mCtx: MongoContext): PriorityQueue[WorkItem] =
    new PriorityQueue[WorkItem]("work")
}
