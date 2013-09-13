package org.ziggrid.utils.serialization;

import java.io.Serializable;


public interface ControlRequest extends Serializable {

	public Serializable execute(SerializedControllerConnection controller);

	public boolean waitForResponse();

}
