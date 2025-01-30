package com.hjpnam.reviewboard.component

import com.hjpnam.reviewboard.core.ZJS.*
import com.hjpnam.reviewboard.domain.data.CompanyFilter
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec

class FilterPanel:
  case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  private val GROUP_LOCATIONS  = "Locations"
  private val GROUP_COUNTRIES  = "Countries"
  private val GROUP_INDUSTRIES = "Industries"
  private val GROUP_TAGS       = "Tags"

  private val possibleFilter    = EventBus[CompanyFilter]()
  private val checkEvents       = EventBus[CheckValueEvent]()
  private val applyFilterClicks = EventBus[Unit]()
  private val dirty =
    applyFilterClicks.events.mapTo(false).mergeWith(checkEvents.events.mapTo(true))
  private val state: Signal[CompanyFilter] =
    checkEvents.events
      .scanLeft(Map.empty[String, Set[String]])((currentMap, event) =>
        event match
          case CheckValueEvent(groupName, value, checked) =>
            if checked then
              currentMap.updatedWith(groupName)(maybeValueSet =>
                Some(maybeValueSet.fold(Set(value))(_ + value))
              )
            else currentMap.updatedWith(groupName)(_.fold(None)(valueSet => Some(valueSet - value)))
      )
      .map(checkMap =>
        CompanyFilter(
          locations = checkMap.getOrElse(GROUP_LOCATIONS, Set()).toList,
          countries = checkMap.getOrElse(GROUP_COUNTRIES, Set()).toList,
          industries = checkMap.getOrElse(GROUP_INDUSTRIES, Set()).toList,
          tags = checkMap.getOrElse(GROUP_TAGS, Set()).toList
        )
      )

  val appliedFilterStream: EventStream[CompanyFilter] =
    applyFilterClicks.events.withCurrentValueOf(state)

  def apply() =
    div(
      onMountCallback(_ => backendCall(_.company.allFiltersEndpoint(())).emitTo(possibleFilter)),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(GROUP_LOCATIONS, _.locations),
            renderFilterOptions(GROUP_COUNTRIES, _.countries),
            renderFilterOptions(GROUP_INDUSTRIES, _.industries),
            renderFilterOptions(GROUP_TAGS, _.tags),
            renderApplyBtn
          )
        )
      )
    )

  private def renderApplyBtn = {
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- dirty.toSignal(false).map(!_),
        onClick.mapTo(()) --> applyFilterClicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
  }

  def renderFilterOptions(groupName: String, optionsFn: CompanyFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            children <-- possibleFilter.events.map(optionsFn(_).map(renderCheckbox(groupName)))
          )
        )
      )
    )

  private def renderCheckbox(groupName: String)(value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(CheckValueEvent(groupName, value, _)) --> checkEvents
      )
    )

end FilterPanel
