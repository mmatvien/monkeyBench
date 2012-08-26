package models

import org.bson.types.ObjectId

import com.mongodb.casbah.Imports.MongoDBObject
import com.novus.salat.dao.ModelCompanion
import com.novus.salat.dao.SalatDAO
import com.novus.salat.global.ctx

import play.api.Play.current
import se.radley.plugin.salat.mongoCollection
import org.apache.commons.codec.digest.DigestUtils._
import mongoContext._

case class User(id: ObjectId, email: String, name: String, password: String)

object User extends ModelCompanion[User, ObjectId] {
  val collection = mongoCollection("user")
  val dao = new SalatDAO[User, ObjectId](collection = collection) {}

  // -- Queries

  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String): Option[User] =
    dao.findOne(MongoDBObject("email" -> email))

  /**
   * Retrieve all users.
   */

  def fetchAll: Seq[User] = {
    dao.find(MongoDBObject.empty).toList
  }

  /**
   * Authenticate a User.
   */

  def authenticate(email: String, password: String): Option[User] = {
    findByEmail(email).filter { user => user.password == hash(password, user.email + "maxH") }
  }

  private def hash(pass: String, salt: String): String = sha256Hex(salt.padTo('0', 256) + pass)

  /**
   * Create a User.
   */
  def create(user: User): User = {
    val newUser = User(new ObjectId, user.email, user.name, hash(user.password, user.email + "maxH"))
    User.save(newUser)
    newUser
  }

}
