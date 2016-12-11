

package intention;

import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;


// TODO: Auto-generated Javadoc
enum VAR_SEARCH_MODE{
	SM_CONST,
	SM_VARIABLE,
	SM_SINGLE,
	SM_BEGIN,
	SM_END,
	SM_ALL
}


enum STATE_LOAD_FILE{
	SLF_NORMAL,
	SLF_IF,
	SLF_IF_OK,
	SLF_THEN,
	SLF_THEN_OK,
	SLF_ELSE,
	SLF_ELSE_OK,
};

class PatternResult{
	public pattern _pat;
	public int _score;
	public int _b;
	public int _e;
	public Map<String, String> _hit = Maps.newHashMap();
	public Map<String, JSONObject> _hit_json = Maps.newHashMap();
	public Map<String, Integer> _hit_tag = Maps.newHashMap();
	
}


class PatternItem {
	String name;
	Variable variable;
	VAR_SEARCH_MODE searchType;	
}

class patternItemControler {
	
	private static Map<String, PatternItem> m_map_item = Maps.newHashMap();
	
	public static String removeVarIndex(String str){
		String res = "";
		int pos = 3;
		int lastPos = 0;
		while(str.indexOf("]", pos) > -1)
		{
			pos = str.indexOf("]", pos);
			if (str.charAt(pos - 1) >= '0' && str.charAt(pos - 1) <= '9'){
				res += str.substring(lastPos, pos - 1);
				res += "]";
			}else{
				res += str.substring(lastPos, pos + 1);
			}
			pos++;
			lastPos = pos;
		}
		if(lastPos < str.length()){
			res += str.substring(lastPos);
		}
		return res;
	}
	
	public PatternItem getItem(String key, variableControler vCtrl)
	{
		if (m_map_item.containsKey(key))
		{
			return m_map_item.get(key);
		}
		PatternItem item = new PatternItem();
		item.name = key;
		if (key.equals(pattern.PATTERN_BEGIN_STRING))
		{
			item.searchType = VAR_SEARCH_MODE.SM_BEGIN;
			item.variable = null;
		}
		else if(key.equals(pattern.PATTERN_END_STRING))
		{
			item.searchType = VAR_SEARCH_MODE.SM_END;
			item.variable = null;
		}
		else if (key.length() > 3 && key.charAt(0) == '$' && key.charAt(1) == '[' && key.charAt(key.length() - 1) == ']')
		{
			String varStr = removeVarIndex(item.name);
			item.variable = vCtrl.getVariable(varStr);
			// * 閫氶厤绗�
			if (varStr.equals(pattern.PATTERN_ANY_STRING))
			{
				item.searchType = VAR_SEARCH_MODE.SM_ALL;
			}
			else
			{
				item.searchType = VAR_SEARCH_MODE.SM_VARIABLE;
			}
		}
		else
		{
			item.variable = null;
			item.searchType = VAR_SEARCH_MODE.SM_CONST;
		}
		m_map_item.put(key, item);
		return item;
	}

}



/**
 * The Class pattern.
 */
public class pattern {
	public static Logger logger = Logger.getLogger(pattern.class);
	public final static String PATTERN_BEGIN_STRING = "^";
	public final static String PATTERN_END_STRING = "$";
	public final static String PATTERN_ANY_STRING = "*";
	/** The m priority. */
	public int m_priority;
	
	/** The m text. */
	public String m_text;
	
	/** The m id. */
	public int m_id;  //m_nodes index
	
	/** The m gid. */
	public int m_gid;  //GID
	
	/** The m items. */
	public Vector<PatternItem> m_items = new Vector();
	
	/** The scene. */
	String scene;
	
	
	/**
	 * Inits the.
	 *
	 * @param vCtrl the v ctrl
	 * @param iCtrl the i ctrl
	 * @return true, if successful
	 */
	public boolean init(variableControler vCtrl, patternItemControler iCtrl)
	{
		m_items.clear();
		PatternItem item;
		int i = 0;
		int pos, pos1, pos2;
		//pattern 浠�"^"寮�澶�
		if (m_text.contains(PATTERN_BEGIN_STRING))
		{
			item = iCtrl.getItem(PATTERN_BEGIN_STRING, vCtrl);
			m_items.add(item);
			i = PATTERN_BEGIN_STRING.length();
			
		}
		
		for (; i < m_text.length(); i++)
		{
			pos = m_text.indexOf(variableControler.VARIABLE_BEGIN_STRING, i);
			if (pos < 0) //娌″尮閰嶅埌鍙橀噺
			{
				//pattern 浠� "$"缁撳熬
				pos2 = m_text.lastIndexOf(PATTERN_END_STRING);
				if ((pos2 > -1) && (pos2 + PATTERN_END_STRING.length() == m_text.length()))
				{
					if (pos2 != i)
					{
						item = iCtrl.getItem(m_text.substring(i, pos2), vCtrl);
						m_items.add(item);
					}
					item = iCtrl.getItem(PATTERN_END_STRING, vCtrl);
					m_items.add(item);
				}
				else
				{
					item = iCtrl.getItem(m_text.substring(i), vCtrl);
					m_items.add(item);
				}
				break;
			}
			else //鎵惧埌鍙橀噺
			{
				pos1 = m_text.indexOf(variableControler.VARIABLE_END_STRING, pos + 2);
				if (pos1 < 0)
				{
					///TODO
					break;
				}
				else
				{
					if (pos > i) //闈炲彉閲忛儴鍒�
					{
						item = iCtrl.getItem(m_text.substring(i, pos), vCtrl);
						m_items.add(item);
					}
					//鍙橀噺
					item = iCtrl.getItem(m_text.substring(pos, pos1 + 1),vCtrl);
					if (item.variable == null)
					{
						logger.info(String.format("[Error]:can not find define :%s, %s", item.name, m_text));
						return false;
					}
					m_items.add(item);
					i = pos1;
				}
			}
		}
		return true;
	}
	
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}

}
