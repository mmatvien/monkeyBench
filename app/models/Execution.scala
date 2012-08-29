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
import java.util.Date

case class Execution(
  id: ObjectId,
  groupId: ObjectId,
  task: ObjectId,
  time: Date,
  url: String,
  headers: String,
  status: String,
  timeSpent: Long)

object Execution extends ModelCompanion[Execution, ObjectId] {
  val collection = mongoCollection("execution")
  val dao = new SalatDAO[Execution, ObjectId](collection = collection) {}

  def findByTask(task: ObjectId): Seq[Execution] = {
    dao.find(MongoDBObject("task" -> task)).toList
  }

  def findByGroup(groupId: ObjectId): Seq[Execution] = {
    dao.find(MongoDBObject("groupId" -> groupId)).toList
  }

  
  def create(exec: Execution): Execution = {
    val execNew: Execution = Execution(new ObjectId,
      exec.groupId,
      exec.task,
      exec.time,
      exec.url,
      exec.headers,
      exec.status,
      exec.timeSpent)
    dao.save(execNew)
    execNew
  }
  
  def deleteGroup(groupId:ObjectId){
    val execs = findByGroup(groupId)
    execs.foreach(x => dao.remove(x))
  }
  // -- Queries
}
