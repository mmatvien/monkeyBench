package controllers

import org.bson.types.ObjectId
import play.api._
import play.api.mvc._
import views._
import models.Task
import models.Execution
import models.User
import models.Project

object Results extends Controller with Secured {

  /**
   * Display the results panel for this project.
   */
  def index(task: ObjectId, email: String) = IsOwnerOf(task) { _ =>
    implicit request =>
      val executions = Execution.findByTask(task)
      User.findByEmail(email).map { user =>
        Ok(
          html.result(executions,
            Project.findInvolving(email),
            user
          )
        )
      }.getOrElse(Forbidden)

  }
  /**
   * Delete a execution
   */
  def delete(groupId: ObjectId) = Action { implicit request =>
    Execution.deleteGroup(groupId)
    Ok
  }
}