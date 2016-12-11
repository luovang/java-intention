
package intention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// TODO: Auto-generated Javadoc

enum patternSelectMode
{
	BYSCORE,
	BYPRIORITY
}

public class recongnizeEngine {

	private static stateCtrl m_stateCtrl;
	private static patternControler m_patternCtrl;
	private static dictTag m_dictTag;
	private static tagControler _tag_ctrl;
	private entranceControler ent_ctrl;
	public static Logger logger = Logger.getLogger(recongnizeEngine.class);
	
	//*lua
	private static LuaState L;
	static
	{
	    L = LuaStateFactory.newLuaState();
	    L.openLibs();
	    //StockAlias.init();
	    init();
	}
	
	public static boolean init()
	{
		boolean ret = false;
		String root = "C:/Users/luovang/workspace/intention/data/";
		String config = "configure.cfg";
		Vector<String>f_states = new Vector();
		Vector<String>f_tags = new Vector();
		Vector<String>f_variables = new Vector();
		Vector<String>f_dicts = new Vector();
		Vector<String>f_patterns = new Vector();
		Vector<String>f_cfgs = new Vector();
		Vector<String>f_mappings = new Vector();
		
		//鍔犺浇鎬荤殑閰嶇疆鏂囦欢
		try
		{
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader(new File(root + config)));
			String line = "";
			while ((line = reader.readLine()) != null)
			{
				if (0 == line.length()) continue;
				if ('#' == line.charAt(0)) continue;
				line = line.trim();
				int pos = line.indexOf("=");
				if (pos < 0) continue;
				String key = line.substring(0, pos).trim();
				String value = line.substring(pos + 1).trim();
				if (key.equals("CONFIG_DEFINE"))
				{
					f_cfgs.addAll(Arrays.asList(value.split(";")));
				}
				else if (key.equals("TAG_DEFINE"))
				{
					f_tags.addAll(Arrays.asList(value.split(";")));
				}
				else if (key.equals("VAR_DEFINE"))
				{
					f_variables.addAll(Arrays.asList(value.split(";")));
				}
			}
			reader.close();
		}
		catch (Exception e)
		{
			logger.info("load root config file except.");
			return false;
		}
		
		//鍔犺浇鍚勪釜绫荤洰涓嬬殑閰嶇疆鏂囦欢
		for (String f_config : f_cfgs)
		{
			try
			{
				@SuppressWarnings("resource")
				BufferedReader reader = new BufferedReader(new FileReader(new File(root + f_config)));
				String line = "";
				while ((line = reader.readLine()) != null)
				{
					if (0 == line.length()) continue;
					if ('#' == line.charAt(0)) continue;
					line = line.trim();
					int pos = line.indexOf("=");
					if (pos < 0) continue;
					String key = line.substring(0, pos).trim();
					String value = line.substring(pos + 1).trim();
					if (key.equals("PAT_DEFINE"))
					{
						f_patterns.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("TAG_DEFINE"))
					{
						f_tags.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("VAR_DEFINE"))
					{
						f_variables.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("VAR_DICT"))
					{
						f_dicts.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("MAPPING_DEFINE"))
					{
						f_mappings.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("STATE_VAR_DEFINE"))
					{
						f_states.addAll(Arrays.asList(value.split(";")));
					}
					else if (key.equals("ENT_DEFINE"))
					{
						
					}
					else if (key.equals("FLOW_DEFINE"))
					{
						
					}
				}
				reader.close();
			}
			catch (Exception e)
			{
				logger.info("load partition config file except.");
				return false;
			}
		}
		
		ret = loadResources(root, f_states, f_tags, f_variables, f_patterns, f_dicts);
		return ret;
	}
	
	private static boolean loadResources(String path, Vector<String>fstates, Vector<String>ftags,
			Vector<String>fvariables, Vector<String>fpatterns, Vector<String>fdicts)
	{
		//鍔犺浇stat.define
		m_stateCtrl = new stateCtrl();
		if (!m_stateCtrl.loadFiles(path, fstates))
		{
			logger.info("load state file fail.");
			return false;
		}
		
		//鍔犺浇tag.define, variable.define, pattern.define
		m_patternCtrl = new patternControler();
		if (!m_patternCtrl.init(path, ftags, fvariables, fpatterns, L))
		{
			logger.info("load tag.define, variable.define, pattern.define file fail.");
			return false;
		}
		
		_tag_ctrl = m_patternCtrl.getTagCTRL();
		
		//鍔犺浇璇嶅吀鏂囦欢
		m_dictTag = new dictTag();
		m_dictTag.initialize(path, fdicts);
		
		return true;
	}
	
	//澶勭悊鍑芥暟鍔犱笂鍚屾鍏抽敭瀛�,绾跨▼瀹夊叏
	public synchronized String doProcess(String input, JSONObject stat)
	{
		String res = "";
		try
		{
			m_stateCtrl.setState(stat);
			Vector<PatternResult> hit_ptns = new Vector();
			
			//1. query 棰勫鐞�
			input = input.replaceAll(" ", "").toUpperCase();
			
			Sentence sentence = new Sentence(input);
			
			//2. 鎵撴爣
			m_dictTag.locateAndInfer(sentence, _tag_ctrl);
			/*System.out.println("####HIT TAG####");
			for (TOKEN token :sentence.m_tokens)
			{
				System.out.println(token.TagIDs.toString());
			}
			System.out.println("################");
			*/
			
			//3. pattern鍖归厤
			m_patternCtrl.doMatchPattern(sentence, hit_ptns);
			
			//4. params璧嬪��
			ExpParams params = new ExpParams(L, m_stateCtrl);
			params.input_length = input.length();
			params.input = sentence;
			params.p_ptn_ctrl = m_patternCtrl;
			params.p_hit_ptns = hit_ptns;
			
			//5.鎵ц 
			/* TODO:
			 * 瀹炵幇鍔熻兘:pattern鍙互缁勫悎(涓庢垨闈�)缁勬垚entry, 姣忎釜entry鏈変釜浼樺厛绾�
			 * 閫夋嫨婊¤冻鏉′欢骞朵笖浼樺厛绾ф渶楂樼殑entry,鐒跺悗鎵цpattern.
			 * 瀵瑰彉閲忚祴鍊兼渶缁堟牴鎹甿apping.define閲岄潰鐨勬槧灏勮緭鍑虹粨鏋�*/
			//ent_ctrl.matchEntrances(null, params);
			
			/* 
			 * 鍏堢畝鍗曞疄鐜�, pattern鍘婚噸鍚庢墽琛屾墍鏈塸attern瀵瑰彉閲忚祴鍊�.
			 * 瀵逛簬澶嶆潅鐨剄uery, 浼氬悓鏃跺懡涓涓熀纭�pattern鍜屼粬浠殑缁勫悎.
			 * 鍛戒腑澶氫釜pattern鏃�, 浼樺厛绾т负榛樿鍊�(50)鏃堕兘鎵ц, 瀵逛簬鍗婂勾鎶ャ�佸勾鎶�
			 * 杩欑被鍖呭惈鍏崇郴鐨剄uery,閫夋嫨浼樺厛绾ц緝楂樼殑涓�涓�(瀵逛簬瀛樺湪姝т箟鐨刾attern,
			 * 浼樺厛绾ч兘璁剧疆姣旈粯璁や紭鍏堢骇楂�)
			 * 绠�鍗曡В鍐虫柟妗�, 淇濈暀绾у埆鏈�楂樼殑pattern, 绾у埆灏辨槸pattern鐨剋eight
			*/
			//System.out.println("####HIT AND SELECT PATTERN####");
			m_patternCtrl.selectPatterns1(params, patternSelectMode.BYPRIORITY);
			//System.out.println("################");
			if (null != stat && !stat.isEmpty() && null != params.hit_tags)
			{
				if (params.input_length > 0) {
					params.p_dsc.jv_stat.put("coverage", params.coverage / (float) params.input_length);
				}
				else
				{
					params.p_dsc.jv_stat.put("coverage", 0);

				}
			}
			
			//6. output;
			logger.info(String.format("cover chars:%f, lenght:%d",  params.coverage,params.input_length));
			res = params.p_dsc.jv_stat.toString();
		}
		catch (Exception e)
		{
			logger.info("error", e);
		}
		return res;
	}
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PropertyConfigurator.configure("C:/Users/luovang/workspace/intention/resources/log4j.properties");
		//StockAlias.init();
		recongnizeEngine engine = new recongnizeEngine();		
		JSONObject stat = new JSONObject();
		engine.doProcess("今天星期六", stat);
		System.out.println(stat.toString());
		stat.clear();
	}

}
