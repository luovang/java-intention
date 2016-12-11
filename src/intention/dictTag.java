

package intention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

// TODO: Auto-generated Javadoc
/**
 * The Class dictTag.
 */
public class dictTag {

	/** The logger. */
	public static Logger logger = Logger.getLogger(dictTag.class);
	
	/** The m map tags. */
	public static Map<String, Map<String, JSONObject>> m_map_tags = Maps.newHashMap();
	
	/**
	 * Checks if is type.
	 *
	 * @param str the str
	 * @param tag the tag
	 * @return true, if is type
	 */
	public static boolean isType(String str, String tag)
	{
		if (!m_map_tags.containsKey(str))
			return false;
		if (!m_map_tags.get(str).containsKey(tag))
			return false;
		return true;
	}
	
	/**
	 * Gets the word tag value.
	 *
	 * @param word the word
	 * @param tag the tag
	 * @param jv the jv
	 * @return the word tag value
	 */
	public static boolean getWordTagValue(String word, String tag, JSONObject jv)
	{
		if(!isType(word, tag))
			return false;
		jv = m_map_tags.get(word).get(tag);
		return true;
	}
	
	//鍩轰簬map鍔犺浇tag璇嶅吀
	private static void loadTagByMap(String tag, Map<String, String> tagMap, Vector<String> tagvec)
	{
		String term = "";
		JSONObject jv = null;
		String value = "";
		for(Map.Entry<String, String> entry : tagMap.entrySet()) {
			term = entry.getKey();
			value = entry.getValue();
			if (null != value) {
				value = String.format("{\"name\":\"%s\"}", value);
			}
			else {
				value = null;
			}
			
			jv = JSON.parseObject(value);
			if (!m_map_tags.containsKey(term))
			{
				Map<String, JSONObject> map_jv = Maps.newHashMap();
				map_jv.put(tag, jv);
				m_map_tags.put(term, map_jv);
			}
			else
			{
				Map<String, JSONObject> map_jv = m_map_tags.get(term);
				map_jv.put(tag, jv);
			}
			tagvec.add(term);
		}
	}
	
	private static void loadCompanyTagByMap(String tag, Map<String, String> tagMap, Vector<String> tagvec)
	{
		String term = "";
		JSONObject jv = null;
		String value = "";
		for(Map.Entry<String, String> entry : tagMap.entrySet()) {
			term = entry.getKey();
			value = entry.getValue();
			if (null != value) {
				value = String.format("{\"normal\":\"%s\",\"list\":\"%s\"}", term, term);
			}
			else {
				value = null;
			}
			
			jv = JSON.parseObject(value);
			if (!m_map_tags.containsKey(term))
			{
				//logger.info("dict not contain:" + tag +"\t" +term);
				Map<String, JSONObject> map_jv = Maps.newHashMap();
				map_jv.put(tag, jv);
				m_map_tags.put(term, map_jv);
			}
			else
			{
				//logger.info("dict contain:" + tag +"\t" +term);
				Map<String, JSONObject> map_jv = m_map_tags.get(term);
				map_jv.put(tag, jv);
			}
			tagvec.add(term);
		}
	}
	
	
	/**
	 * Initialize.
	 *
	 * @param path the path
	 * @param fileName the file name
	 * @return true, if successful
	 */
	public static boolean initialize(String path, Vector<String> fdicts)
	{
		try
		{
			Vector<String> tagvec = new Vector<String>();
			for (String fileName : fdicts)
			{
				@SuppressWarnings("resource")
				BufferedReader reader = new BufferedReader(new FileReader(new File(path + fileName)));
				String line = "";
				String type_str = "";
				String term = "";
				String []arr;
				while ((line = reader.readLine()) != null)
				{
					if (0 == line.length()) continue;
					if ('#' == line.charAt(0)) continue;
					if (line.startsWith("TAG:"))
					{
						type_str = line.substring(4);
					}
					else
					{
						if (type_str.equals(""))
							continue;
						
						line = line.trim();
						term = line;
						JSONObject jv = null;
						arr = line.split("\t");
						// key value 缁撴瀯 value涓簀son
						if (arr.length == 2)
						{
							term = arr[0];
							try
							{

								jv = JSON.parseObject(arr[1]);
							}
							catch(Exception e)
							{
								logger.info("load tag dict fail for bad format.");
								return false;
							}

						}
						
						if (!m_map_tags.containsKey(term))
						{
							Map<String, JSONObject> map_jv = Maps.newHashMap();
							map_jv.put(type_str, jv);
							m_map_tags.put(term, map_jv);
						}
						else
						{
							Map<String, JSONObject> map_jv = m_map_tags.get(term);
							map_jv.put(type_str, jv);
						}
						tagvec.add(term);
					}
				}
			}
			/*鍔犺浇鑲＄エ鍒悕tag,璇ag鏈塪ataapi鎺ュ彛鎻愪緵鏁版嵁,鍦╱til閲岄潰宸茶В鏋愬埌map涓�*/
			//key涓烘浘鐢ㄥ悕锛� value涓虹幇鐢ㄥ悕
			//loadTagByMap("stock_old", StockAlias.getStockNewSynonymsMap(), tagvec);
			//key 涓烘柊鐢ㄥ悕锛� value涓烘浘鐢ㄥ悕
			//loadTagByMap("stock_new", StockAlias.getStockOldSynonymsMap(), tagvec);
			//鍔犺浇dataapi鑾峰彇鐨勫叕鍙哥畝绉拌瘝琛�
			//loadCompanyTagByMap("company", StockAlias.SecNameStockMap, tagvec);
			mapDict.init(tagvec);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error("load tag dict file fail.");
			return false;
		}
		return true;
	}
	
	public boolean locateAndInfer(Sentence sent, tagControler _tag_Ctrl)
	{
		int pos = 0;
		sent.toChars(sent.m_sentence);
		for (int i = 0; i < sent.m_charVec.size(); i++)
		{
			String preffix_str = "";
			String next_c = "";
			for (int j = i; j < sent.m_charVec.size(); j++)
			{
				preffix_str += sent.m_charVec.get(j);
				if (mapDict.isStop(preffix_str))
				{
					if (m_map_tags.containsKey(preffix_str))
					{
						if (m_map_tags.get(preffix_str).size() > 0)
						{
							TOKEN token = new TOKEN();
							token.pos = pos;
							token.len = preffix_str.length();
							for (Map.Entry<String, JSONObject> entry : m_map_tags.get(preffix_str).entrySet())
							{
								int tag_id = _tag_Ctrl.getTagId(entry.getKey());
								token.TagIDs.add(tag_id);
								token.normalizationMap.put(tag_id, entry.getValue());
							}
							sent.m_tokens.add(token);
						}
					}
					if (j + 1 < sent.m_charVec.size())
					{
						next_c = sent.m_charVec.get(j + 1);
					}
					if (!mapDict.hasNext(preffix_str, next_c))
					{
						break;
					}
				}
			}
			pos += sent.m_charVec.get(i).length();
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
		String a = "{\"name\":\"luofang\",\"age\":10}";
		JSONObject jv = JSON.parseObject(a);
		System.out.println(jv.getString("name"));
		System.out.println(jv.getIntValue("age"));
		
		Vector<String> dicts = new Vector();
		dicts.add("data/intentionRecongnize/report/report.dict");
		dictTag.initialize("", dicts);
		boolean bret = dictTag.getWordTagValue("鍙戣鑲＄エ", "report_ipo_pricing", jv);
		System.out.println(bret);
		
		String str = "hello|world";
		System.out.println(str.indexOf("|"));
		String[]arr = str.split("\\|");
		System.out.println(arr.toString());

	}

}
