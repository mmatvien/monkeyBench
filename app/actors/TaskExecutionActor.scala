package actors

import java.util.Date
import org.bson.types.ObjectId
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import models.Execution
import play.api.libs.ws.WS
import org.apache.http.client.utils.URLEncodedUtils
import java.net.URLEncoder
import java.net.URL
import play.api.libs.concurrent.Promise

case class Start(uris: String, hits: Int, token: Option[String], header: Option[String], task: ObjectId, action: String, body: Option[String])
case class Result(url: String, timeSpent: Long, status: Int, headers: String)
case class ExecutionX(url: String, tokenClient: String, header: Option[String], action: String, body: Option[String])

class MasterExecutor extends Actor with ActorLogging {
  var startTime: Long = _
  var totalTime: Long = _
  var resultCount: Int = 0
  var hits: Int = _
  var urlCount: Int = _
  var task: ObjectId = _
  var groupId = new ObjectId
  var executionTime: Date = _
  var tokenClient: Option[String] = _
  var header: Option[String] = _
  var action: String = _
  var body: Option[String] = _

  override def preStart() = {
    startTime = System.currentTimeMillis

  }
  override def postStop() {
    totalTime = System.currentTimeMillis - startTime
    // println("master took " + totalTime)
  }

  def receive = {
    case s: Start => {
      groupId = new ObjectId
      executionTime = new Date
      hits = s.hits
      val urlSeq = s.uris.split("\n")
      task = s.task
      urlCount = urlSeq.length
      var uc = 1
      tokenClient = s.token
      action = s.action
      body = s.body
      header = s.header

      var tokenHeader = ""

      tokenClient match {
        case Some("token client") =>
        case Some(client)         => tokenHeader = "BEARER " + getToken(client)
        case None                 =>
      }

      urlSeq.foreach(url => {
        uc += 1
        for (h <- 0 until hits) {
          val worker = context.actorOf(Props[TaskExecutionActor], "executionActor" + uc + h)
          worker ! ExecutionX(url, tokenHeader, header, action, body)
        }
      })

    }
    case result: Result => {
      // log.debug(resultCount + " " + result.url + " : " + result.timeSpent + " status: " + result.status)
      resultCount += 1
      Execution.create(Execution(new ObjectId, groupId,
        task, executionTime, result.url, result.headers, result.status.toString, result.timeSpent))

      if (resultCount == hits * urlCount) context.system.shutdown()
    }
  }

  def getToken(clientId: String): String = {
    val url = "https://stg.authorization.go.com/token"

    val postMap = Map(
      "grant_type" -> Seq("password"),
      "affiliate_name" -> Seq("disney"),
      "client_id" -> Seq(clientId),
      "username" -> Seq("warhol"),
      "password" -> Seq("secret"),
      "scope" -> Seq("RETURN_ALL_CLIENT_SCOPES"))

    val res = WS.url(url).post(postMap)
    ((res.value.get.json) \ "access_token").as[String]

  }
}

class TaskExecutionActor extends Actor with ActorLogging {
  def receive = {
    case e: ExecutionX => {
      val ex = execute(e.url, e.header, e.tokenClient, e.action, e.body)
      sender ! Result(e.url, ex._1, ex._2, ex._3)
    }
  }

  def execute(url: String, header: Option[String], tokenClient: String, action: String, body: Option[String]): Tuple3[Long, Int, String] = {
    val started = System.currentTimeMillis

    var headersX = ("", "")

    header match {
      case Some(headers) => {
        headersX = headers.split("\n").foldLeft("", "")((s, op) => (op.split(":")(0), op.split(":")(1)))
        log.error("headers : " + headersX)
      }
      case None =>
    }

    // val headers = constructHeaders(tokenClient, header)

   log.error("action"+action)
    
    action match {
      case "GET" => {
         log.error("TOKEN: " + tokenClient)
        val get = WS.url(url).withHeaders("Authorization" -> tokenClient).get()
        
        get.await(100000)
        
        val executionTime = System.currentTimeMillis - started
        (executionTime, get.value.get.status, get.value.get.getAHCResponse.getHeaders.toString)
      }
      case "POST" => {
        val get = WS.url(url).withHeaders(headersX).post("")
        val executionTime = System.currentTimeMillis - started
        (executionTime, get.value.get.status, get.value.get.getAHCResponse.getHeaders.toString)
      }
    }
  }

  def constructHeaders(token: String, header: Option[String]): (String, String) = {
    ("Authorization" -> token)
  }

}

