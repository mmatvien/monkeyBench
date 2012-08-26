import com.mongodb.casbah.Imports._
import play.api._
import models._
import org.apache.commons.codec.digest.DigestUtils._

object Global extends GlobalSettings {

  private def hash(pass: String, salt: String): String = sha256Hex(salt.padTo('0', 256) + pass)

  override def onStart(app: Application) {

      def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

    if (User.fetchAll.isEmpty) {

      User.create(User(new ObjectId, "Ryan.X.Bodolay.-ND@disney.com", "Ryan Bodolay", "secret"))
      User.create(User(new ObjectId, "Cliffton.X.Goh.-ND@disney.com", "Cliffton Goh", "secret"))
      User.create(User(new ObjectId, "mmatvien@gmail.com", "Maxim Matvienko", "freed0M11"))
      User.create(User(new ObjectId, "Matt.J.Obrion.-ND@disney.com", "Matt Obrion", "secret"))

      val projectId = new ObjectId

      Seq(
        Project(projectId, "UI", "PEP") -> Seq("mmatvien@gmail.com", "Matt.J.Obrion.-ND@disney.com", "Cliffton.X.Goh.-ND@disney.com", "Ryan.X.Bodolay.-ND@disney.com")
      ).foreach {
          case (project, members) => Project.create(project, members)
        }

    }

  }

}