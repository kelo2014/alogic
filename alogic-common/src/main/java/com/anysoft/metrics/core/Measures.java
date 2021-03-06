package com.anysoft.metrics.core;

import com.anysoft.util.JsonSerializer;
import com.anysoft.util.XmlSerializer;


/**
 * 指标的量度集
 * 
 * @author duanyy
 * @since 1.2.8
 * 
 * @version 1.6.4.45 [20160418 duanyy] <br>
 * - 支持多个方法 <br>
 */
public interface Measures extends XmlSerializer,JsonSerializer{
	
	public static enum Method {
		lst,
		avg,
		max,
		min,
		sum
	}
	
	/**
	 * 预定义的方法组：last
	 */
	public static final Method[] last = new Method[]{
		Method.lst
	};	
	
	/**
	 * 预定义的方法组: avg
	 */
	public static final Method[] avg = new Method[]{
		Method.avg
	};
	
	/**
	 * 预定义的方法组: max
	 */
	public static final Method[] max = new Method[]{
		Method.max
	};
	
	/**
	 * 预定义的方法组：min
	 */
	public static final Method[] min = new Method[]{
		Method.min
	};
	
	/**
	 * 预定义的方法组:sum
	 */
	public static final Method[] sum = new Method[]{
		Method.sum	
	};
	
	/**
	 * 获取量度的叠加方法
	 * @return Method
	 */
	public Method[] method();
	
	/**
	 * 设置量度的叠加方法
	 * @param method
	 * @return Measures
	 */
	public Measures method(Method method[]);
	
	/**
	 * 量度的叠加
	 * @param other
	 * @return Measures
	 */
	public Measures incr(Measures other);
	
	/**
	 * 从左边增加一个或多个量度
	 * @param values
	 * @return Measures
	 */
	public Measures lpush(Object [] values);
	
	/**
	 * 从右边增加一个或多个量度
	 * @param values
	 * @return Measures
	 */
	public Measures rpush(Object [] values);
	
	/**
	 * 获取量度的个数 
	 * @return Measures
	 */
	public int count();
	
	/**
	 * 获取指定量度的类型
	 * @param idx
	 * @return type 
	 */
	public char type(int idx);
	
	/**
	 * 获取所有量度的类型
	 * @return types
	 */
	public char [] types();
	
	/**
	 * 获取所有量度值
	 * @return strings
	 */
	public String [] values();
	
	/**
	 * 获取指定维度的量度值
	 * @param idx
	 * @return Object
	 */
	public Object get(int idx);
	
	/**
	 * 获取指定的量度
	 * @param idx
	 * @return Long value
	 */
	public Long asLong(int idx);
	
	/**
	 * 获取指定的量度
	 * @param idx
	 * @return Double value
	 */
	public Double asDouble(int idx);
	
	/**
	 * 获取指定的量度
	 * @param idx
	 * @return String value
	 */
	public String asString(int idx);
}
