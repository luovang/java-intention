

package intention;

import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.collect.Maps;

// TODO: Auto-generated Javadoc

public class mapDict {
	
	/** The logger. */
	public static Logger logger = Logger.getLogger(mapDict.class);
	
	/** The Constant INIT_FLAG. */
	public static final char INIT_FLAG = 0x00;
	
	/** The Constant STOP_FLAG. */
	public static final char STOP_FLAG = 0x01;
	
	/** The Constant NEXT_FLAG. */
	public static final char NEXT_FLAG = 0x02;
	
	
	/** The m dict. */
	public static Map<String, Character> m_dict = Maps.newHashMap();
	
	/**
	 * Insert.
	 *
	 * @param str the str
	 * @return true, if successful
	 */
	public static boolean insert(String str)
	{
		char[] chars = str.toCharArray();
		String presuffix_str = "";
		String key = "";
		char value = 0;
		if (chars.length <= 0) 
			return false;
		if (chars.length == 1)
		{
			key = String.valueOf(chars[0]);
			if (m_dict.containsKey(key))
			{
				value = m_dict.get(String.valueOf(chars[0]));
				value |= STOP_FLAG;
				m_dict.put(key, value);
			}
			else
			{
				m_dict.put(key, STOP_FLAG);
			}
			return true;
		}
		for(int i = 0; i < chars.length; i++)
		{
            String old_pstr = presuffix_str;
            presuffix_str += chars[i];
            if(!m_dict.containsKey(presuffix_str))
            {
                m_dict.put(presuffix_str, INIT_FLAG);    // init
            }

            // next
            if(i + 1 < chars.length)
            {
            	if (m_dict.containsKey(presuffix_str))
            	{
            		value = m_dict.get(presuffix_str);
            		value |= NEXT_FLAG;
            	}
            	else
            	{
            		value = NEXT_FLAG;
            	}
            	m_dict.put(presuffix_str, value);
            }
            // is_stop
            if(i == chars.length - 1)
            {
            	if (m_dict.containsKey(presuffix_str))
            	{
            		value = m_dict.get(presuffix_str);
            		value |= STOP_FLAG;
            	}
            	else
            	{
            		value = STOP_FLAG;
            	}
            	m_dict.put(presuffix_str, value);
            }
		}
		
		return true;
	}
	
	
	/**
	 * Checks for next.
	 *
	 * @param preffix_str the preffix str
	 * @param next_c the next c
	 * @return true, if successful
	 */
	public static boolean hasNext(String preffix_str, String next_c)
	{
		String next_str = preffix_str + next_c;
		if (m_dict.containsKey(next_str))
			return true;
		return false;
	}
	
	/**
	 * Checks for next.
	 *
	 * @param state_str the state str
	 * @return true, if successful
	 */
	public static boolean hasNext(String state_str)
	{
		if (m_dict.containsKey(state_str))
		{
			if(NEXT_FLAG == (m_dict.get(state_str) & NEXT_FLAG))
				return true;
		}
		return false;
	}
	
	/**
	 * Checks if is stop.
	 *
	 * @param str the str
	 * @return true, if is stop
	 */
	public static boolean isStop(String str)
	{
		if (m_dict.containsKey(str))
		{
			if (STOP_FLAG == (m_dict.get(str) & STOP_FLAG))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Inits the.
	 *
	 * @param vec the vec
	 */
	public static void init(Vector<String> vec)
	{
		m_dict.clear();
		for (String str : vec)
		{
			insert(str);
		}
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "杩欐槸娴嬭瘯璇彞銆�";
		char[] chars = str.toCharArray();
		for(char c : chars)
			System.out.println(c);
		Map<String, Character> m = Maps.newHashMap();
		char v = 0x01;
		m.put("ma", v);
		char a = m.get("mb");
		a |= v;
		
		

	}

}
