package io.sherpair.w4s.config

import io.sherpair.w4s.domain.Analyzer

case class Suggestions(analyzer: Analyzer, fuzziness: Int, maxSuggestions: Int)
