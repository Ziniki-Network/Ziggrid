package org.ziggrid.api;

import java.util.Map;

public interface ExistingObjectProvider {

	StoreableObject canYouProvide(Object inTx, String watchable, Map<String, Object> options);

}