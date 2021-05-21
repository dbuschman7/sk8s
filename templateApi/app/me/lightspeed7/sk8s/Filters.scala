package me.lightspeed7.sk8s

import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter

class Filters @Inject() (
  gzip: GzipFilter,
  response: ResponseFilter //
) extends HttpFilters {

  def filters: Seq[EssentialFilter] = Seq(gzip, response)

}
