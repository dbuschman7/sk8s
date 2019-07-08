package me.lightspeed7.sk8s

import java.util.Properties

import org.scalatest.{ Assertion, Matchers }

import scala.concurrent.duration._
import scala.util.Try

class VariablesTest extends Sk8sFunSuite with Matchers {

  test("Test Sources") {
    Sources.env.value("HOME") should be(Sources.sysProps.value("user.home"))
  }

  test("Test default value handling ") {

    val p   = new Properties()
    val src = PropertiesSource(p)

    val var1 = Variables.source[Boolean](src, "bool1", Constant[Boolean](false))
    val var2 = Variables.source[Boolean](src, "bool2", Constant[Boolean](false))
    val var3 = Variables.source[Boolean](src, "bool3", Constant[Boolean](true))

    p.setProperty("bool1", "true")
    p.setProperty("bool2", "false")

    var1.exists shouldBe true
    var1.value shouldBe true

    var2.exists shouldBe true
    var2.value shouldBe false

    var3.value shouldBe true
  }

  test("Test data type handling") {
    new Variable[String] {
      def exists: Boolean = false

      def name: String = ""

      def displayName: String = ""

      def value: String = ""

      def testMe: Assertion = {
        convert[Boolean]("true") should be(Some(true))
        convert[Int]("12345") should be(Some(12345))
        convert[Long]("1234567890123") should be(Some(1234567890123L))
        convert[Duration]("10 seconds") should be(Some(10 seconds))
      }
    }.testMe

  }

  test("Sys Props dynamic behavior") {
    val var1 = Variables.source[String](Sources.sysProps, "dave1", Constant("DaVe"))
    System.clearProperty("dave1")

    val before = var1.value
    System.setProperty("dave1", " wuz heer")
    val after = var1.value

    (before + after) should be("DaVe wuz heer")
  }

  test("test combination variables") {

    val p   = new Properties()
    val src = Sources.registerProperties("props", p)

    val multi = Variables.multiSource[Boolean]("bools", Constant(false), (src, "bool1", false), (src, "bool2", true), (src, "bool3", false))
    multi.value should be(false) // default from first since none defined

    p.setProperty("bool2", "true")
    multi.value should be(true) // explicit value for second
    RunMode.setRunMode(RunMode.Production)

    multi.valueStr should be("**********") // security is enforced
    RunMode.setRunMode(RunMode.Test)

    p.setProperty("bool1", "false")
    multi.value should be(false)      // explicit value for first
    multi.valueStr should be("false") // not secured
  }

  test("RunMode Detection") {
    import RunMode._
    find("STAGING") should be(Some(Staging))
    find("PRODUCTION") should be(Some(Production))
    find("DEVELOPER") should be(Some(Developer))
    find("TEST") should be(Some(Test))
  }

  test("Test RunMode variables") {
    import RunMode._

    val multi = Variables.runMode[String](
      "Detect Run Mode",
      "default", //
      (Developer, Constant("developer")), //
      (Production, Constant("production")), //
      Staging -> Constant("staging"), //
      Test    -> Constant("test") //
    )

    System.clearProperty(RunMode.SK8S_RUN_MODE_ENV)
    RunMode.setRunMode(RunMode.Production)
    Thread.sleep(1000)
    multi.value should be("production")

    RunMode.setRunMode(RunMode.Staging)
    multi.value should be("staging")

    Sources.env.asInstanceOf[EnvironmentSource].clearVariable(SK8S_RUN_MODE_ENV)
    val expected = if (currentRunMode == Test) "test" else "developer"
    multi.value should be(expected)
  }

  test(" maybe source behavior") {

    val mby: Variable[String] = Variables.maybeSource(Sources.env, "maybe")
    mby.exists shouldBe false
    Try(mby.value).isFailure shouldBe true
    mby.isConstant shouldBe false
    mby.security shouldBe false
    mby.displayName shouldBe "Src(Env) - maybe"
    Try(mby.value).isFailure shouldBe true
    Try(mby.value).failed.get.getClass.getName shouldBe "java.lang.IllegalStateException"

    Sources.env.asInstanceOf[EnvironmentSource].overrideVariable("maybe", "success")
    mby.value shouldBe "success"
  }

  test("Test FirstValueVariable") {
    val var1                    = Variables.source(Sources.env, "ENV_VAR", Constant("env"))
    var first: Variable[String] = Variables.firstValue("firstValue", var1)

    // no env set yet.
    first.exists shouldBe true // external always returns true
    Try(first.value).isFailure shouldBe true

    Sources.env.asInstanceOf[EnvironmentSource].overrideVariable("ENV_VAR", "success")
    first.value shouldBe "success"

    // catch unknown variables before the existing value
    val var2 = Variables.maybeSource[String](Sources.env, "FOO")
    val var3 = Variables.maybeSource[String](Sources.env, "BAR")

    first = Variables.firstValue[String]("first", var3, var2, var1)
    val tryVal = Try(first.value)
    tryVal.isSuccess shouldBe true
    tryVal.get shouldBe "success"
  }

  test("Test source vars") {
    val mode: String = Variables.source[String](Sources.env, "MODE", Constant("Application")).value
    mode shouldBe "Application"
  }

  test("test security masking of values") {
    import RunMode._

    val var1 = Variables.source(Sources.env, "foo", Constant("Home", "Not Env"))
    val var2 = Variables.source(Sources.env, "foo", Constant("Home", "Not Env"), security = true)

    RunMode.setRunMode(RunMode.Test)
    currentRunMode.requiresSecurity should be(false)
    var1.toString() should include("Not Env")
    var2.toString() should include("Not Env")

    RunMode.setRunMode(RunMode.Developer)
    currentRunMode.requiresSecurity should be(false)
    var1.toString() should include("Not Env")
    var2.toString() should include("Not Env")

    RunMode.setRunMode(RunMode.Production)
    currentRunMode.requiresSecurity should be(true)
    var1.toString() should include("Not Env")
    var2.toString() should include("**********")

    RunMode.setRunMode(RunMode.Staging)
    currentRunMode.requiresSecurity should be(true)
    var1.toString() should include("Not Env")
    var2.toString() should include("**********")

    RunMode.setRunMode(RunMode.FuncTest)
    currentRunMode.requiresSecurity should be(true)
    var1.toString() should include("Not Env")
    var2.toString() should include("**********")

    RunMode.setTest()
  }

  test("print lines up") {
    val var1 = Variables.source(Sources.env, "foo", Constant("Home", "Not Env"))
    val var2 = Variables.constant("bar", "value")

    println(var1.dump(0))
    println(var2.dump(0))

    // make the the text lines up for display
    var1.dump(0).indexOf("foo") shouldBe var2.dump(0).indexOf("bar")
  }

  test("dump configuration") {
    Variables.dumpConfiguration(in => println(in))

    Variables.dumpJson(in => println(in))
  }

  test("DefinedService test") {
    import Variables._

    val serviceFullName = "serviceFullName"
    val envVarKey       = Some("envVarKey")
    val external: Variable[String] = Variables
      .external[String]("name", security = false, Constant[String]("Constant", "constant"), () => Option("external"))

    // Allow new variable to override others
    val svcUrl: Variable[String] = {
      Variables.firstValue[String](
        serviceFullName,
        maybeSource(Sources.env, serviceFullName),
        maybeSource(Sources.env, envVarKey.getOrElse("UNKNOWN_URL")), //
        external //
      )
    }

    val tryVal = Try(svcUrl.value)
    tryVal.isSuccess shouldBe true
    tryVal.get shouldBe "external"

  }
}
