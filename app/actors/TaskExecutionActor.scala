package actors

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.Props
import play.api.libs.ws.WS
import play.api.libs.ws.ResponseHeaders
import play.api.libs.iteratee.Iteratee

case class Start(uris: String, hits: Int, token: Option[String], header: Option[String])
case class Result(url: String, timeSpent: Long, status: Int)
case class Execution(url: String)

class MasterExecutor extends Actor with ActorLogging {
  var startTime: Long = _
  var totalTime: Long = _
  var resultCount: Int = 0
  var hits: Int = _
  var urlCount: Int = _

  override def preStart() = {
    startTime = System.currentTimeMillis
  }
  override def postStop() {
    totalTime = System.currentTimeMillis - startTime
    println("master took " + totalTime)
  }

  def receive = {
    case s: Start => {
      hits = s.hits
      val urlSeq = s.uris.split("\n")
      urlCount = urlSeq.length
      var uc = 1
      urlSeq.foreach(url => {
        uc += 1
        for (h <- 0 until hits) {
          val worker = context.actorOf(Props[TaskExecutionActor], "executionActor" + uc + h)
          worker ! Execution(url)
        }
      })

    }
    case result: Result => {
      log.debug(resultCount + " " + result.url + " : " + result.timeSpent + " status: " + result.status)
      resultCount += 1
      if (resultCount == hits * urlCount) context.system.shutdown()
    }
  }
}

class TaskExecutionActor extends Actor with ActorLogging {
  def receive = {
    case e: Execution => {
      val ex = execute(e.url)
      sender ! Result(e.url, ex._1, ex._2)
    }
  }

  def execute(url: String): Tuple2[Long, Int] = {
    // val header = ("Authorization", "BEARER " + token)
    // WS.url(url).withHeaders(header).get().value.get.body

    val headers = ("Cache-Control" -> "no-cache", "Pragma" -> "no-cache")

    val started = System.currentTimeMillis
    val get = WS.url(url).withHeaders("Pragma" -> "no-cache", "Cache-Control" -> "no-cache,max-age=0").get()
    //
    val executionTime = System.currentTimeMillis - started
    // log.debug(get.value.get.getAHCResponse.getHeaders.toString)

    val hp = WS.url("http://amundsen.com/media-types/collection/").
      withHeaders("Accept" -> "application/json").get {
        header: ResponseHeaders =>
          val hdrs = header.headers
          log.debug(hdrs.toString)
          Iteratee.fold[Array[Byte], StringBuffer](new StringBuffer) { (buf, array) => { buf.append(array); buf } }
      }

    (executionTime, get.value.get.status)
  }
}

