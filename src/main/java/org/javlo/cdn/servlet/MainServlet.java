package org.javlo.cdn.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.javlo.cdn.helper.ResourceHelper;
import org.javlo.cdn.helper.StringHelper;
import org.javlo.cdn.helper.UrlHelper;

public class MainServlet extends HttpServlet {

	private static final Properties NOT_FOUND = new Properties();

	private static Logger logger = Logger.getLogger(MainServlet.class.getName());

	private static final String VERSION = "B 0.0.4";
	private static Map<String, Properties> config = new HashMap<>();

	public static File CONFIG_FOLDER = new File(System.getProperty("user.home") + "/etc/javlo_cdn");
	public static File DATA_FOLDER = new File(System.getProperty("user.home") + "/data/javlo_cdn");

	@Override
	public void init() throws ServletException {
		super.init();
		System.out.println("");
		System.out.println("***************************");
		System.out.println("*** JAVLO CDN : " + VERSION + " ***");
		System.out.println("***************************");
		System.out.println("");
		System.out.println("CONFIG_FOLDER = " + CONFIG_FOLDER);
		System.out.println("DATA_FOLDER   = " + DATA_FOLDER);

		if (!CONFIG_FOLDER.exists()) {
			CONFIG_FOLDER.mkdirs();
		}

		if (!DATA_FOLDER.exists()) {
			DATA_FOLDER.mkdirs();
		}
	}

	private static final File createFileCache(String host, String uri, Boolean compress) {
		File file = new File(DATA_FOLDER.getAbsolutePath() + '/' + StringHelper.createFileName(host) + '/' + StringHelper.createFileName(uri) + "." + StringHelper.getFileExtension(uri).toLowerCase());
		if (compress == null && !file.exists()) {
			File cFile = new File(DATA_FOLDER.getAbsolutePath() + '/' + StringHelper.createFileName(host) + '/' + StringHelper.createFileName(uri) + "." + StringHelper.getFileExtension(uri).toLowerCase() + ".gzip");
			if (cFile.exists()) {
				return cFile;
			}
		}
		if (compress != null && compress) {
			file = new File(file.getAbsolutePath() + ".gzip");
		}
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

	private void reset(String host) throws IOException {
		File file = new File(DATA_FOLDER.getAbsolutePath() + '/' + StringHelper.createFileName(host));
		logger.info("delete cache : " + file);
		File fileDest = new File(DATA_FOLDER.getAbsolutePath() + "/___DELETE_ME___" + StringHelper.createFileName(host));
		file.renameTo(fileDest);
		deleteDirectoryRecursion(fileDest);
	}

	private static final void deleteDirectoryRecursion(File file) throws IOException {
		if (file.isDirectory()) {
			File[] entries = file.listFiles();
			if (entries != null) {
				for (File entry : entries) {
					deleteDirectoryRecursion(entry);
				}
			}
		}
		if (!file.delete()) {
			throw new IOException("Failed to delete " + file);
		}
	}

	private static boolean isCompress(File file) {
		return file.getAbsolutePath().endsWith(".gzip");
	}

	private CacheReturn getInCache(String host, String uri) throws FileNotFoundException {
		File cacheFile = createFileCache(host, uri, null);
		if (cacheFile.exists()) {
			return new CacheReturn(ResourceHelper.getFileExtensionToMineType(StringHelper.getFileExtension(cacheFile.getName())), new FileInputStream(cacheFile), isCompress(cacheFile));
		} else {
			return null;
		}
	}

	private CacheReturn putInCache(String host, String uri, InputStream in) throws IOException {
		String ext = StringHelper.getFileExtension(uri);
		boolean compress = false;
		if (ext != null && ext.length() > 0) {
			ext = ext.toLowerCase();
			if (ext.equals("js") || ext.equals("html") || ext.equals("txt") || ext.equals("css") || ext.equals("svg")) {
				compress = true;
			}
		}
		OutputStream out = null;
		File cacheFile = createFileCache(host, uri, compress);
		try {
			out = new FileOutputStream(cacheFile);
			if (compress) {
				out = new GZIPOutputStream(out);
			}
			ResourceHelper.writeStreamToStream(in, out);
		} finally {
			out.close();
		}
		return getInCache(host, uri);
	}

	private static Properties getConfig(String host) throws FileNotFoundException, IOException {
		host = StringHelper.createFileName(host);
		Properties out = config.get(host);
		if (out == null) {
			synchronized (config) {
				File prop = new File(CONFIG_FOLDER.getAbsoluteFile() + "/" + host + ".properties");
				if (!prop.exists()) {
					logger.warning("host not found : " + prop);
					out = NOT_FOUND;
				} else {
					out = new Properties();
					try (InputStream in = new FileInputStream(prop)) {
						out.load(in);
					}
				}
				config.put(host, out);
			}
		}
		if (out == NOT_FOUND) {
			return null;
		} else {
			return out;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response, false);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		process(request, response, true);
	}

	private void process(HttpServletRequest request, HttpServletResponse response, boolean post) {
		try {
			String host = StringHelper.getDomainName(request.getRequestURL().toString());
			String uri = request.getPathInfo();
			if (uri.length() > 3) {
				uri = uri.substring(1); // remove '/'
				Properties config = getConfig(host);
				if (config == null) {
					int index = uri.indexOf('/');
					if (index > 0) {
						host = uri.substring(0, index);
						uri = uri.substring(index);
						config = getConfig(host);
					} else {
						logger.severe("context not found.");
					}
				}
				if (config != null) {
					String urlHost = config.getProperty("url.target");
					if (urlHost != null) {
						if (uri.equals("/" + config.get("code.reset"))) {
							reset(host);
						} else {
							CacheReturn cache = getInCache(host, uri);
							if (cache == null) {
								synchronized (this) {
									cache = getInCache(host, uri);
									if (cache == null) {
										String sourceUrl = UrlHelper.mergePath(urlHost, uri);
										URL url = new URL(sourceUrl);
										InputStream in = null;
										try {
											URLConnection conn = url.openConnection();
											in = conn.getInputStream();
											logger.info("add in cache : " + sourceUrl);
											cache = putInCache(host, uri, in);
										} catch (Exception e) {
											e.printStackTrace();
											logger.severe("error connection : " + url);
										} finally {
											ResourceHelper.safeClose(in);
										}
									}
								}
							}
							if (cache != null) {
								if (cache.getMimeType() != null) {
									response.setContentType(cache.getMimeType());
								}
								String cleanHost = urlHost;
								if (cleanHost.endsWith("/")) {
									cleanHost = cleanHost.substring(0, cleanHost.length() - 1);
								}
								if (cache.isCompress()) {
									response.addHeader("Content-Encoding", "gzip");
								}
								response.addHeader("Access-Control-Allow-Origin", cleanHost);
								response.addHeader("Cache-control", "max-age=" + (60 * 60 * 24 * 30) + ", public"); // 30 days
								ResourceHelper.writeStreamToStream(cache.getInputStream(), response.getOutputStream());
								ResourceHelper.safeClose(cache.getInputStream());
							} else {
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							}
						}
					} else {
						logger.severe("bad config file (no url.target) : " + host);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
