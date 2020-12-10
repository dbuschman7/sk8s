package me.lightspeed7.sk8s.auth

import me.lightspeed7.sk8s.json.NamedEnumEntry
import play.api.libs.json.{ Json, OFormat }

import scala.collection.concurrent.TrieMap

//
// Base Role Definition
// ////////////////////////////////
final case class Role( //
                      name: String,
                      impliedRoles: Set[String],
                      ordinal: Int,
                      internal: Boolean = false,
                      authorized: Boolean = true //
) extends NamedEnumEntry
    with Serializable
    with Ordered[Role] {

  lazy val allImplies: Set[String] = allImpliedRoles.map(_.name)

  lazy val allImpliedRoles: Set[Role] = impliedRoles.foldLeft(Set[Role](this)) { (acc, cur) =>
    acc ++ RolesRegistry.find(cur).map(_.allImpliedRoles).getOrElse(Set[Role]())
  }

  def implies(perm: Role): Boolean = allImpliedRoles.contains(perm)

  def compare(that: Role): Int = this.ordinal - that.ordinal

  def fromJava: Role = this

}

object Role {
  implicit val __json: OFormat[Role] = Json.format[Role]
}

object RolesRegistry {

  private val map: TrieMap[String, Role] = TrieMap[String, Role]()

  def register(roles: Role*): Unit = roles.map(r => map.put(r.name, r))

  def find(name: String): Option[Role] = map.get(name)

  def values(): Seq[Role]     = map.values.toSeq.sortBy(_.ordinal)
  def valuesPublic: Seq[Role] = values() filterNot (_.internal)
  def loggedIn: Set[Role]     = values().filter(_.authorized).toSet

  def loadPresets(): Unit =
    register(
      Seq(
        new Role("Anonymous", Set(), ordinal = 0, internal = true, authorized = false),
        new Role("Authenticated", Set("Anonymous"), 1, internal = true, authorized = false),
        new Role("User", Set("Anonymous"), 1, internal = false, authorized = true)
      ) //
      : _*)
}
