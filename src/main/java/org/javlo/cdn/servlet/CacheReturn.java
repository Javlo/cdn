package org.javlo.cdn.servlet;

import java.io.InputStream;

public class CacheReturn {
	private String mimeType;
	private InputStream inputStream;
	private boolean compress;
	
	public CacheReturn(String mimeType, InputStream out, boolean compress) {
		super();
		this.mimeType = mimeType;
		this.inputStream = out;
		this.compress = compress;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public InputStream getInputStream() {
		return inputStream;
	}
	public void setInputStream(InputStream out) {
		this.inputStream = out;
	}
	public boolean isCompress() {
		return compress;
	}
	public void setCompress(boolean compress) {
		this.compress = compress;
	}
}
