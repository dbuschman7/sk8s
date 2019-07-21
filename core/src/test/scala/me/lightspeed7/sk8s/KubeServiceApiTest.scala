package me.lightspeed7.sk8s

import java.nio.file.{ Path, Paths }

import me.lightspeed7.sk8s.http.RestQuery
import org.scalatest.{ FunSuite, Matchers }
import play.api.libs.json.{ JsResult, Json }

import scala.concurrent.Await
import scala.concurrent.duration._

class KubeServiceApiTest extends FunSuite with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  import EndpointJson._

  val sk8sDir: Path           = Paths.get("sk8s").toAbsolutePath
  val testResourcesPath: Path = Paths.get(sk8sDir.toString + "/src/test/resources")
  val svcAcct: ServiceAccount = Sk8s.serviceAccount(testResourcesPath)

  ignore("Rest Query test") { // this test only works for my minikube install, thus cannot run autonomous

    val f = RestQuery.queryForEndpoints(svcAcct, "hello")

    val response = Await.result(f, Duration.Inf)
    response should not be null
    response.isRight shouldBe true
    println(response.right.get)

    println(response.right.get.subsets.mkString(","))

  }

  ignore("HostAndPort Parsing") { // this test only works for my minikube install, thus cannot run autonomous

    val endpoints: Option[Endpoints] = Await.result(svcAcct.endpoints("hello", "server"), Duration.Inf)
    val pods                         = endpoints.get.hostIpsWithPort(endpoints.get.servicePort(Some("server")))

    pods.length should be(7)
    pods.map(_.port).toSet.size should be(1)

  }

  test("Parse Endpoint Json Kafka") {
    val endPointsJ: JsResult[Endpoints] = Json.fromJson[Endpoints](Json.parse(endpointData1))
    endPointsJ.isSuccess should be(true)
    val endPoints = endPointsJ.get

    println(endPoints)
    endPoints.subsets.foreach(println)

    val hostList1 = endPoints.hostIpsWithPort(9092)
    println(s"HostList 1 - $hostList1")
    hostList1.length shouldBe 3
    hostList1.map(_.port).toSet shouldBe Set(9092)
    hostList1.map(_.host).toSet shouldBe Set("10.244.10.110", "10.244.11.50", "10.244.4.69")

    val hostList2 = endPoints.hostIpsWithPort(endPoints.servicePort(Some("server")))
    println(s"HostList 2 - $hostList2")
    hostList2.length shouldBe 3
    hostList2.map(_.port).toSet shouldBe Set(9092)
    hostList2.map(_.host).toSet shouldBe Set("10.244.10.110", "10.244.11.50", "10.244.4.69")

    val dnsList: Set[String] = endPoints.dnsHosts(9092)
    println(dnsList)
    dnsList.size shouldBe 3
    dnsList shouldBe Set( //
                         "kafka-0.kafka-svc.data.svc.cluster.local",
                         "kafka-2.kafka-svc.data.svc.cluster.local",
                         "kafka-1.kafka-svc.data.svc.cluster.local" //
    )
  }

  test("Parse Endpoint Json Mongo") {
    val endPointsJ: JsResult[Endpoints] = Json.fromJson[Endpoints](Json.parse(endpointData2))
    endPointsJ.isSuccess should be(true)
    val endPoints = endPointsJ.get

    println(endPoints)
    endPoints.subsets.foreach(println)

    val hostList = endPoints.hostIpsWithPort(27017)
    println(s"HostList 1 - $hostList")
    hostList.length shouldBe 3
    hostList.map(_.port).toSet shouldBe Set(27017)
    hostList.map(_.host).toSet shouldBe Set("10.244.1.4", "10.244.3.6", "10.244.4.4")

    val dnsList: Set[String] = endPoints.dnsHosts(27017)
    println(dnsList)
    dnsList.size shouldBe 3
    dnsList shouldBe Set( //
      "mongo-controller-test-1.mongo-svc.default.svc.cluster.local",
      "mongo-controller-test-0.mongo-svc.default.svc.cluster.local",
      "mongo-controller-test-2.mongo-svc.default.svc.cluster.local" //
    )
  }

  test("Parse endpoint metadata-svc") {
    val endPointsJ: JsResult[Endpoints] = Json.fromJson[Endpoints](Json.parse(endpointData3))
    endPointsJ.isSuccess should be(true)
    val endPoints = endPointsJ.get

    println(endPoints)
    endPoints.subsets.foreach(println)

    val hostList = endPoints.hostIpsWithPort(9000)
    println(s"HostList 1 - $hostList")
    hostList.length shouldBe 2
    hostList.map(_.port).toSet shouldBe Set(9000)
    hostList.map(_.host).toSet shouldBe Set("10.244.0.34", "10.244.7.113")

  }

  test("Parse helm chart deployment JSON") {
    val endPointsJ: JsResult[Endpoints] = Json.fromJson[Endpoints](Json.parse(endpointData4))
    endPointsJ.isSuccess should be(true)
    val endPoints = endPointsJ.get

    val portName: Option[String] = endPoints.findPortNameFor(27017)
    portName shouldBe Some("mongodb")

    endPoints.servicePort(portName) shouldBe 27017

    val hostList = endPoints.hostIpsWithPort(27017)
    println(hostList)
    hostList.size shouldBe 4
    hostList.map(_.host).toSet shouldBe Set("10.244.11.13", "10.244.11.14", "10.244.6.9", "10.244.7.12")
    hostList.map(_.port).toSet shouldBe Set(27017)

    endPoints.dnsHosts(27017) shouldBe Set(
      "mongo-data-mongodb-primary-0.mongo-data-mongodb-headless.data.svc.cluster.local",
      "mongo-data-mongodb-secondary-0.mongo-data-mongodb-headless.data.svc.cluster.local",
      "mongo-data-mongodb-secondary-1.mongo-data-mongodb-headless.data.svc.cluster.local",
      "mongo-data-mongodb-arbiter-0.mongo-data-mongodb-headless.data.svc.cluster.local"
    )

  }

  test("Parse ErrorMessage JSON ") {
    val jsResult: JsResult[ErrorMessage] = Json.fromJson[ErrorMessage](Json.parse(errorMessageJson))
    jsResult.isSuccess shouldBe true

    val status = jsResult.get

    status.code shouldBe 403
    status.message should include("is forbidden")
    status.details.get.name shouldBe "mongo-data"
  }

  val endpointData1: String =
    """
      |{
      |  "kind": "Endpoints",
      |  "apiVersion": "v1",
      |  "metadata": {
      |    "name": "kafka-svc",
      |    "namespace": "data",
      |    "selfLink": "/api/v1/namespaces/data/endpoints/kafka-svc",
      |    "uid": "d29aa7b4-9cae-11e8-9627-0a58ac1f0588",
      |    "resourceVersion": "16789834",
      |    "creationTimestamp": "2018-08-10T15:05:30Z",
      |    "labels": {
      |      "app": "kafka"
      |    }
      |  },
      |  "subsets": [
      |    {
      |      "addresses": [
      |        {
      |          "ip": "10.244.10.110",
      |          "hostname": "kafka-0",
      |          "nodeName": "aks-agentpool-25033075-0",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "data",
      |            "name": "kafka-0",
      |            "uid": "706273d1-9be2-11e8-9627-0a58ac1f0588",
      |            "resourceVersion": "16617810"
      |          }
      |        },
      |        {
      |          "ip": "10.244.11.50",
      |          "hostname": "kafka-2",
      |          "nodeName": "aks-agentpool-25033075-5",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "data",
      |            "name": "kafka-2",
      |            "uid": "000f5c61-9be3-11e8-9627-0a58ac1f0588",
      |            "resourceVersion": "16618659"
      |          }
      |        },
      |        {
      |          "ip": "10.244.4.69",
      |          "hostname": "kafka-1",
      |          "nodeName": "aks-agentpool-25033075-1",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "data",
      |            "name": "kafka-1",
      |            "uid": "93fe1ea9-9be2-11e8-9627-0a58ac1f0588",
      |            "resourceVersion": "16618199"
      |          }
      |        }
      |      ],
      |      "ports": [
      |        {
      |          "name": "server",
      |          "port": 9092,
      |          "protocol": "TCP"
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val endpointData2: String =
    """
      |{
      |  "kind": "Endpoints",
      |  "apiVersion": "v1",
      |  "metadata": {
      |    "name": "mongo-svc",
      |    "namespace": "default",
      |    "selfLink": "/api/v1/namespaces/default/endpoints/mongo-svc",
      |    "uid": "f3052015-5a15-11e8-93e1-0a58ac1f077c",
      |    "resourceVersion": "13191909",
      |    "creationTimestamp": "2018-05-17T21:04:55Z",
      |    "labels": {
      |      "app": "mongo"
      |    }
      |  },
      |  "subsets": [
      |    {
      |      "addresses": [
      |        {
      |          "ip": "10.244.1.4",
      |          "hostname": "mongo-controller-test-1",
      |          "nodeName": "aks-agentpool-25033075-4",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "default",
      |            "name": "mongo-controller-test-1",
      |            "uid": "6f79fa32-89df-11e8-9a57-0a58ac1f0b46",
      |            "resourceVersion": "12557399"
      |          }
      |        },
      |        {
      |          "ip": "10.244.3.6",
      |          "hostname": "mongo-controller-test-0",
      |          "nodeName": "aks-agentpool-25033075-9",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "default",
      |            "name": "mongo-controller-test-0",
      |            "uid": "6ce1d92b-8a0f-11e8-bea6-0a58ac1f06e2",
      |            "resourceVersion": "12608916"
      |          }
      |        },
      |        {
      |          "ip": "10.244.4.4",
      |          "hostname": "mongo-controller-test-2",
      |          "nodeName": "aks-agentpool-25033075-1",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "default",
      |            "name": "mongo-controller-test-2",
      |            "uid": "1151404f-8a0c-11e8-bea6-0a58ac1f06e2",
      |            "resourceVersion": "13191908"
      |          }
      |        }
      |      ],
      |      "ports": [
      |        {
      |          "port": 27017,
      |          "protocol": "TCP"
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val endpointData3: String =
    """
      |{
      |  "kind": "Endpoints",
      |  "apiVersion": "v1",
      |  "metadata": {
      |    "name": "metadata-svc",
      |    "namespace": "default",
      |    "selfLink": "/api/v1/namespaces/default/endpoints/metadata-svc",
      |    "uid": "ce34a384-5a15-11e8-93e1-0a58ac1f077c",
      |    "resourceVersion": "17764573",
      |    "creationTimestamp": "2018-05-17T21:03:53Z",
      |    "labels": {
      |      "app": "metadata"
      |    }
      |  },
      |  "subsets": [
      |    {
      |      "addresses": [
      |        {
      |          "ip": "10.244.0.34",
      |          "nodeName": "aks-agentpool-25033075-7",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "default",
      |            "name": "metadata-api-59b99885f6-gx46f",
      |            "uid": "91b92a74-9cb4-11e8-9627-0a58ac1f0588",
      |            "resourceVersion": "16795271"
      |          }
      |        },
      |        {
      |          "ip": "10.244.7.113",
      |          "nodeName": "aks-agentpool-25033075-2",
      |          "targetRef": {
      |            "kind": "Pod",
      |            "namespace": "default",
      |            "name": "metadata-api-59b99885f6-b8rhf",
      |            "uid": "8aefa43e-9cb4-11e8-9627-0a58ac1f0588",
      |            "resourceVersion": "17764546"
      |          }
      |        }
      |      ],
      |      "ports": [
      |        {
      |          "name": "server",
      |          "port": 9000,
      |          "protocol": "TCP"
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin

  val endpointData4: String =
    """
      |{
      |   "kind":"Endpoints",
      |   "apiVersion":"v1",
      |   "metadata":{
      |      "name":"mongo-data-mongodb-headless",
      |      "namespace":"data",
      |      "selfLink":"/api/v1/namespaces/data/endpoints/mongo-data-mongodb-headless",
      |      "uid":"ab4e0589-3b3a-11e9-abff-4eb50e73dd5a",
      |      "resourceVersion":"9223240",
      |      "creationTimestamp":"2019-02-28T09:24:37Z",
      |      "labels":{
      |         "app":"mongodb",
      |         "chart":"mongodb-5.6.2",
      |         "heritage":"Tiller",
      |         "release":"mongo-data"
      |      }
      |   },
      |   "subsets":[
      |      {
      |         "addresses":[
      |            {
      |               "ip":"10.244.11.14",
      |               "hostname":"mongo-data-mongodb-primary-0",
      |               "nodeName":"aks-agentpool-83610088-0",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-primary-0",
      |                  "uid":"ab6775f1-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223213"
      |               }
      |            },
      |            {
      |               "ip":"10.244.6.9",
      |               "hostname":"mongo-data-mongodb-secondary-0",
      |               "nodeName":"aks-agentpool-83610088-6",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-secondary-0",
      |                  "uid":"ab79aab3-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223141"
      |               }
      |            },
      |            {
      |               "ip":"10.244.7.12",
      |               "hostname":"mongo-data-mongodb-secondary-1",
      |               "nodeName":"aks-agentpool-83610088-2",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-secondary-1",
      |                  "uid":"ab7fab47-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223163"
      |               }
      |            }
      |         ],
      |         "ports":[
      |            {
      |               "name":"metrics",
      |               "port":9216,
      |               "protocol":"TCP"
      |            }
      |         ]
      |      },
      |      {
      |         "addresses":[
      |            {
      |               "ip":"10.244.11.13",
      |               "hostname":"mongo-data-mongodb-arbiter-0",
      |               "nodeName":"aks-agentpool-83610088-0",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-arbiter-0",
      |                  "uid":"ab56744e-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223238"
      |               }
      |            },
      |            {
      |               "ip":"10.244.11.14",
      |               "hostname":"mongo-data-mongodb-primary-0",
      |               "nodeName":"aks-agentpool-83610088-0",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-primary-0",
      |                  "uid":"ab6775f1-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223213"
      |               }
      |            },
      |            {
      |               "ip":"10.244.6.9",
      |               "hostname":"mongo-data-mongodb-secondary-0",
      |               "nodeName":"aks-agentpool-83610088-6",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-secondary-0",
      |                  "uid":"ab79aab3-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223141"
      |               }
      |            },
      |            {
      |               "ip":"10.244.7.12",
      |               "hostname":"mongo-data-mongodb-secondary-1",
      |               "nodeName":"aks-agentpool-83610088-2",
      |               "targetRef":{
      |                  "kind":"Pod",
      |                  "namespace":"data",
      |                  "name":"mongo-data-mongodb-secondary-1",
      |                  "uid":"ab7fab47-3b3a-11e9-abff-4eb50e73dd5a",
      |                  "resourceVersion":"9223163"
      |               }
      |            }
      |         ],
      |         "ports":[
      |            {
      |               "name":"mongodb",
      |               "port":27017,
      |               "protocol":"TCP"
      |            }
      |         ]
      |      }
      |   ]
      |}
    """.stripMargin

  val errorMessageJson: String =
    """{
      |   "kind":"Status",
      |   "apiVersion":"v1",
      |   "metadata":{
      |
      |   },
      |   "status":"Failure",
      |   "message":"endpoints \"mongo-data\" is forbidden: User \"system:serviceaccount:default:api-endpoints-read\" cannot get resource \"endpoints\" in API group \"\" in the namespace \"data\"",
      |   "reason":"Forbidden",
      |   "details":{
      |      "name":"mongo-data",
      |      "kind":"endpoints"
      |   },
      |   "code":403
      |}
      |""".stripMargin
}
