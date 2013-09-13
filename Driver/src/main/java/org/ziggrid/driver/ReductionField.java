package org.ziggrid.driver;

import java.util.Iterator;

import org.ziggrid.model.FieldDefinition;
import org.ziggrid.model.Reduction;
import org.ziggrid.utils.exceptions.UtilException;

import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

public class ReductionField {
	private final FieldDefinition field;
	private final Iterator<ViewRow> rows;
	private ViewRow row;

	public ReductionField(FieldDefinition field, Reduction value, ViewResponse response) {
		this.field = field;
		this.rows = response.iterator();
	}

	public String nextKey() {
		if (!rows.hasNext())
			return null;
		row = rows.next();
		if (row.getKey() == null)
			return "[]";
		return row.getKey();
	}

	public Object getValue() {
		return correctType(row.getValue());
	}

	private Object correctType(String value) {
		if (field.type.equals("string"))
			return value;
		else if (field.type.equals("number"))
			return Integer.parseInt(value); // TODO: could be float
		else
			throw new UtilException("Cannot reduce type " + value);
	}

}
