package models

import org.bson.types.ObjectId

import com.mongodb.casbah.Imports.MongoDBObject
import com.novus.salat.dao.ModelCompanion
import com.novus.salat.dao.SalatDAO
import com.novus.salat.global.ctx

import play.api.Play.current
import se.radley.plugin.salat.mongoCollection
import mongoContext._

case class Project(id: ObjectId, folder: String, name: String)

object Project extends ModelCompanion[Project, ObjectId] {

  val collection = mongoCollection("project")
  val dao = new SalatDAO[Project, ObjectId](collection = collection) {}
  // -- Queries

  /**
   * Retrieve a Project from id.
   */
  def findById(id: ObjectId): Option[Project] =
    dao.findOneById(id)

  /**
   * Retrieve project for user
   */
  def findInvolving(user: String): Seq[Project] = {
    ProjectMember.findByEmail(user) map (projectMamber => dao.findOneById(projectMamber.projectId).get)
  }

  def findByFolder(folder: String): List[Project] =
    dao.find(MongoDBObject("folder" -> folder)).toList

  /**
   * Update a project.
   */
  def rename(id: ObjectId, newName: String): Unit =
    dao.update(MongoDBObject("id" -> id), MongoDBObject("name" -> newName), false, false)

  /**
   * Delete a project.
   */
  def delete(id: ObjectId): Unit =
    dao.remove(dao.findOneById(id).get)

  /**
   * Delete all project in a folder
   */
  def deleteInFolder(folder: String): Unit =
    findByFolder(folder).foreach(dao.remove(_))

  /**
   * Rename a folder
   */
  def renameFolder(folder: String, newName: String): Unit =
    dao.update(MongoDBObject("folder" -> folder), MongoDBObject("folder" -> newName), false, false)

  /**
   * Retrieve project member
   */
  def membersOf(project: ObjectId): Seq[User] =
    ProjectMember.findByProjectId(project) map (s => User.findByEmail(s.email).get)

  /**
   * Add a member to the project team.
   */
  def addMember(project: ObjectId, user: String): Unit =
    ProjectMember.save(ProjectMember(new ObjectId(), new ObjectId(project.toString), user))

  /**
   * Remove a member from the project team.
   */
  def removeMember(project: ObjectId, user: String): Unit =
    ProjectMember.remove(ProjectMember.findByProjectIdAndEmail(project, user).get)

  /**
   * Check if a user is a member of this project
   */
  def isMember(project: ObjectId, user: String): Boolean = {
    ProjectMember.findByProjectIdAndEmail(project, user) match {
      case Some(x) => true
      case None    => false
    }
  }

  /**
   * Create a Project.
   */
  def create(project: Project, members: Seq[String]): Project = {
    val projectId: ObjectId = new ObjectId
    val newProject: Project = Project(projectId, project.name, project.folder)
    dao.save(newProject)
    members.foreach { email =>
      ProjectMember.save(ProjectMember(new ObjectId, projectId, email))
    }
    newProject
  }

}
