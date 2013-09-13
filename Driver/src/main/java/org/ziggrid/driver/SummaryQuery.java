package org.ziggrid.driver;

import org.ziggrid.model.SummaryDefinition;

public class SummaryQuery {
	public final SummaryDefinition defn;
	public final CouchQuery query;

	public SummaryQuery(SummaryDefinition defn, CouchQuery query) {
		this.defn = defn;
		this.query = query;
	}
}
