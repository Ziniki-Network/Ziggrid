package org.ziggrid.utils.utils;

import java.util.Map;

public class HollerithText extends HollerithItem{
	private final String text;

	public HollerithText(String text) {
		this.text = text;
	}

	@Override
	public String apply(Map<String, String> values) {
		return justify.format(text, width);
	}
}
