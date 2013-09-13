package org.ziggrid.utils.csv;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;

import org.ziggrid.utils.exceptions.UtilException;

public class CSVReader {
	private InputStream is;
	private LineNumberReader lnr;
	private ArrayList<String> headers;

	public CSVReader(String file) throws FileNotFoundException {
		this(new FileInputStream(file));
	}

	public CSVReader(InputStream is)  {
		this.is = is;
		lnr = new LineNumberReader(new InputStreamReader(is));
	}

	public CSVLine readLine() {
		try {
			String s = lnr.readLine();
			if (s == null)
				 return null;
			
			CSVLine ret = new CSVLine();
			int pos = 0;
			int from = 0;
			boolean inq = false;
			for (int i=0;i<s.length();i++) {
				if (!inq && s.charAt(i) == ',') {
					if (i != from)
						ret.put(pos, s.substring(from, i));
					pos++;
					from = i+1;
				} else if (!inq && i == from && s.charAt(i) == '"') {
					inq = true;
					from++;
				} else if (inq && s.charAt(i) == '"' && (i+1 == s.length() || s.charAt(i+1) == ',')) {
					ret.put(pos, s.substring(from, i));
					pos++;
					i++; // skip the ','
					from = i+1;
					inq = false;
				}
			}
			if (s.length() > from)
				ret.put(pos, s.substring(from));
			return ret;
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public void grabHeaders()
	{
		CSVLine data = readLine();
		headers = new ArrayList<String>();
		for (int i=0;i<data.getCount();i++)
		{
			headers.add(data.get(i));
		}
	}

	public void close() {
		try {
			lnr.close();
			is.close();
		} catch (Exception ex) {
			throw UtilException.wrap(ex);
		}
	}

	public CSVMap mapLine() {
		CSVLine thisLine = readLine();
		if (thisLine == null)
			return null;
		return new CSVMap(headers, thisLine);
	}

	public int lineNo() {
		return lnr.getLineNumber();
	}
}
