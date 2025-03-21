package com.hjpnam.reviewboard.component

import com.hjpnam.reviewboard.page.*
import com.raquo.laminar.api.L.{*, given}
import frontroute.{path, *}

object Router:
  def apply() =
    mainTag(
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("company"))(
            CompanyPage()
          ),
          path("login")(
            LoginPage()
          ),
          path("signup")(
            SignUpPage()
          ),
          path("profile")(
            ProfilePage()
          ),
          path("forgot")(
            ForgotPasswordPage()
          ),
          path("recover")(
            RecoverPasswordPage()
          ),
          path("logout")(
            LogoutPage()
          ),
          noneMatched(
            NotFoundPage()
          )
        )
      )
    )
