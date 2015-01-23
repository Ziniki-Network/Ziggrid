package org.ziggrid.driver;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ziggrid.api.Definition;
import org.ziggrid.api.IModel;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.enhancers.CompositeDefinitionMapper;
import org.ziggrid.driver.enhancers.CorrelationDefinitionMapper;
import org.ziggrid.driver.enhancers.DefinitionMapper;
import org.ziggrid.driver.enhancers.EnhancementDefinitionMapper;
import org.ziggrid.driver.enhancers.LeaderboardDefinitionMapper;
import org.ziggrid.driver.enhancers.SnapshotDefinitionMapper;
import org.ziggrid.driver.enhancers.SummaryDefinitionMapper;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;
import org.ziggrid.kvstore.StorageMapper;
import org.ziggrid.model.CompositeDefinition;
import org.ziggrid.model.CorrelationDefinition;
import org.ziggrid.model.EnhancementDefinition;
import org.ziggrid.model.Grouping;
import org.ziggrid.model.LeaderboardDefinition;
import org.ziggrid.model.ObjectDefinition;
import org.ziggrid.model.SnapshotDefinition;
import org.ziggrid.model.SummaryDefinition;
import org.zinutils.collections.SetMap;
import org.zinutils.exceptions.UtilException;

/** This is the class that does all the hard work of being an enhancer.
 * For each input record, it generates one queue entry and an enhanced object.
 * 
 * The ActingEnhancer simply reads that queue entry and makes it an object.
 * <p>
 * &copy; 2014 Gareth Powell.  All rights reserved.
 *
 * @author Gareth Powell
 *
 */
public class MappingEnhancer extends StorageMapper {
	private static final Logger logger = LoggerFactory.getLogger("MappingEnhancer");
	private final SetMap<String, DefinitionMapper> enhancers = new SetMap<String, DefinitionMapper>();
	private final EnhancementVM evm;
	
	public MappingEnhancer(IModel m) {
		super(m);
		evm = new EnhancementVM();
		for (String p : model.definitions)
			for (ObjectDefinition k : model.objects(p))
				for (Definition d :  model.willProcess(k.name)) {
					if (d instanceof EnhancementDefinition) {
						EnhancementDefinitionMapper v = new EnhancementDefinitionMapper((EnhancementDefinition) d, model.shaFor(d));
						if (m.restrictionIncludes(v.to))
							enhancers.add(k.name, v);
					} else if (d instanceof SummaryDefinition) {
						// we want one for each level of scale ...
						SummaryDefinition sd = (SummaryDefinition) d;
						for (int s=0;s<=sd.matches.size();s++) {
							SummaryDefinitionMapper v = new SummaryDefinitionMapper(sd, s, model.shaFor(d, "key"+s));
							if (m.restrictionIncludes(v.ofType))
								enhancers.add(k.name, v);
						}
					} else if (d instanceof LeaderboardDefinition) {
						LeaderboardDefinition ld = (LeaderboardDefinition) d;
						for (Grouping g : ld.groupings()) {
							LeaderboardDefinitionMapper v = new LeaderboardDefinitionMapper(ld, g, model.shaFor(d, g.asGroupName()));
							if (m.restrictionIncludes(v.name))
								enhancers.add(k.name, v);
						}
					} else if (d instanceof SnapshotDefinition) {
						SnapshotDefinition sd = (SnapshotDefinition) d;
						SnapshotDefinitionMapper v = new SnapshotDefinitionMapper(sd, model.shaFor(sd));
						if (m.restrictionIncludes(v.myName))
							enhancers.add(k.name, v);
					} else if (d instanceof CompositeDefinition) {
						CompositeDefinition cd = (CompositeDefinition) d;
						CompositeDefinitionMapper v = new CompositeDefinitionMapper(cd, model.shaFor(cd));
						if (m.restrictionIncludes(v.into))
							enhancers.add(k.name, v);
					} else if (d instanceof CorrelationDefinition) {
						CorrelationDefinition cd = (CorrelationDefinition) d;
						for (Grouping g : cd.groupings()) {
							CorrelationDefinitionMapper v = new CorrelationDefinitionMapper(cd, model.shaFor(cd, g.asGroupName()), g);
							if (m.restrictionIncludes(v.name))
								enhancers.add(k.name, v);
						}
					} else
						throw new UtilException("Cannot handle definition of type " + d.getClass());
				}
		
		for (String kv : enhancers.keySet()) {
			logger.info("Enhancers when writing " + kv + ":");
			for (DefinitionMapper defn : enhancers.get(kv))
				logger.info("  " + defn);
		}
	}

	@Override
	public void mapEnqueue(Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) {
		String type = (String) item.get("ziggridType");
		if (!enhancers.contains(type))
			return;
		Set<DefinitionMapper> mappers = enhancers.get(type);
		for (DefinitionMapper dm : mappers)
			try {
				dm.process(evm, sender, tx, prev, item);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}

}
