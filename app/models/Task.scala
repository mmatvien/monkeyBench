package models

import java.util.{ Date }

import org.bson.types.ObjectId

import com.mongodb.casbah.Imports.MongoDBObject
import com.novus.salat.dao.ModelCompanion
import com.novus.salat.dao.SalatDAO
import com.novus.salat.global.ctx
import mongoContext._
import play.api.Play.current
import se.radley.plugin.salat.mongoCollection

case class Task(id: ObjectId,
                folder: String,
                project: ObjectId,
                title: String,
                done: Boolean,
                dueDate: Option[Date],
                assignedTo: Option[String],
                hits: String,
                token: Option[String],
                header: Option[String],
                uris: String)

object Task extends ModelCompanion[Task, ObjectId] {

  val collection = mongoCollection("task")
  val dao = new SalatDAO[Task, ObjectId](collection = collection) {}
  // -- Queries

  /**
   * Retrieve a Task from the id.
   */
  def findById(id: ObjectId): Option[Task] =
    dao.findOneById(id)

  /**
   * Retrieve todo tasks for the user.
   */
  def findTodoInvolving(user: String): Seq[(Task, Project)] = {
    val projects = ProjectMember.findByEmail(user) map (project => Project.findById(project.projectId).get)
    val tup = projects map (project => { findByProject(project.id) map (task => Tuple2(task, project)) })
    tup.head
  }

  /**
   * Find tasks related to a project
   */
  def findByProject(project: ObjectId): Seq[Task] =
    dao.find(MongoDBObject("project" -> project)).toList

  /**
   * Find tasks related to a folder
   */
  def findByProjectFolder(project: ObjectId, folder: String): Seq[Task] =
    dao.find(MongoDBObject("project" -> project, "folder" -> folder)).toList
  /**
   * Delete a task
   */
  def delete(id: ObjectId) {
    dao.remove(findById(id).get)
  }

  /**
   * Delete all task in a folder.
   */
  def deleteInFolder(projectId: ObjectId, folder: String) {
    findByProjectFolder(projectId, folder) map (task => delete(task.id))
  }

  /**
   * Mark a task as done or not
   */
  def markAsDone(taskId: ObjectId, done: Boolean) {
    dao.update(MongoDBObject("id" -> taskId), MongoDBObject("done" -> done), false, false)
  }

  /**
   * Rename a folder.
   */
  def renameFolder(projectId: ObjectId, folder: String, newName: String) {
    dao.update(MongoDBObject("projectId" -> projectId), MongoDBObject("folder" -> newName), false, false)
  }

  /**
   * Check if a user is the owner of this task
   */
  def isOwner(task: ObjectId, user: String): Boolean = {

    dao.findOneById(task) match {
      case Some(x) => {
        val member = ProjectMember.findByProjectId(x.project) find (_.email == user)
        !member.isEmpty
      }
      case None => false
    }
  }

  /**
   * Create a Task.
   */
  def create(task: Task): Task = {
    val newtask: Task = Task(new ObjectId,
      task.folder,
      task.project,
      task.title,
      task.done,
      task.dueDate,
      task.assignedTo,
      task.hits,
      task.token,
      task.header,
      task.uris)
    dao.save(newtask)
    newtask
  }

}
