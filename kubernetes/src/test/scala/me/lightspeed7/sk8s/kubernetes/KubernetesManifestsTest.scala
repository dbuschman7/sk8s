package me.lightspeed7.sk8s.kubernetes

import java.time.{ ZoneId, ZonedDateTime }
import java.util.UUID

import me.lightspeed7.sk8s.{ DockerImage, Sk8sFunSuite }
import me.lightspeed7.sk8s.manifests.Common.Java11
import me.lightspeed7.sk8s.manifests.{ JobStatus, Sk8sAppConfig }
import me.lightspeed7.sk8s.util.AlphaId
import org.scalatest.Matchers
import play.api.libs.json.Json

class KubernetesManifestsTest extends Sk8sFunSuite with Matchers {

  val k8sJobId: AlphaId = AlphaId.fromString("gesvbldgpzthvcokmbni")
//  val mongoJobId: UUID = UUID.fromString("5c309a63-30a2-4c91-8d51-e5ae00973f2d")

  implicit val config: Sk8sAppConfig = Sk8sAppConfig
    .create(Java11, "sk8s-" + k8sJobId, "david")
    .replicas(3)
    .semVer(3, 2, 1)
    .image(DockerImage("docker.io", Some("sk8s"), "some-container", Some("3.7.190704.texas")))
    .cpu(1.0)
    .memory(2148)
    .withNoServiceAccountToken

  test("Generate simple job set with base config") {

    val expected: String = getFileContents(getLibraryTestFilePath("kubernetes", "sk8s-job-base.json")).utf8String

    val jobT = config.createJobBase
    jobT.isSuccess shouldBe true

    val pretty = Json.prettyPrint(Json.toJson(jobT.get))
    println(pretty)
    pretty shouldBe expected
  }

  test("test JobStatus") {

    // complete
    // Right(JobStatus(Status(Some(Condition(,,None,None,None,None)),Some(2019-04-03T05:19:08Z),Some(2019-04-03T05:19:29Z),None,Some(1),None)))
    val skCond                 = skuber.batch.Job.Condition("", "", None, None, None, None)
    val skStart: ZonedDateTime = ZonedDateTime.of(2019, 4, 3, 5, 19, 8, 0, ZoneId.systemDefault())
    val skComp: ZonedDateTime  = ZonedDateTime.of(2019, 4, 3, 5, 19, 29, 0, ZoneId.systemDefault())
    val skObjComp              = JobStatus(skuber.batch.Job.Status(List(skCond), Some(skStart), Some(skComp), None, Some(1), None))

    skObjComp.isComplete shouldBe true
    skObjComp.isError shouldBe false
    skObjComp.isSuccess shouldBe true

    skObjComp.duration.toSeconds shouldBe 21

    // error
    val skObjError = JobStatus(skuber.batch.Job.Status(List(skCond), Some(skStart), Some(skComp), None, None, Some(1)))

    skObjError.isComplete shouldBe true
    skObjError.isError shouldBe true
    skObjError.isSuccess shouldBe false

    skObjError.duration.toSeconds shouldBe 21

    // no complete
    // Right(JobStatus(Status(None,Some(2019-04-03T20:31:56Z),None,Some(1),None,None)))
    val skObjRunning = JobStatus(skuber.batch.Job.Status(List(), Some(skStart), None, Some(1), None, None))

    skObjRunning.isComplete shouldBe false
    skObjRunning.isError shouldBe false
    skObjRunning.isSuccess shouldBe false

    skObjRunning.duration.toSeconds shouldBe 0

  }

  test("Generate StatefulSet with base config") {

    val expected: String = getFileContents(getLibraryTestFilePath("kubernetes", "sk8s-sts-base.json")).utf8String

    val stsT = config.createStatefulSetBase
    stsT.isSuccess shouldBe true

    val pretty = Json.prettyPrint(Json.toJson(stsT.get))
    println(pretty)
    pretty shouldBe expected
  }

  test("Generate Deployment with base config") {

    val expected: String = getFileContents(getLibraryTestFilePath("kubernetes", "sk8s-dep-base.json")).utf8String

    val stsT = config.createDeploymentBase
    stsT.isSuccess shouldBe true

    val pretty = Json.prettyPrint(Json.toJson(stsT.get))
    println(pretty)
    pretty shouldBe expected
  }
}
