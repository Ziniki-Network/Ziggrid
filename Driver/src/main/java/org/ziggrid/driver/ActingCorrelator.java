package org.ziggrid.driver;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.CorrelatorGlobalFactors.CGFactor;
import org.ziggrid.driver.CorrelatorGlobalFactors.FactorValue;
import org.ziggrid.kvstore.KVStorageEngine;
import org.ziggrid.kvstore.KVStore;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.QueuedItem;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.Grouping;
import org.zinutils.exceptions.NotImplementedException;

public class ActingCorrelator extends ActingProcessor {
	protected static final Logger logger = LoggerFactory.getLogger("Correlator");
	private final CorrelatorGlobalFactors cgf;
	private final String sha;
	private final String produces;
	private final Grouping grp;

	public ActingCorrelator(String sha, KVStorageEngine store, CorrelationDefinition cd, Grouping grp) {
		super(store, "Correlator");
		this.sha = sha;
		cgf = new CorrelatorGlobalFactors(store, sha);
		this.grp = grp;
		this.produces = cd.name + grp.asGroupName();
		addWatchable(this.produces);
	}

	@Override
	public StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options) {
		throw new NotImplementedException("This should be overridden: " + watchable + " in " + this.getClass());
	}

	@Override
	protected void processMessage(KVTransaction tx, KVStore store, QueuedItem qi) {
		String individ = (String) qi.get("_indiv");
		String globid = (String) qi.get("_global");
		double outcome = (Double) qi.get("_outcome");
		
		cgf.record(globid, outcome);
		
		{
			StoreableObject indivFactors;
			String id = "cif/" + sha + "/" + individ + "/" + globid;
			if (tx.contains(id)) {
				indivFactors = tx.get(id);
				indivFactors.set("times", ((Integer)indivFactors.get("times"))+1);
			} else {
				indivFactors = new StoreableObject("corrIndivFactors", id);
				indivFactors.set("global", globid);
				indivFactors.set("times", 1);
			}
			store.write(indivFactors);
		}
		
		{
			List<StoreableObject> allPrefixed = tx.getAllPrefixed("cif/"+sha+"/"+individ);

			double myValue = 0.0;
			int denom = 0;
			for (StoreableObject ind : allPrefixed) {
				CGFactor g = cgf.get((String)ind.get("global"));
				if (g == null)
					continue;
				int times = (Integer)ind.get("times");
				FactorValue fv = g.get();
				if (fv.occurred == 0)
					continue;
				myValue += fv.total*times/(Integer)fv.occurred;
				denom += times;
			}
			myValue /= denom;
			
			String id = "corr/" + sha + "/" + individ;
			StoreableObject output;
			if (tx.contains(id)) {
				output = tx.get(id);
				output.set("_reductionCount", ((Integer)output.get("_reductionCount"))+1);
			} else {
				output = new StoreableObject(produces, id);
				for (String f : grp.fields)
					output.set(f, qi.get(f));
				output.set("_reductionCount", 1);
			}
			output.set("correlation", myValue);
			output.set("count", denom);
			store.write(output);
		}
	}
}
