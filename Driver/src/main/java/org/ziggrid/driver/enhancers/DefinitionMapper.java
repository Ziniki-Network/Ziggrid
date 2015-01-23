package org.ziggrid.driver.enhancers;

import org.codehaus.jettison.json.JSONException;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.driver.EnhancementVM;
import org.ziggrid.kvstore.KVTransaction;
import org.ziggrid.kvstore.Sender;

public interface DefinitionMapper {

	void process(EnhancementVM evm, Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item) throws JSONException;

}
