package cromwell.pipeline.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.dimafeng.testcontainers.{ ForAllTestContainer, PostgreSQLContainer }
import com.typesafe.config.Config
import cromwell.pipeline.datastorage.dto.{ User, UserNoCredentials }
import cromwell.pipeline.utils.auth.{ AccessTokenContent, TestContainersUtils, TestUserUtils }
import cromwell.pipeline.ApplicationComponents
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{ AsyncWordSpec, Matchers }

class UserControllerItTest
    extends AsyncWordSpec
    with Matchers
    with ScalatestRouteTest
    with PlayJsonSupport
    with ForAllTestContainer {

  override val container: PostgreSQLContainer = TestContainersUtils.getPostgreSQLContainer()
  container.start()
  implicit val config: Config = TestContainersUtils.getConfigForPgContainer(container)
  private val components: ApplicationComponents = new ApplicationComponents()

  override protected def beforeAll(): Unit =
    components.datastorageModule.pipelineDatabaseEngine.updateSchema()

  import components.controllerModule.userController
  import components.datastorageModule.userRepository

  "UserController" when {

    "getUsersByEmail" should {

      "should find newly added user by email pattern" in {

        val dummyUser: User = TestUserUtils.getDummyUser()
        val userByEmailRequest: String = dummyUser.email
        val seqUser: Seq[User] = Seq(dummyUser)
        userRepository.addUser(dummyUser).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId.value)
          Get("/users?email=" + userByEmailRequest) ~> userController.route(accessToken) ~> check {
            status shouldBe StatusCodes.OK
            responseAs[Seq[User]] shouldEqual seqUser
          }
        }
      }
    }

    "deactivateUserById" should {
      "return user's entity with false value if user was successfully deactivated" in {
        val dummyUser: User = TestUserUtils.getDummyUser()
        val deactivatedUserResponse = UserNoCredentials.fromUser(dummyUser.copy(active = false))
        userRepository.addUser(dummyUser).map { _ =>
          val accessToken = AccessTokenContent(dummyUser.userId.value)
          Delete("/users") ~> userController.route(accessToken) ~> check {
            responseAs[UserNoCredentials] shouldBe deactivatedUserResponse
            status shouldBe StatusCodes.OK
          }
        }
      }
    }
  }
}
