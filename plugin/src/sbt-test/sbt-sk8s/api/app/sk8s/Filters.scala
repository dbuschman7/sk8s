package me.lightspeed7.sk8s

import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter

class Filters @Inject()(
    gzip: GzipFilter,
    telemetry: TelemetryFilter,
    response: ResponseFilter //
) extends HttpFilters {

  def filters: Seq[EssentialFilter] = Seq(gzip, response, telemetry)

}
