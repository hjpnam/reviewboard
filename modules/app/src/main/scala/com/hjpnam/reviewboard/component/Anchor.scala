package com.hjpnam.reviewboard.component

import com.raquo.laminar.api.L.{*, given}

object Anchor:
  def renderNavLink(text: String, location: String, cssClass: String = "") =
    a(
      href := location,
      cls  := cssClass,
      text
    )
