package com.hjpnam.reviewboard.component

import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.domain.data.Review
import com.hjpnam.reviewboard.http.request.CreateReviewRequest
import com.raquo.laminar.api.L.{*, given}
import zio.ZIO

class AddReviewCard(companyId: Long, onDisable: () => Unit, triggerBus: EventBus[Unit]):
  final case class State(
      review: Review = Review.empty(companyId),
      showErrors: Boolean = false,
      upstreamError: Option[String] = None
  )

  val stateVar = Var(State())

  def apply() =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description add-review",
          div(
            // score dropdowns
            div(
              cls := "add-review-scores",
              renderDropdown("Would recommend", (r, score) => r.copy(wouldRecommend = score)),
              renderDropdown("Management", (r, score) => r.copy(management = score)),
              renderDropdown("Culture", (r, score) => r.copy(culture = score)),
              renderDropdown("Salaries", (r, score) => r.copy(salaries = score)),
              renderDropdown("Benefits", (r, score) => r.copy(benefits = score))
            ),
            // text area for the text review
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> stateVar.updater[String]((s, value) =>
                  s.copy(review = s.review.copy(review = value))
                )
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              onClick --> (_ => onDisable()),
              "Cancel"
            ),
            child.maybe <-- stateVar.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(_.map(renderError))
          )
        )
      )
    )

  private def renderDropdown(name: String, updateFn: (Review, Int) => Review) =
    val selectorId = name.split(" ").map(_.toLowerCase).mkString("-")
    div(
      cls := "add-review-score",
      label(forId := selectorId, s"$name:"),
      select(
        idAttr := selectorId,
        (1 to 5).reverse.map(v =>
          option(
            v.toString
          )
        ),
        onChange.mapToValue --> stateVar.updater[String]((s, value) =>
          s.copy(review = updateFn(s.review, value.toInt))
        )
      )
    )

  private def renderError(error: String) = div(
    cls := "page-status-errors",
    error
  )

  val submitter = Observer[State] { state =>
    if state.upstreamError.isDefined then stateVar.update(_.copy(showErrors = true))
    else
      backendCall(_.review.createEndpoint(CreateReviewRequest.fromReview(state.review))).unit
        .tapBoth(
          e =>
            ZIO.succeed(
              stateVar.update(_.copy(showErrors = true, upstreamError = Some(e.getMessage)))
            ),
          _ => ZIO.succeed(onDisable())
        )
        .emitTo(triggerBus)
  }
