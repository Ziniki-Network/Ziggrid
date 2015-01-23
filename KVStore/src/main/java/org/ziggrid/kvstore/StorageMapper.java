package org.ziggrid.kvstore;

import org.ziggrid.api.IModel;
import org.ziggrid.api.StoreableObject;
import org.ziggrid.model.Model;

public abstract class StorageMapper {
	protected final Model model;

	public StorageMapper(IModel model) {
		this.model = (Model) model;
	}

	public abstract void mapEnqueue(Sender sender, KVTransaction tx, StoreableObject prev, StoreableObject item);
}
