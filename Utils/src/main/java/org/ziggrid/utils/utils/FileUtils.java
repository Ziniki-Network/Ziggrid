package org.ziggrid.utils.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ziggrid.utils.exceptions.NoSuchDirectoryException;
import org.ziggrid.utils.exceptions.ResourceNotFoundException;
import org.ziggrid.utils.exceptions.UtilException;

public class FileUtils {

	public static class GlobFilter implements FileFilter {
		private final String pattern;
		private final Collection<File> includeOnlyDirs;
		private final Collection<File> excludeOnlyDirs;
		private final File rootdir;

		public GlobFilter(File file, String pattern, Collection<File> includeOnlyDirs, Collection<File> excludeOnlyDirs) {
			this.rootdir = relativePath(file);
			this.pattern = pattern;
			this.includeOnlyDirs = roundUp(includeOnlyDirs);
			this.excludeOnlyDirs = roundUp(excludeOnlyDirs);
		}

		private Collection<File> roundUp(Collection<File> dirs) {
			if (dirs == null)
				return null;
			List<File> ret = new ArrayList<File>();
			for (File f : dirs)
			{
				File from = relativePath(rootdir, f.getPath());
				if (!from.isDirectory())
					continue;
				ret.add(makeRelativeTo(from, rootdir));
				for (File d : findDirectoriesUnder(from))
					ret.add(makeRelativeTo(relativePath(from, d.getPath()), rootdir));
			}
			return ret;
		}

		@Override
		public boolean accept(File f) {
			File relativeParent = makeRelativeTo(f.getParentFile(), rootdir);
			return StringUtil.globMatch(pattern, f.getName()) &&
			(includeOnlyDirs == null || includeOnlyDirs.contains(relativeParent)) &&
			(excludeOnlyDirs == null || !excludeOnlyDirs.contains(relativeParent));
		}
	}

	private static FileFilter isdirectory = new FileFilter() {
		@Override
		public boolean accept(File path) {
			return path.isDirectory();
		}
	};

	private static FileFilter anyFile = new FileFilter() {
		@Override
		public boolean accept(File dir) {
			return true;
		}
	};

	private static Comparator<? super File> filePathComparator = new Comparator<File>() {

		@Override
		public int compare(File o1, File o2) {
			if (o1.getPath().length() > o2.getPath().length())
				return -1;
			else if (o1.getPath().length() == o2.getPath().length())
				return 0;
			else
				return 1;
		}
	};
	
	private static File root = new File(System.getProperty("user.dir"));

	public static void chdirAbs(File absFile) {
		try
		{
			if (!absFile.isDirectory())
				throw new UtilException("Cannot have " + root + " be the root directory, because it does not exist");
			root = absFile;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}


	public static void chdir(File parentFile) {
		try
		{
			File changeTo = new File(root, parentFile.getPath()).getCanonicalFile();
			if (!changeTo.isDirectory())
				throw new UtilException("Cannot have " + changeTo + " be the root directory, because it does not exist");
			root = changeTo;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static File relativePath(String string) {
		return new File(root, string);
	}
	
	public static File relativePath(File f) {
		if (f.isAbsolute())
			return f;
		else
		{
			try {
				return new File(root, f.getPath()).getCanonicalFile();
			}
			catch (Exception ex)
			{
				throw UtilException.wrap(ex);
			}
		}
	}
	
	public static File relativePath(File qbdir, String string) {
		if (qbdir == null)
			return new File(string);
		else if (qbdir.isAbsolute())
			return new File(qbdir, string);
		else
			return relativePath(new File(qbdir, string).getPath());
	}

	public static File makeRelative(File f) {
		return makeRelativeTo(f, root);
	}
	
	public static File makeRelativeTo(File f, File under) {
		if (under == null)
			return f;
		String uf = under.getPath();
		String ufSlash = uf + File.separator;
		
		// Try the "text" path ...
		String tf = f.getPath();
		if (uf.equals(tf))
			return new File("");
		if (tf.startsWith(ufSlash))
			return new File(tf.substring(ufSlash.length()));
		
		// Try the "canonical" path
		try {
			tf = f.getCanonicalPath();
		} catch (IOException e) {
			throw new UtilException("The path " + f + " does not exist");
		}
		if (uf.equals(tf))
			return new File("");
		if (tf.startsWith(ufSlash))
			return new File(tf.substring(ufSlash.length()));
		
		throw new RuntimeException("This case is not handled: " + tf + " is not a subdir of " + uf);
	}

	// TODO: this should consider all possible breakups based on -
	public static File findDirectoryNamed(String projectName) {
		File ret = new File(root, projectName);
		if (ret.isDirectory())
			return ret;
		if ((ret = findDirNamedRecursive(root, projectName)) != null)
			return ret;
		throw new UtilException("There is no project directory: " + projectName);
	}
	
	private static File findDirNamedRecursive(File base, String remaining) {
		System.out.println("Looking for " + remaining + " in " + base);
		int idx = -1;
		do
		{
			idx = remaining.indexOf("-", idx+1);
			File rec;
			if (idx == -1)
				rec = new File(base, remaining);
			else
				rec = new File(base, remaining.substring(0, idx));
			if (rec.isDirectory())
			{
				if (idx == -1)
					return rec;
				File ret = findDirNamedRecursive(rec, remaining.substring(idx+1));
				if (ret != null)
					return ret;
			}
		} while (idx != -1);
		return null;
	}

	// TODO: this feels very functional in its combinations of things
	public static List<File> findFilesMatchingIncluding(File dir, String string, List<File> includePackages) {
		return findFiles(dir, null, string, includePackages, null);
	}

	public static List<File> findFilesMatchingExcluding(File dir, String string, List<File> excludePackages) {
		return findFiles(dir, null, string, null, excludePackages);
	}

	public static List<File> findFilesMatching(File file, String string) {
		return findFiles(file, null, string, null, null);
	}

	public static List<File> findFilesUnderMatching(File file, String string) {
		return findFiles(file, file, string, null, null);
	}

	public static List<File> findFilesUnderRelativeToMatching(File file, File relativeTo, String string) {
		return findFiles(file, relativeTo, string, null, null);
	}

	private static List<File> findFiles(File file, File under, String string, Collection<File> includeOnlyDirs, Collection<File> excludeOnlyDirs) {
		List<File> ret = new ArrayList<File>();
		if (!file.exists())
			throw new NoSuchDirectoryException("There is no file " + file);
		FileFilter filter = new GlobFilter(file, string, includeOnlyDirs, excludeOnlyDirs);
		findRecursive(ret, filter, under, file);
		return ret;
	}

	public static List<File> findDirectoriesUnder(File dir) {
		List<File> ret = new ArrayList<File>();
		if (!dir.exists())
			throw new NoSuchDirectoryException("There is no file " + dir);
		findRecursive(ret, isdirectory, dir, dir);
		return ret;
	}

	private static void findRecursive(List<File> ret, FileFilter filter, File under, File dir) {
		File[] contents = dir.listFiles(filter);
		if (contents == null)
			return;
		for (File f : contents)
			ret.add(makeRelativeTo(f, under));
		File[] subdirs = dir.listFiles(isdirectory);
		for (File d : subdirs)
			findRecursive(ret, filter, under, d);
	}

	public static String convertToDottedName(File path) {
		if (path.getParent() == null)
			return dropExtension(path.getName());
		return convertToDottedName(path.getParentFile()) + "." + path.getName();
	}

	public static String convertToDottedNameDroppingExtension(File path) {
		if (path.getParent() == null)
			return dropExtension(path.getName());
		return convertToDottedName(path.getParentFile()) + "." + dropExtension(path.getName());
	}

	public static String dropExtension(String name) {
		int idx = name.indexOf('.');
		if (idx == -1)
			return name;
		return name.substring(0, idx);
	}

	public static File mavenToFile(String pkginfo) {
		String[] spl = pkginfo.split(":");
		if (spl == null || spl.length != 4)
			throw new UtilException("'" + pkginfo + "' is not a valid maven package name");
		return fileConcat(convertDottedToPath(spl[0]).getPath(), spl[1], spl[3], spl[1]+"-"+spl[3]+"."+spl[2]);
	}


	public static File convertDottedToPathWithExtension(String clz, String ext) {
		File wo = convertDottedToPath(clz);
		return combine(wo.getParentFile(), wo.getName() + ext);
	}

	public static File convertDottedToPath(String pkg) {
		String[] spl = pkg.split("\\.");
		return fileConcat(spl);
	}
	
	public static String convertDottedToSlashPath(String pkg) {
		String[] spl = pkg.split("\\.");
		String ret = "";
		for (String s : spl)
			ret += "/" + s;
		return ret.substring(1);
	}

	public static File fileConcat(String... spl) {
		File ret = null;
		for (String s : spl)
		{
			if (s == null)
				continue;
			if (ret == null)
				ret = new File(s);
			else
				ret = new File(ret, s);
		}
		if (ret == null)
			throw new UtilException("Could not concatenate " + Arrays.toString(spl));
		return ret;
	}

	public static String urlPath(String root, File mavenToFile) {
		if (mavenToFile == null)
		{
			if (root.endsWith("/"))
				return root.substring(0, root.length()-1);
			return root;
		}
		return urlPath(root, mavenToFile.getParentFile()) + "/" + mavenToFile.getName();
	}

	public static void copyFileToStream(File from, OutputStream to) {
		InputStream is = null;
		try
		{
			try
			{
				is = new FileInputStream(from);
				copyStream(is, to);
			}
			finally
			{
				if (is != null)
					is.close();
			}
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static File copyStreamToTempFile(InputStream is) {
		try
		{
			File tmp = File.createTempFile("copy", "out");
			tmp.deleteOnExit();
			copyStreamToFile(is, tmp);
			return tmp;
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}
	
	public static void copyStreamToFile(InputStream is, File outFile) {
		try
		{
			FileOutputStream to = null;
			try
			{
				to = new FileOutputStream(outFile);
				copyStream(is, to);
			}
			finally
			{
				if (to != null)
					to.close();
			}
		}
		catch (Exception ex)
		{
			throw UtilException.wrap(ex);
		}
	}


	public static void copyStream(InputStream inputStream, OutputStream to) throws IOException {
		byte[] bs = new byte[500];
		int cnt = 0;
		while ((cnt = inputStream.read(bs, 0, 500)) > 0)
			to.write(bs, 0, cnt);
	}

	public static String readResource(String resourceName) {
		InputStream stream = FileUtils.class.getResourceAsStream(resourceName);
		if (stream == null)
		{
			if (!resourceName.startsWith("/"))
				stream = FileUtils.class.getResourceAsStream("/" + resourceName);
			if (stream == null)
				throw new ResourceNotFoundException("Could not find resource " + resourceName);
		}
		String ret = new String(readAllStream(stream));
		try { stream.close(); } catch (IOException ex) { throw UtilException.wrap(ex); }
		return ret;
	}

	// Apologies for the apparent duplication, but it's important to avoid
	// turning things into characters if at all possible - they can end up as '?' characters
	public static byte[] readAllStream(InputStream asStream) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int cnt;
			byte[] buf = new byte[500];
			while ((cnt = asStream.read(buf, 0, 500)) > 0) {
				baos.write(buf, 0, cnt);
			}
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static byte[] readAllReader(Reader r) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int cnt;
			char[] cbuf = new char[500];
			while ((cnt = r.read(cbuf, 0, 500)) > 0) {
				baos.write(new String(cbuf, 0, cnt).getBytes());
			}
			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}

	public static String readNStream(int contentLength, InputStream inputStream) {
		return readNReader(contentLength, new InputStreamReader(inputStream));
	}

	private static String readNReader(int cnt, Reader r) {
		try {
			int pos = 0;
			char[] cbuf = new char[cnt];
			int n;
			while (pos < cnt && (n = r.read(cbuf, pos, cnt-pos)) > 0) {
				pos += n;
			}
			return new String(cbuf);
		}
		catch (IOException ex) {
			throw UtilException.wrap(ex);
		}
	}


	public static void assertDirectory(File file) {
		if (!file.exists())
			if (!file.mkdirs())
				throw new UtilException("Cannot create directory " + file);
		if (!file.isDirectory())
			throw new UtilException("File '" + file + "' is not a directory");
	}

	public static File getCurrentDir() {
		return root;
	}

	public static void cleanDirectory(File dir) {
		List<File> ret = new ArrayList<File>();
		findRecursive(ret, anyFile, dir, dir);
		// sort longest to shortest to resolve empty directories
		Collections.sort(ret, filePathComparator);
		for (File f : ret)
		{
			if (!new File(dir, f.getPath()).delete())
				throw new UtilException("Could not delete: " + f);
		}
	}
	
	public static void persistentCleanDirectory(File bindir, int numberOfRetries, int msToWaitBetweenTries) {
		try {
			cleanDirectory(bindir);
		}
		catch (UtilException ex)
		{
			if (numberOfRetries > 0)
			{
				System.out.println(ex.getMessage() + " Retrying in " + msToWaitBetweenTries + "ms");
				persistentCleanDirectory(bindir, numberOfRetries - 1, msToWaitBetweenTries);
			}
			else
			{
				throw new UtilException("Not able to sucessfully delete " + bindir);
			}
		}
	}
	
	public static void deleteDirectoryTree(File dir) {
		cleanDirectory(dir);
		dir.delete();
	}

	public static File combine(Object... paths) {
		File ret = null;
		if (paths[0] instanceof File)
			ret = (File) paths[0];
		else if (paths[0] instanceof String)
			ret = new File((String) paths[0]);
		
		for (int i=1;i<paths.length;i++) {
			if (paths[i] instanceof File)
				ret = combine(ret, (File)paths[i]);
			else if (paths[i] instanceof String)
				ret = combine(ret, (String)paths[i]);
		}
		return ret;
	}
	
	public static File combine(File path1, String path2) {
		if (path1 == null && path2 == null)
			return null;
		else if (path1 == null)
			return new File(path2);
		else if (path2 == null)
			return path1;
		else
			return new File(path1, path2);
	}

	public static File combine(File path1, File path2) {
		if (path1 == null && path2 == null)
			return null;
		else if (path1 == null)
			return path2;
		else if (path2 == null)
			return path1;
		else
			return new File(path1, path2.getPath());
	}

	public static File combine(String path1, String path2) {
		if (path1 == null)
			return combine((File)null, path2);
		else
			return combine(new File(path1), path2);
	}

	public static String getHostName() {
		try {
		    return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw UtilException.wrap(e);
		}
	}

	public static void cat(File file) {
		try {
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String s;
			while ((s = lnr.readLine()) != null)
				System.out.println(s);
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	public static String getUnextendedName(File file) {
		String ret = file.getName();
		if (ret.indexOf(".") == -1)
			return ret;
		return ret.substring(0, ret.indexOf("."));
	}

	public static String getPackage(File file) {
		return convertToDottedName(file.getParentFile());
	}

	public static void copyAssertingDirs(File from, File to) {
		assertDirectory(to.getParentFile());
		copy(from, to);
	}

	public static void copyRecursive(File from, File to) {
		if (from == null)
			return;
		assertDirectory(to);
		File[] toCopy = from.listFiles();
		if (toCopy == null || toCopy.length == 0)
			return;
		int nerrors = 0;
		for (File f : toCopy)
		{
			try {
				File f2 = new File(to, f.getName());
				if (f.isDirectory())
					copyRecursive(f, f2);
				else
					copy(f, f2);
			} catch (Exception ex) {
				nerrors++;
			}
		}
		if (nerrors > 0)
			throw new UtilException("Encountered " + nerrors + " copying " + from + " to " + to);
	}

	public static void copy(File f, File f2) {
		try
		{
			FileInputStream fis = new FileInputStream(f);
			FileOutputStream fos = new FileOutputStream(f2);
			copyStream(fis, fos);
			fis.close();
			fos.close();
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}

	public static String clean(String name) {
		StringBuilder ret = new StringBuilder(name);
		for (int i=0;i<ret.length();i++)
		{
			char c = ret.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '.' && c != '_')
				ret.setCharAt(i, '.');
		}
		for (int i=ret.length()-1;i>=0 && ret.charAt(i ) == '.';i--)
			ret.deleteCharAt(i);
		if (ret.length() > 180)
			ret.delete(180, ret.length());
		return ret.toString();
	}

	public static Set<File> directorySet(Iterable<File> sourceFiles) {
		HashSet<File> ret = new HashSet<File>();
		for (File f : sourceFiles)
			ret.add(f.getParentFile());
		return ret;
	}

	public static File ensureExtension(File f, String ext) {
		return new File(f.getParentFile(), ensureExtension(f.getName(), ext));
	}

	public static String ensureExtension(String name, String ext) {
		if (name.endsWith(ext))
			return name;
		
		int idx = name.lastIndexOf(".");
		if (idx == -1)
			return name + ext;
		return name.substring(0, idx) + ext;
	}


	public static boolean isUnder(File file, File under) {
		try {
			String s = file.getCanonicalPath();
			String u = under.getCanonicalPath();
			return s.startsWith(u);
		} catch (IOException e) {
			return false;
		}
	}


	public static File moveRelativeRoot(File tmp, File from, File to) {
		if (!isUnder(tmp, from))
			throw new UtilException(tmp + " is not under " + from);
		File ret = makeRelativeTo(tmp, from);
		return new File(to, ret.getPath());
	}


	public static boolean isUpToDate(File copy, File orig) {
		return (copy.lastModified() >= orig.lastModified());
	}


	public static String readFile(File f) {
		FileInputStream fis = null;
		try
		{
			LineSeparator separator = detectLineSeparator(f);
			StringBuffer sb = new StringBuffer();
			fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String s;
			while ((s = br.readLine()) != null)
				sb.append(s + separator);
			return sb.toString();
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
		finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException ex) {
					throw UtilException.wrap(ex);
				}
		}
	}

	private static LineSeparator detectLineSeparator(File f) {
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(f);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			Character c;
			while((c = (char) br.read()) != null)
			{
				if(c == '\n')
					return LineSeparator.UNIX;
				if(c == '\r')
				{
					if(br.read() == '\n')
						return LineSeparator.WINDOWS;
					else
						return LineSeparator.OLDMAC; //This seems very unlikely
				}
			}
			return LineSeparator.OTHER; //This is perhaps even less likely
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
		finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException ex) {
					throw UtilException.wrap(ex);
				}
		}
	}


	public static byte[] readFileAsBytes(File f) {
		FileInputStream fis = null;
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			fis = new FileInputStream(f);
			copyStream(fis, baos);
			return baos.toByteArray();
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
		finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException ex) {
					throw UtilException.wrap(ex);
				}
		}
	}


	public static String posixPath(File path) {
		return path.getPath().replaceAll("\\\\", "/");
	}


	public static void createFile(File file, String contents) {
		try
		{
			FileOutputStream fos = new FileOutputStream(file);
			ByteArrayInputStream bais = new ByteArrayInputStream(contents.getBytes());
			copyStream(bais, fos);
			fos.flush();
			fos.close();
		}
		catch (IOException ex)
		{
			throw UtilException.wrap(ex);
		}
	}


	public static List<File> splitJavaPath(String path) {
		String[] elts = path.split(File.pathSeparator);
		List<File> ret = new ArrayList<File>();
		for (String s : elts) {
			if (s == null || s.length() == 0)
				continue;
			try {
				File pe = relativePath(new File(s)).getCanonicalFile();
				if (pe.exists())
					ret.add(pe);
			} catch (Exception ex) { 
				// whatever
			}
		}
		return ret;
	}

	public static File canonical(String dir) {
		try {
			return new File(dir).getCanonicalFile();
		} catch (IOException e) {
			throw UtilException.wrap(e);
		}
	}

	
}
