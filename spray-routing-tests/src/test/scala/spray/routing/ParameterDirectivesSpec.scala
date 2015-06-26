/*
 * Copyright © 2011-2015 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package spray.routing

import shapeless.HNil

class ParameterDirectivesSpec extends RoutingSpec {

  "when used with 'as[Int]' the parameter directive" should {
    "extract a parameter value as Int (using the general `parameters` directive)" in {
      Get("/?amount=123") ~> {
        parameters('amount.as[Int] :: HNil) {
          echoComplete
        }
      } ~> check {
        responseAs[String] === "123"
      }
    }
    "extract a parameter values as Int (using the `parameter` directive)" in {
      Get("/?amount=123") ~> {
        parameter('amount.as[Int]) {
          echoComplete
        }
      } ~> check {
        responseAs[String] === "123"
      }
    }
    "cause a MalformedQueryParamRejection on illegal Int values" in {
      Get("/?amount=1x3") ~> {
        parameter('amount.as[Int]) {
          echoComplete
        }
      } ~> check {
        rejection must beLike {
          case MalformedQueryParamRejection("amount", "'1x3' is not a valid 32-bit integer value", Some(_)) ⇒ ok
        }
      }
    }
    "supply typed default values" in {
      Get() ~> {
        parameter('amount ? 45) {
          echoComplete
        }
      } ~> check {
        responseAs[String] === "45"
      }
    }
    "create typed optional parameters that" in {
      "extract Some(value) when present" in {
        Get("/?amount=12") ~> {
          parameter("amount".as[Int] ?) {
            echoComplete
          }
        } ~> check {
          responseAs[String] === "Some(12)"
        }
      }
      "extract None when not present" in {
        Get() ~> {
          parameter("amount".as[Int] ?) {
            echoComplete
          }
        } ~> check {
          responseAs[String] === "None"
        }
      }
      "cause a MalformedQueryParamRejection on illegal Int values" in {
        Get("/?amount=x") ~> {
          parameter("amount".as[Int] ?) {
            echoComplete
          }
        } ~> check {
          rejection must beLike {
            case MalformedQueryParamRejection("amount", "'x' is not a valid 32-bit integer value", Some(_)) ⇒ ok
          }
        }
      }
    }
  }

  "when used with 'as(HexInt)' the parameter directive" should {
    import spray.httpx.unmarshalling.FromStringDeserializers.HexInt
    "extract parameter values as Int" in {
      Get("/?amount=1f") ~> {
        parameter('amount.as(HexInt)) {
          echoComplete
        }
      } ~> check {
        responseAs[String] === "31"
      }
    }
    "cause a MalformedQueryParamRejection on illegal Int values" in {
      Get("/?amount=1x3") ~> {
        parameter('amount.as(HexInt)) {
          echoComplete
        }
      } ~> check {
        rejection must beLike {
          case MalformedQueryParamRejection("amount",
            "'1x3' is not a valid 32-bit hexadecimal integer value", Some(_)) ⇒ ok
        }
      }
    }
    "supply typed default values" in {
      Get() ~> {
        parameter('amount.as(HexInt) ? 45) {
          echoComplete
        }
      } ~> check {
        responseAs[String] === "45"
      }
    }
    "create typed optional parameters that" in {
      "extract Some(value) when present" in {
        Get("/?amount=A") ~> {
          parameter("amount".as(HexInt) ?) {
            echoComplete
          }
        } ~> check {
          responseAs[String] === "Some(10)"
        }
      }
      "extract None when not present" in {
        Get() ~> {
          parameter("amount".as(HexInt) ?) {
            echoComplete
          }
        } ~> check {
          responseAs[String] === "None"
        }
      }
      "cause a MalformedQueryParamRejection on illegal Int values" in {
        Get("/?amount=x") ~> {
          parameter("amount".as(HexInt) ?) {
            echoComplete
          }
        } ~> check {
          rejection must beLike {
            case MalformedQueryParamRejection("amount",
              "'x' is not a valid 32-bit hexadecimal integer value", Some(_)) ⇒ ok
          }
        }
      }
    }
  }

  "The 'parameters' extraction directive" should {
    "extract the value of given parameters" in {
      Get("/?name=Parsons&FirstName=Ellen") ~> {
        parameters("name", 'FirstName) { (name, firstName) ⇒
          complete(firstName + name)
        }
      } ~> check {
        responseAs[String] === "EllenParsons"
      }
    }
    "extract the value of given parameters (with designated unmarshaller)" in {
      Get("/?x=on&y=off") ~> {
        parameters('x.as[Boolean], 'y.as[Boolean]) { (x, y) ⇒
          complete(x.toString + y)
        }
      } ~> check {
        responseAs[String] === "truefalse"
      }
    }
    "correctly extract an optional parameter" in {
      Get("/?foo=bar") ~> parameters('foo ?) {
        echoComplete
      } ~> check {
        responseAs[String] === "Some(bar)"
      }
      Get("/?foo=bar") ~> parameters('baz ?) {
        echoComplete
      } ~> check {
        responseAs[String] === "None"
      }
    }
    "ignore additional parameters" in {
      Get("/?name=Parsons&FirstName=Ellen&age=29") ~> {
        parameters("name", 'FirstName) { (name, firstName) ⇒
          complete(firstName + name)
        }
      } ~> check {
        responseAs[String] === "EllenParsons"
      }
    }
    "reject the request with a MissingQueryParamRejection if a required parameters is missing" in {
      Get("/?name=Parsons&sex=female") ~> {
        parameters('name, 'FirstName, 'age) { (name, firstName, age) ⇒
          completeOk
        }
      } ~> check {
        rejection === MissingQueryParamRejection("FirstName")
      }
    }
    "supply the default value if an optional parameter is missing" in {
      Get("/?name=Parsons&FirstName=Ellen") ~> {
        parameters("name" ?, 'FirstName, 'age ? "29", 'eyes ?) { (name, firstName, age, eyes) ⇒
          complete(firstName + name + age + eyes)
        }
      } ~> check {
        responseAs[String] === "EllenSome(Parsons)29None"
      }
    }
    "supply the default value if an optional parameter is missing (with the general `parameters` directive)" in {
      Get("/?name=Parsons&FirstName=Ellen") ~> {
        parameters(("name" ?) :: 'FirstName :: ('age ? "29") :: ('eyes ?) :: HNil) { (name, firstName, age, eyes) ⇒
          complete(firstName + name + age + eyes)
        }
      } ~> check {
        responseAs[String] === "EllenSome(Parsons)29None"
      }
    }
  }

  "The 'parameter' requirement directive" should {
    "block requests that do not contain the required parameter" in {
      Get("/person?age=19") ~> {
        parameter('nose ! "large") {
          completeOk
        }
      } ~> check {
        handled must beFalse
      }
    }
    "block requests that contain the required parameter but with an unmatching value" in {
      Get("/person?age=19&nose=small") ~> {
        parameter('nose ! "large") {
          completeOk
        }
      } ~> check {
        handled must beFalse
      }
    }
    "let requests pass that contain the required parameter with its required value" in {
      Get("/person?nose=large&eyes=blue") ~> {
        parameter('nose ! "large") {
          completeOk
        }
      } ~> check {
        response === Ok
      }
    }
    "be useable for method tunneling" in {
      val route = {
        (post | parameter('method ! "post")) {
          complete("POST")
        } ~
          get {
            complete("GET")
          }
      }
      Get("/?method=post") ~> route ~> check {
        responseAs[String] === "POST"
      }
      Post() ~> route ~> check {
        responseAs[String] === "POST"
      }
      Get() ~> route ~> check {
        responseAs[String] === "GET"
      }
    }
  }

  case class Person(name: String, age: Int)

  "The 'rejectIfUnmatchedParamsFound' directive" should {
    "block request that has unmatched parameters" in {
      Get("/person?age=19&address=donotwant") ~> {
        parameter("age") { age ⇒
          rejectIfUnmatchedParamsFound { completeOk }
        }
      } ~> check {
        handled must beFalse
        rejection must beEqualTo(NotAllowedQueryParamRejection("address"))
      }

      Get("/person?name=blah&age=19&address=donotwant") ~> {
        parameter("name") { address ⇒
          parameter("age") { age ⇒
            rejectIfUnmatchedParamsFound { completeOk }
          }
        }
      } ~> check {
        handled must beFalse
        rejection must beEqualTo(NotAllowedQueryParamRejection("address"))
      }

      Get("/person?age=19&name=blah&address=donotwant") ~> {
        parameters('name, 'age).as(Person) { _ ⇒
          rejectIfUnmatchedParamsFound { completeOk }
        }
      } ~> check {
        handled must beFalse
        rejection must beEqualTo(NotAllowedQueryParamRejection("address"))
      }
    }
  }

  "The 'rejectIfUnmatchedParamsFound' directive" should {
    "not block request that has not unmatched parameters" in {
      Get("/person?age=19") ~> {
        parameter("age") { age ⇒
          rejectIfUnmatchedParamsFound { completeOk }
        }
      } ~> check { handled must beTrue }

      Get("/person?name=blah&age=19") ~> {
        parameter("name") { address ⇒
          parameter("age") { age ⇒
            rejectIfUnmatchedParamsFound { completeOk }
          }
        }
      } ~> check { handled must beTrue }

      Get("/person?age=19&name=blah") ~> {
        parameters('name, 'age).as(Person) { _ ⇒
          rejectIfUnmatchedParamsFound { completeOk }
        }
      } ~> check { handled must beTrue }
    }
  }
}