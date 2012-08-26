package models

import org.bson.types.ObjectId

import com.mongodb.casbah.Imports.MongoDBObject
import com.novus.salat.dao.ModelCompanion
import com.novus.salat.dao.SalatDAO
import com.novus.salat.global.ctx
import mongoContext._

import play.api.Play.current
import se.radley.plugin.salat.mongoCollection

case class ProjectMember(id: ObjectId, projectId: ObjectId, email: String)

object ProjectMember extends ModelCompanion[ProjectMember, ObjectId] {

  val collection = mongoCollection("project_member")
  val dao = new SalatDAO[ProjectMember, ObjectId](collection = collection) {}
  
  // -- Queries
  def findByProjectIdAndEmail(projectId: ObjectId, email: String): Option[ProjectMember] =
    dao.findOne(MongoDBObject("projectId" -> projectId, "email" -> email))

  def findByEmail(email: String): List[ProjectMember] =
    dao.find(MongoDBObject("email" -> email)).toList

  def findByProjectId(projectId: ObjectId): List[ProjectMember] =
    dao.find(MongoDBObject("projectId" -> projectId)).toList

}