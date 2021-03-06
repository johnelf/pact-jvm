package au.com.dius.pact.model

import au.com.dius.pact.model.JsonDiff.DiffConfig

object ResponseMatching extends ResponseMatching(DiffConfig(allowUnexpectedKeys = true, structural = false))

class ResponseMatching(val providerDiffConfig: DiffConfig) {
  import Matching._

  def matchRules(expected: Response, actual: Response): MatchResult = {
    matchStatus(expected.status, actual.status) and
      matchHeaders(expected.headers, actual.headers) and
      matchBodies(expected.body, actual.body, providerDiffConfig)
  }
}
