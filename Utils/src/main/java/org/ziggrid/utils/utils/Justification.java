package org.ziggrid.utils.utils;

public enum Justification {
	LEFT, RIGHT, PADLEFT_TRUNCRIGHT, PADRIGHT_TRUNCLEFT, PADLEFT, PADRIGHT;

	public String format(String text, int len) {
		if (text == null)
			text = "";
		if (len < 0)
			return text;
		int tlen = text.length();
		if (tlen < len)
		{
			switch (this)
			{
			case LEFT:
			case PADRIGHT_TRUNCLEFT:
			case PADRIGHT:
				return text + pad(len-tlen);
			case RIGHT:
			case PADLEFT_TRUNCRIGHT:
			case PADLEFT:
				return pad(len-tlen) + text;
			}
		}
		else
		{
			switch (this)
			{
			case LEFT:
			case PADLEFT_TRUNCRIGHT:
				return text.substring(0, len);
			case RIGHT:
			case PADRIGHT_TRUNCLEFT:
				return text.substring(tlen-len);
			case PADLEFT:
			case PADRIGHT:
				return text;
			}
		}
		return null;
	}

	static String pad(int len) {
		char[] cs = new char[len];
		for (int i=0;i<len;i++)
			cs[i] = ' ';
		return new String(cs);
	}
}
