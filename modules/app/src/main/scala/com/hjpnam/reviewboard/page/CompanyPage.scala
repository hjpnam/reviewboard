package com.hjpnam.reviewboard.page

import com.hjpnam.reviewboard.component.{AddReviewCard, Markdown, Time}
import org.scalajs.dom
import com.hjpnam.reviewboard.component.CompanyComponents.{renderCompanyImg, renderCompanyOverview}
import com.hjpnam.reviewboard.core.Session
import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.domain.data.{Company, Review, UserToken}
import com.raquo.laminar.DomApi
import com.raquo.laminar.api.L.{*, given}

object CompanyPage:
  enum Status:
    case LOADING
    case NOT_FOUND
    case OK(company: Company)

  private val addReviewCardActive = Var(false)
  private val fetchCompanyBus     = EventBus[Option[Company]]()
  private val triggerRefreshBus   = EventBus[Unit]()

  private def refreshReviewList(companyId: Long): EventStream[List[Review]] =
    backendCall(_.review.getByCompanyIdEndpoint(companyId)).toEventStream.mergeWith(
      triggerRefreshBus.events.flatMapSwitch(_ =>
        backendCall(_.review.getByCompanyIdEndpoint(companyId)).toEventStream
      )
    )

  private def reviewsSignal(companyId: Long): Signal[List[Review]] = fetchCompanyBus.events
    .flatMapSwitch(
      _.fold(EventStream.empty)(_ => refreshReviewList(companyId))
    )
    .scanLeft(List.empty[Review])((_, list) => list)

  val status = fetchCompanyBus.events.scanLeft(Status.LOADING)((_, maybeCompany) =>
    maybeCompany.fold(Status.NOT_FOUND)(Status.OK.apply)
  )

  def apply(companyId: Long) = div(
    cls := "container-fluid the-rock",
    onMountCallback(_ =>
      backendCall(_.company.getByIdEndpoint(companyId.toString)).emitTo(fetchCompanyBus)
    ),
    child <-- status.map {
      case Status.LOADING     => div("loading...")
      case Status.NOT_FOUND   => div("company not found")
      case Status.OK(company) => static(company, reviewsSignal(companyId))
    }
  )

  def static(company: Company, reviewsSignal: Signal[List[Review]]) =
    div(
      cls := "container-fluid the-rock",
      div(
        cls := "row jvm-companies-details-top-card",
        div(
          cls := "col-md-12 p-0",
          div(
            cls := "jvm-companies-details-card-profile-img",
            renderCompanyImg(company)
          ),
          div(
            cls := "jvm-companies-details-card-profile-title",
            h1(company.name),
            div(
              cls := "jvm-companies-details-card-profile-company-details-company-and-location",
              renderCompanyOverview(company)
            )
          ),
          child <-- Session.userState.signal.map(userToken =>
            maybeRenderUserAction(userToken, reviewsSignal)
          )
        )
      ),
      div(
        cls := "container-fluid",
        renderCompanySummary,
        child.maybe <-- addReviewCardActive.signal
          .map(active =>
            Option.when(active)(
              AddReviewCard(
                company.id,
                () => addReviewCardActive.set(false),
                triggerRefreshBus
              )()
            )
          ),
        children <-- reviewsSignal.map(_.map(renderReview)),
        div(
          cls := "container",
          div(
            cls := "rok-last",
            div(
              cls := "row invite-row",
              div(
                cls := "col-md-6 col-sm-6 col-6",
                span(
                  cls := "rock-apply",
                  p("Do you represent this company?"),
                  p("Invite people to leave reviews.")
                )
              ),
              div(
                cls := "col-md-6 col-sm-6 col-6",
                a(
                  href   := company.url,
                  target := "blank",
                  button(`type` := "button", cls := "rock-action-btn", "Invite people")
                )
              )
            )
          )
        )
      )
    )

  def maybeRenderUserAction(maybeUser: Option[UserToken], reviewsSignal: Signal[List[Review]]) =
    maybeUser.fold(
      div(
        cls := "jvm-companies-details-card-apply-now-btn",
        "You must log in to post a review."
      )
    )(user =>
      div(
        cls := "jvm-companies-details-card-apply-now-btn",
        child <-- reviewsSignal
          .map(_.find(_.userId == user.id))
          .map(
            _.fold(
              button(
                `type` := "button",
                cls    := "btn btn-warning",
                "Add a review",
                disabled <-- addReviewCardActive.signal,
                onClick.mapTo(true) --> addReviewCardActive.writer
              )
            )(_ => div("You've already posted a review."))
          )
      )
    )

  def renderCompanySummary =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description",
          "TODO company summary"
        )
      )
    )

  def renderReview(review: Review) =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        cls("review-highlighted") <-- Session.userState.signal
          .map(_.map(_.id) == Option(review).map(_.userId)),
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderReviewDetail("Would Recommend", review.wouldRecommend),
            renderReviewDetail("Management", review.management),
            renderReviewDetail("Culture", review.culture),
            renderReviewDetail("Salary", review.salaries),
            renderReviewDetail("Benefits", review.benefits)
          ),
          // TODO parse this Markdown
          div(
            cls := "review-content",
            injectMarkdown(review.review)
          ),
          div(cls := "review-posted", s"Posted ${Time.unix2Hr(review.created.toEpochMilli)}"),
          child.maybe <-- Session.userState.signal
            .map(_.filter(_.id == review.userId))
            .map(_.map(_ => div(cls := "review-posted", "Your review")))
        )
      )
    )

  def renderReviewDetail(detail: String, score: Int) =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )

  private def injectMarkdown(markdown: String) =
    DomApi
      .unsafeParseHtmlStringIntoNodeArray(Markdown.toHtml(markdown))
      .map {
        case t: dom.Text         => span(t.data)
        case e: dom.html.Element => foreignHtmlElement(e)
        case _                   => emptyNode
      }
