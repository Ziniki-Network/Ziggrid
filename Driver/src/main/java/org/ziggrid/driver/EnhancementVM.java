package org.ziggrid.driver;

import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ziggrid.exceptions.ZiggridException;
import org.ziggrid.model.BinaryOperation;
import org.ziggrid.model.DoubleConstant;
import org.ziggrid.model.Enhancement;
import org.ziggrid.model.FieldEnhancement;
import org.ziggrid.model.GroupingOperation;
import org.ziggrid.model.IfElseOperation;
import org.ziggrid.model.IntegerConstant;
import org.ziggrid.model.ListConstant;
import org.ziggrid.model.StringConstant;
import org.ziggrid.model.StringContainsOp;
import org.ziggrid.model.SumOperation;
import org.zinutils.exceptions.UtilException;

public class EnhancementVM {
	public Object process(Enhancement enhancement, JSONObject entry) throws JSONException {
		if (enhancement instanceof FieldEnhancement)
		{
			return entry.get(((FieldEnhancement) enhancement).field);
		} else if (enhancement instanceof IntegerConstant) {
			return ((IntegerConstant)enhancement).value;
		} else if (enhancement instanceof DoubleConstant) {
			return ((DoubleConstant)enhancement).value;
		} else if (enhancement instanceof StringConstant) {
			return ((StringConstant)enhancement).value;
		} else if (enhancement instanceof ListConstant) {
			return ((ListConstant)enhancement).values;
		} else if (enhancement instanceof BinaryOperation) {
			BinaryOperation binop = (BinaryOperation) enhancement;
			Object lhs = process(binop.lhs, entry);
			Object rhs = process(binop.rhs, entry);
			if (binop.op.equals("==")) {
				if (lhs instanceof Integer && rhs instanceof Integer)
					return (int)(Integer)lhs == (int)(Integer)rhs;
				else if (lhs instanceof Number && rhs instanceof Number)
					return number(lhs) == number(rhs);
				else
					return lhs.equals(rhs);
			} else if (binop.op.equals(">=")) {
				if (lhs instanceof Integer && rhs instanceof Integer)
					return (int)(Integer)lhs >= (int)(Integer)rhs;
				else if (lhs instanceof Number && rhs instanceof Number)
					return number(lhs) >= number(rhs);
				else
					throw new UtilException("Cannot compare objects of type " + lhs.getClass() + " and " + rhs.getClass());
			} else if (binop.op.equals("-")) {
				return number(lhs)-number(rhs);
			} else if (binop.op.equals("/")) {
				return number(lhs)/number(rhs);
			} else if (binop.op.equals("in")) {
				if (lhs == null)
					return false;
				@SuppressWarnings("unchecked")
				List<Object> options = (List<Object>) rhs;
				for (Object o : options)
					if (lhs.equals(o))
						return true;
				return false;
			}
			else
				throw new ZiggridException("Binary Operation " + binop.op + " not supported");
		} else if (enhancement instanceof StringContainsOp) {
			StringContainsOp op = (StringContainsOp) enhancement;
			String inside = entry.getString(op.field);
			return inside != null && inside.contains(op.text);
		} else if (enhancement instanceof SumOperation) {
			SumOperation op = (SumOperation) enhancement;
			double accum = 0;
			if (op.op.equals("*"))
				accum = 1;
			for (Enhancement x : op.args) {
				Object o = process(x, entry);
				if (op.op.equals("+"))
					accum += number(o);
				else if (op.op.equals("*"))
					accum *= number(o);
				else
					throw new ZiggridException("Sum Operation " + op.op + " not supported");
			}
			return accum;
		} else if (enhancement instanceof IfElseOperation) {
			IfElseOperation ife = (IfElseOperation) enhancement;
			Object test = process(ife.test, entry);
			boolean b;
			if (test instanceof Boolean)
				b = (Boolean)test;
			else if (test instanceof Integer)
				b = ((Integer)test) != 0;
			else if (test instanceof Double)
				b = ((Double)test) != 0;
			else
				throw new ZiggridException("Cannot handle " + test + " as a boolean");
			if (b)
				return process(ife.ifTrue, entry);
			else
				return process(ife.ifFalse, entry);
		} else if (enhancement instanceof GroupingOperation) {
			GroupingOperation gop = (GroupingOperation) enhancement;
			int base = (int)number(process(gop.basedOn, entry));
			@SuppressWarnings("unchecked")
			List<Integer> obj = (List<Integer>) process(gop.dividers, entry);
			for (int x : obj)
				if (base < x)
					return x;
			return process(gop.moreThan, entry);
		} else
			throw new ZiggridException("Cannot enhance: " + enhancement);
	}

	private double number(Object n) {
		if (n instanceof Integer)
			return (double)(Integer)n;
		else if (n instanceof Double)
			return (Double)n;
		else if (n instanceof String)
			return Double.parseDouble((String)n);
		else
			throw new ZiggridException("Not a number: " + n);
	}
}
