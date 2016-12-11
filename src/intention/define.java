
package intention;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.alibaba.fastjson.JSONObject;

class TOKEN{
	public int pos;
	public int len;
	public Set<Integer> TagIDs = Sets.newHashSet();
	public Map<Integer, JSONObject> normalizationMap = Maps.newHashMap();
	
	public JSONObject getNormalization(int tagid)
	{
		if(normalizationMap.containsKey(tagid))
		{
			return normalizationMap.get(tagid);
		}
		return null;
	}
	
}

class Sentence{
	
	/**
	 * 
	 */
	String m_sentence;
	Vector<TOKEN> m_tokens = new Vector();
	Vector<String> m_charVec;
	
	public void toChars(String str)
	{
		m_charVec = new Vector(Arrays.asList(str.split("|")));
		
	}
	
	Sentence(String input)
	{
		this.m_sentence = input;
	}
}

public class define {


	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
