package controllers

import akka.actor.ActorSystem
import argonaut.Argonaut._
import argonaut.Json
import models.{GroupActor, UserActor}
import persistence.{GroupMongoPersistence, UserMongoPersistence}
import play.api.mvc._
import utils.ActorUtils._
import utils.Helpers.{GET, DELETE, GETS, POST}

object Application extends Controller {

  implicit var system: ActorSystem = null
  system = ActorSystem("Fracker")

  val user = system.actorOf(UserMongoPersistence.props(), name="UserActor")
  val group = system.actorOf(GroupMongoPersistence.props(), name="GroupActor")

  val mUser = system.actorOf(UserActor.props(user), name = "UserModelActor")
  val mGroup = system.actorOf(GroupActor.props(group), name = "GroupModelActor")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def getUser(id: String) = Action {
    val answer = await[Json](mUser, GET(id))
    Ok(answer.toString()).as("application/json")
  }

  def getUsers = Action {
    val answer = await[Json](mUser, GETS(None, None))
    Ok(answer.toString()).as("application/json")
  }

  def createUser = Action { request =>
    request.body.asText match {
      case Some(json) =>
        val answer = await[Json](mUser, POST(json)) // TODO: decode user response to return better message codes --- change Json to Try[Json]??
        Status(201)(answer.toString())
      case None =>
        val answer = Json("error" -> jString("No data to parse"))
        Status(406)(answer.toString())
    }
    //Ok(answer.toString()).as("application/json")
  }

  def deleteUser(id: String) = Action { request =>
    await[Boolean](mUser, DELETE(id)) match {
      case true => Status(200)("User deleted successfully")
      case false => Status(503)("Failed to delete user")    // 500?
    }
  }

  def getGroup(id: String) = Action {
    val answer = await[Json](mGroup, GET(id))
    Ok(answer.toString()).as("application/json")
  }

  def getGroups = Action {  //TODO: POST must come with user !!_id!! or token
    val answer = await[Json](mGroup, GETS(None, None))
    Ok(answer.toString()).as("application/json")
  }

  def createGroup = Action { request =>
    val answer = request.body.asText match {
      case Some(json) => await[Json](mGroup, POST(json))
      case None => Json("error" -> jString("No data to parse"))
    }
    Ok(answer.toString()).as("application/json")
  }

  def getUserGroups(id: String) = Action { request =>
    Ok(Json("id" -> jString(id)).toString()).as("application/json")
  }

  def deleteGroup(id: String) = Action { request =>
    await[Boolean](mGroup, DELETE(id)) match {
      case true => Status(200)("Group deleted successfully")
      case false => Status(503)("Failed to delete group")    // 500?
    }
  }

}