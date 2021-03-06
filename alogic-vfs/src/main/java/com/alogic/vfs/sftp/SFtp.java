package com.alogic.vfs.sftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alogic.vfs.core.VirtualFileSystem;
import com.anysoft.util.IOTools;
import com.anysoft.util.Properties;
import com.anysoft.util.PropertiesConstants;
import com.anysoft.util.code.Coder;
import com.anysoft.util.code.CoderFactory;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SFTPException;
import ch.ethz.ssh2.SFTPv3Client;
import ch.ethz.ssh2.SFTPv3DirectoryEntry;
import ch.ethz.ssh2.SFTPv3FileAttributes;
import ch.ethz.ssh2.SFTPv3FileHandle;

/**
 * 基于sftp的vfs
 * 
 * @author weibj
 *
 */

public class SFtp extends VirtualFileSystem.Abstract {

	/**
	 * sftp的channel
	 */
	protected SFTPv3Client client0 = null;

	/**
	 * sftp的session
	 */
	protected Connection conn = null;
	/*
	 * sftp的主机
	 */
	protected String host = "10.128.91.3";

	/*
	 * sftp的端口号
	 */
	protected int port = 22;

	/*
	 * sftp的登录用户名
	 */
	protected String username = "root";

	/*
	 * 创建目录的权限值
	 */
	protected int permissions = 0777;

	/*
	 * sftp的登录密码
	 */
	// protected String password = "ecld_at2n1";
	protected String password = "LPoUSQSbfDwcAce9HX18BQ==";
	/*
	 * 加密的coder
	 */
	protected String coder = "DES3";

	private static int ONCE_MAX_BYTES = 32768;

	public SFtp() {

	}

	@Override
	public void configure(Properties p) {
		host = PropertiesConstants.getString(p, "host", host);
		port = PropertiesConstants.getInt(p, "port", port);
		username = PropertiesConstants.getString(p, "username", username);
		password = PropertiesConstants.getString(p, "password", password);
		coder = PropertiesConstants.getString(p, "coder", coder);
		id = PropertiesConstants.getString(p, "id", "default", true);
		// 列出文件列表时，默认过滤"."和".."
		dftPattern = PropertiesConstants.getString(p, "dftPattern", "[.]*[^.]+.*");
		root = PropertiesConstants.getString(p, "root", root);
		permissions = PropertiesConstants.getInt(p, "permissions", permissions);
	}

	protected SFTPv3Client getClient() {
		if (client0 == null) {
			synchronized (this) {
				if (client0 == null) {
					try {
						conn = new Connection(host, port);
						conn.connect();
						String _password = password;
						if (coder != null && coder.length() > 0) {
							// 通过coder进行密码解密
							try {
								Coder _coder = CoderFactory.newCoder(coder);
								_password = _coder.decode(password, username);
							} catch (Exception ex) {
								LOG.error("Can not find coder:" + coder);
							}
						}
						boolean isAuthed = conn.authenticateWithPassword(username, _password);
						if (isAuthed) {
							client0 = new SFTPv3Client(conn);
						}
					} catch (Exception e) {
						LOG.error(e);
					}
				}
			}
		}

		return client0;
	}

	protected void getFileInfo(SFTPv3FileAttributes attrs, Map<String, Object> json) {
		json.put("dir", attrs.isDirectory());
		json.put("lastModified", attrs.mtime);
		json.put("lastAccess", attrs.atime);
		json.put("fileSize", attrs.size);
		json.put("symLink", attrs.isSymlink());
		json.put("groupId", attrs.gid);
		json.put("permission", attrs.permissions);
		json.put("ownerId", attrs.uid);
	}

	protected String getRealPath(String path) {
		return root + File.separatorChar + path;
	}

	@Override
	public List<String> listFiles(String path, String pattern, int offset, int limit) {
		SFTPv3Client client = getClient();
		String realPath = getRealPath(path);
		try {
			if (isDir(path)) {
				List<String> result = new ArrayList<String>();
				List<SFTPv3DirectoryEntry> files = client.ls(realPath);

				if (files != null) {
					Pattern p = Pattern.compile(pattern);
					int current = 0;

					for (int i = 0; i < files.size(); i++) {
						SFTPv3DirectoryEntry entry = files.get(i);
						String filename = entry.filename;
						Matcher matcher = p.matcher(filename);
						;
						if (matcher.matches()) {
							if (current >= offset) {
								result.add(filename);
							}
							current++;
							if (current >= offset + limit) {
								break;
							}
						}

					}
				}
				return result;
			}
		} catch (IOException e) {
			LOG.error(e);
		}
		return null;
	}

	@Override
	public void listFiles(String path, String pattern, Map<String, Object> json, int offset, int limit) {
		SFTPv3Client client = getClient();
		String realPath = getRealPath(path);
		try {
			if (isDir(path)) {
				List<Object> result = new ArrayList<Object>();
				List<SFTPv3DirectoryEntry> files = client.ls(realPath);

				if (files != null) {
					Pattern p = Pattern.compile(pattern);
					int current = 0;

					for (int i = 0; i < files.size(); i++) {
						SFTPv3DirectoryEntry file = files.get(i);
						String filename = file.filename;
						Matcher matcher = p.matcher(filename);
						if (matcher.matches()) {
							if (current >= offset) {
								Map<String, Object> map = new HashMap<String, Object>();
								getFileInfo(file.attributes, map);
								map.put("name", filename);
								map.put("path", path + File.separator + filename);
								result.add(map);
							}
							current++;
							if (current >= offset + limit) {
								break;
							}
						}

					}
					json.put("file", result);
					json.put("offset", offset);
					json.put("limit", limit);
					json.put("total", current);
					json.put("path", path);
				}
			}
		} catch (IOException e) {
			LOG.error(e);
		}

	}

	@Override
	public boolean deleteFile(String path) {
		SFTPv3Client client = getClient();
		String realPath = getRealPath(path);
		try {
			if (isDir(path)) {
				deleteFolder(path);
				client.rmdir(getRealPath(path));
			} else {
				client.rm(realPath);
			}
			return true;
		} catch (IOException e) {
			LOG.error(e);
			return false;
		}
	}

	//递归删除文件夹
	private Boolean deleteFolder(String folderPath) {
		Boolean result = true;
		SFTPv3Client client = getClient();
		List<String> fileLists = new ArrayList<String>();
		fileLists = listFiles(folderPath, dftPattern, 0, Integer.MAX_VALUE);
		for (String file : fileLists) {
			try {
				String path = folderPath + File.separatorChar + file;
				if (isDir(path)) {
					deleteFolder(path);
					client.rmdir(getRealPath(path));
				} else {
					client.rm(getRealPath(path));
				}
			} catch (IOException e) {
				LOG.error(e);
				result = false;
			}
		}
		return result;
	}

	@Override
	public boolean exist(String path) {
		SFTPv3Client client = getClient();
		path = getRealPath(path);
		try {
			client.stat(path);
		} catch (SFTPException e) {
			return false;
		} catch (IOException ie) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isDir(String path) {
		SFTPv3Client client = getClient();
		path = getRealPath(path);
		try {
			SFTPv3FileAttributes attrs = client.stat(path);
			if (attrs == null) {
			}
			return attrs == null ? false : attrs.isDirectory();
		} catch (IOException e) {
			LOG.error(e);
			return false;
		}
	}

	@Override
	public long getFileSize(String path) {
		SFTPv3Client client = getClient();
		path = getRealPath(path);
		try {
			SFTPv3FileAttributes attrs = client.stat(path);
			return attrs == null ? 0 : attrs.size;
		} catch (IOException e) {
			LOG.error(e);
			return 0;
		}
	}

	@Override
	public void getFileInfo(String path, Map<String, Object> json) {
		SFTPv3Client client = getClient();
		path = getRealPath(path);
		try {
			SFTPv3FileAttributes attrs = client.stat(path);
			if (attrs != null) {
				getFileInfo(attrs, json);
				json.put("path", path);
			}
		} catch (IOException e) {
			LOG.error(e);
		}
	}

	@Override
	public boolean makeDirs(String path) {
		SFTPv3Client client = getClient();
		String arrPath[] = path.split(File.separatorChar + "");
		String pathtmp = "";
		for (int i = 0; i < arrPath.length; i++) {
			pathtmp += File.separatorChar + arrPath[i];
			try {
				if (!isDir(pathtmp)) {
					client.mkdir(getRealPath(pathtmp), permissions);
				}
			} catch (IOException e) {
				LOG.error(e);
				return false;
			}
		}
		return true;
	}

	private byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
		byte[] byte_3 = new byte[byte_1.length + byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}

	@Override
	public InputStream readFile(String path) {
		SFTPv3Client client = getClient();
		String realPath = getRealPath(path);
		int startid = 0;
		try {
			if (!isDir(path)) {
				SFTPv3FileHandle handle = client.openFileRO(realPath);
				byte[] result = new byte[0];
				byte[] tempResult = new byte[ONCE_MAX_BYTES];
				int len = client.read(handle, startid, tempResult, 0, ONCE_MAX_BYTES);
				while (len == ONCE_MAX_BYTES) {
					result = byteMerger(result, tempResult);
					startid += len;
					len = client.read(handle, startid, tempResult, 0, ONCE_MAX_BYTES);
				}
				byte[] lastResult = new byte[len];
				System.arraycopy(tempResult, 0, lastResult, 0, len);
				result = byteMerger(result, lastResult);
				client.closeFile(handle);
				return new ByteArrayInputStream(result);
			}
			return null;
		} catch (IOException e) {
			LOG.error(e);
			return null;
		}
	}

	@Override
	public void finishRead(String path, InputStream in) {
		IOTools.close(in);
	}

	@Override
	public OutputStream writeFile(String path) {
		final SFTPv3Client client = getClient();
		String realPath = getRealPath(path);
		try {
			if (!exist(path)) {
				final SFTPv3FileHandle handle = client.createFile(realPath);

				OutputStream out = new OutputStream() {
					private boolean isClosed = false;
					private int startid = 0;

					public void write(byte[] d) throws java.io.IOException {
						write(d, 0, d.length);
					}

					public void write(byte[] d, int s, int len) throws java.io.IOException {
						if (isClosed) {
							throw new IOException("stream already closed");
						}

						if (d == null) {
							throw new NullPointerException();
						}
						if (s < 0 || len < 0 || s + len > d.length) {
							throw new IndexOutOfBoundsException();
						}

						try {
							client.write(handle, startid, d, s, len);
							startid += len;
						} catch (IOException e) {
							throw e;
						} catch (Exception e) {
							throw new IOException(e.toString());
						}
					}

					byte[] _data = new byte[1];

					public void write(int foo) throws java.io.IOException {
						_data[0] = (byte) foo;
						write(_data, 0, 1);
					}

					public void close() throws java.io.IOException {
						if (isClosed) {
							return;
						}

						try {
							client.closeFile(handle);
						} catch (IOException e) {
							throw e;
						} catch (Exception e) {
							throw new IOException(e.toString());
						}
						isClosed = true;
					}
				};
				return out;
			}
			return null;
		} catch (IOException e) {
			LOG.error(e);
			return null;
		}
	}

	@Override
	public void finishWrite(String path, OutputStream out) {
		IOTools.close(out);
	}

	@Override
	public void close() throws Exception {
		if (client0 != null) {
			client0.close();
		}
		if (conn != null) {
			conn.close();
		}
	}

	public static void main(String[] args) throws IOException {
		//SFtp sftp = new SFtp();
		Coder des3Coder = CoderFactory.newCoder("DES3");
		System.out.println(des3Coder.encode("ecld_at2n1", "alogic"));
		Coder md5Coder = CoderFactory.newCoder("MD5");
		String expressPassword = des3Coder.decode("1234", "chenxuj");
		String _password = md5Coder.encode(expressPassword, "chenxuj");
		System.out.println(_password);

		// sftp.makeDirs("/root/test1/test2/test3");

		// sftp.makeDirs("/root/test1");
		// sftp.deleteFile("/root/test.txt");
		//sftp.deleteFile("/root/test");
		// System.out.println(sftp.getFileSize("/root/maven-metadata.xml"));
		// System.out.println(sftp.isDir("root/test.txt")?"exits":"not exits");
		// System.out.println(sftp.isDir("/root/test")?"exits":"not exits");
		// System.out.println(sftp.exist("/root/test.txt")?"exits":"not exits");
		// Map<String,Object> json = new HashMap<String,Object>();
		// sftp.getFileInfo("/root", json);
		// sftp.listFiles("/root","", json, 0, 10);
		// sftp.listFiles("/root", 0, 10);
		// sftp.deleteFile("/root/local.xml");

		/*
		 * InputStream is = sftp.readFile("/root/test.pdf"); InputStreamReader a
		 * = new InputStreamReader(is); BufferedReader b = new
		 * BufferedReader(a); String line; while ((line = b.readLine()) != null)
		 * { System.out.println(line); }
		 */

		// sftp.writeFileTest("/root/test.txt");
		// OutputStream os = sftp.writeFile("/root/test.txt");
		// String test = "hahhahahahahhahahahha";
		// byte[] test2 = test.getBytes();
		// os.write(test2, 0, 10);
		// os.write(test2, 10, 10);
		// os.write(test.getBytes());
		// os.write(8);
		// os.flush();
	}
}
