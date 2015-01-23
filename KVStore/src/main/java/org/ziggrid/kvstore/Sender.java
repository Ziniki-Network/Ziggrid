package org.ziggrid.kvstore;

import java.util.Map;

public interface Sender {

	void queue(KVTransaction tx, String forProcessorSHA, byte[] writeTo, Map<String, Object> fields);

}
