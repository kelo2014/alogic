package com.logicbus.backend.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.anysoft.util.IOTools;
import com.logicbus.backend.Context;

/**
 * 基于Byte消息
 * @author duanyy
 * 
 * @since 1.6.1.1
 */
public class ByteMessage implements Message {
	/**
	 * a logger of log4j
	 */
	protected static final Logger logger = LogManager.getLogger(ByteMessage.class);
	/**
	 * 输入的字节流
	 */
	protected byte [] input = null;
	
	/**
	 * 获取输入的字节流
	 * @return 输入字节流
	 */
	public byte [] getInput(){return input;}
	
	/**
	 * 输出的字节流
	 */
	protected byte [] output = null;
	
	/**
	 * 设置待输出的字节流数据
	 * @param _output 待输出的数据
	 */
	public void setOutput(byte [] _output){
		output = _output;
	}
	
	public void init(MessageDoc ctx) {
		InputStream in = null;
		try {
			in = ctx.getInputStream();
			input = readBytes(in);
		}catch (Exception ex){
			logger.error("Error when reading data from inputstream",ex);
		}finally{
			IOTools.close(in);
		}
	}

	public void finish(MessageDoc ctx) {
		OutputStream out = null;
		try {
			ctx.setResponseContentType(contentType);
			out = ctx.getOutputStream();
			if (output != null)
				writeBytes(out,output);
		}catch (Exception ex){
			logger.error("Error when writing data from inputstream",ex);
		}finally{
			IOTools.close(out);
		}
	}

	public String getContentType(Context doc) {
		return contentType;
	}

	protected String contentType = "text/plain";
	
	public void setContentType(String _contentType){
		contentType = _contentType;
	}

	/**
	 * 从输入流中读取字节
	 * @param in 输入流
	 * @return 字节数组
	 * @throws IOException
	 */
	public static byte [] readBytes(InputStream in) throws IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		byte[] buf =new byte[1024];  
        
        int size=0;  
          
        while((size=in.read(buf))!=-1)  
        {  
            bos.write(buf,0,size);  
        }  
		
		return bos.toByteArray();
	}
	
	/**
	 * 向输出流中写出数据
	 * @param out 输出流
	 * @param data 待写出的数据
	 * @throws IOException
	 */
	public static void writeBytes(OutputStream out,byte [] data) throws IOException {
		out.write(data);
	}

}