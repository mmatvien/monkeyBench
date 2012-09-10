package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import java.util.{ Date }
import models._
import views._
import org.bson.types.ObjectId
import akka.actor.ActorSystem
import akka.actor.Props
import actors.MasterExecutor
import actors.Start

/**
 * Manage tasks related operations.
 */
object Tasks extends Controller with Secured {

  /**
   * Display the tasks panel for this project.
   */
  def index(project: ObjectId) = IsMemberOf(project) { _ =>
    implicit request =>
      Project.findById(project).map { p =>
        val tasks = Task.findByProject(project)
        val team = Project.membersOf(project)
        Ok(html.tasks.index(p, tasks, team))
      }.getOrElse(NotFound)
  }

  val taskForm = Form(
    tuple(
      "title" -> nonEmptyText,
      "dueDate" -> optional(date("MM/dd/yy")),
      "assignedTo" -> optional(text),
      "hits" -> nonEmptyText,
      "token" -> optional(text),
      "header" -> optional(text),
      "uris" -> nonEmptyText,
      "action" -> nonEmptyText,
      "body" -> optional(text)
    )
  )

  // -- Tasks

  /**
   * Create a task in this project.
   */
  def add(project: ObjectId, folder: String) = IsMemberOf(project) { _ =>
    implicit request =>
      taskForm.bindFromRequest.fold(
        errors => BadRequest,
        {
          case (title, dueDate, assignedTo, hits, token, header, uris, action, body) =>
            val exist = Task.findByNameAndFolderAndTitle(project, folder, title)
            exist match {
              case Some(x) => {
                Logger.error("updating existing task")
                val updated = Task.updateById(x.id, hits, token match {
                  case Some("token client") => None
                  case Some(x)              => Some(x)
                  case None                 => None
                }, header, uris, action, body)
                Ok(html.tasks.item(updated))
              }
              case None => {
                Logger.error("inserting new task")
                val task = Task.create(
                  Task(new ObjectId, folder, project, title, false, dueDate, assignedTo, hits, token, header, uris, action, body)
                )
                Ok(html.tasks.item(task))
              }
            }

        }

      )
  }

  /**
   * Update a task
   */
  def update(task: ObjectId) = IsOwnerOf(task) { _ =>
    implicit request =>
      Form("done" -> boolean).bindFromRequest.fold(
        errors => BadRequest,
        isDone => {
          Task.markAsDone(task, isDone)
          Ok
        }
      )
  }

  /**
   * Delete a task
   */
  def delete(task: ObjectId) = IsOwnerOf(task) { _ =>
    implicit request =>
      Task.delete(task)
      Ok
  }

  // -- Task folders

  /**
   * Add a new folder.
   */
  def addFolder = Action {
    Ok(html.tasks.folder("NewFolder"))
  }

  /**
   * Delete a full tasks folder.
   */
  def deleteFolder(project: ObjectId, folder: String) = IsMemberOf(project) { _ =>
    implicit request =>
      Task.deleteInFolder(project, folder)
      Ok
  }

  def runTask(task: ObjectId) = IsOwnerOf(task) { _ =>
    implicit request => {
      val taskObj = Task.findById(task).get
      val system = ActorSystem("taskSystem")
      val executionActor = system.actorOf(Props[MasterExecutor], name = "taskExecutionActor")
      executionActor ! Start(taskObj.uris, taskObj.hits.toInt, taskObj.token, taskObj.header, task, taskObj.action, taskObj.body)
      Ok(" done")
    }
  }

  /**
   * Rename a tasks folder.
   */
  def renameFolder(project: ObjectId, folder: String) = IsMemberOf(project) { _ =>
    implicit request =>
      Form("name" -> nonEmptyText).bindFromRequest.fold(
        errors => BadRequest,
        newName => {
          Task.renameFolder(project, folder, newName)
          Ok(newName)
        }
      )
  }

}

