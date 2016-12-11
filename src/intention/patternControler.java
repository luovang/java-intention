

package intention;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

//import org.apache.lucene.queries.function.valuesource.VectorValueSource;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.keplerproject.luajava.LuaStateFactory;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;


import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSONObject;

// TODO: Auto-generated Javadoc

class HIT_Y_INFO{
	int yPos;
	int yLength;
	JSONObject normalizations;		//閺嶅洤鍣崠鏍ф倵閻ㄥ嫬灏柊宥嗘瀮閺�?
	int tagID;
};


public class patternControler {
	
	/** The logger. */
	public static Logger logger = Logger.getLogger(patternControler.class);
	
	/** The tag ctrl. */
	//ADD for OFFLINE
	private static tagControler _tag_ctrl;
	
	/** The var ctrl. */
	private static variableControler _var_ctrl;
	
	/** The item ctrl. */
	private static patternItemControler _item_ctrl;
	
	/** The m roots. 姣忎釜pattern 涓殑variable 浼氬瓨鍌ㄥ埌roots, key鏄疺ariable, Value鏄痬_id */
	private static Map<Variable, Vector<Integer>> m_roots = Maps.newHashMap();
	
	/** The m nodes. 姣忎釜pattern鐨刟nalyze鍚庝細瀵瑰簲澶氫釜node */
	private static Vector<pattern> m_nodes = new Vector();
	
	/** The m actions. */
	private static Vector<JSONObject> m_actions = new Vector();
	
	/** The m pattern stats. */
	private Vector<Integer> m_patternStats = new Vector();

	/** The m map con act. */
	private static Map<Integer, Vector<ConditionAction> > m_map_con_act = Maps.newHashMap();
	
	private static int defaultPriority = 50; //pattern 榛樿浼樺厛绾�50
	
	/**
	 *  The l.
	 *
	 * @param path the path
	 * @param ftags the ftags
	 * @param fvariables the fvariables
	 * @param fpatterns the fpatterns
	 * @return true, if successful
	 */
		
	public tagControler getTagCTRL()
	{
		return _tag_ctrl;
	}
	
	public Map<Integer, Vector<ConditionAction> > getPtnAcitons()
	{
		return m_map_con_act;
	}
	
	public int doPatternActions(ExpParams params)
	{
		Map<Integer, Vector<ConditionAction> > map_ptn_act = getPtnAcitons();
		PatternResult final_ptn;
		int max_score = 0;
		int index = -1;;
		
		for (int i = 0; i < params.p_hit_ptns.size(); i++)
		{
			System.out.println(String.format("hit pattern gid:%d\tscore:%d",
					params.p_hit_ptns.get(i)._pat.m_gid, params.p_hit_ptns.get(i)._score));
			if (params.p_hit_ptns.get(i)._score > max_score)
			{
				index = i;
			}
		}
		if (index < 0)
		{
			return -1;
		}
		final_ptn = params.p_hit_ptns.get(index);
		System.out.println(String.format("final hit pattern:%d\tscore:%d", final_ptn._pat.m_gid, final_ptn._score));
		int gid = final_ptn._pat.m_gid;
		Vector<ConditionAction> v_act_con = map_ptn_act.get(gid);
		params._ptn_res = final_ptn;
		for (ConditionAction act_con : v_act_con)
		{
			act_con.execute(params);
		}
		return 0;
	}
	
	public int selectPatterns(ExpParams params, patternSelectMode type)
	{
		Map<Integer, Vector<ConditionAction> > map_ptn_act = getPtnAcitons();
		//Map<String, Map<Integer, PatternResult>> map_hit_ptns = Maps.newHashMap();
		Map<Integer, PatternResult> map_hit_ptns = Maps.newHashMap();
		Vector<PatternResult> final_ptns = new Vector();
		int max_score  = 0;
		int max_score_index = 0;
		int max_priority = 0;
		int index = 0;
		// pattern鍘婚噸, 鐩稿悓GID pattern 淇濈暀寰楀垎鏈�楂�
		for (PatternResult pat : params.p_hit_ptns)
		{
			if (pat._score > max_score)
			{
				max_score = pat._score;
				max_score_index = index;
			}
			if (pat._pat.m_priority > max_priority)
			{
				max_priority = pat._pat.m_priority;
			}
			
			if (map_hit_ptns.containsKey(pat._pat.m_gid))
			{
				if (pat._score > map_hit_ptns.get(pat._pat.m_gid)._score)
				{
					map_hit_ptns.put(pat._pat.m_gid, pat);
				}
			}
			else
			{
				map_hit_ptns.put(pat._pat.m_gid, pat);
			}
			index++;
		}
		// pattern select
		if (type == patternSelectMode.BYSCORE) //鍩轰簬pattern鍒嗗��
		{
			final_ptns.add(params.p_hit_ptns.get(max_score_index));
		}
		else if (type == patternSelectMode.BYPRIORITY) //鍩轰簬pattern浼樺厛绾�
		{
			for (PatternResult pat : map_hit_ptns.values())
			{
				//淇濈暀榛樿浼樺厛绾�(50)鐨刾attern, 瀵逛簬澶т簬50鐨刾attern浼樺厛绾у彇鏈�楂樼殑
				if (pat._pat.m_priority == max_priority || pat._pat.m_priority == defaultPriority)
				{
					final_ptns.add(pat);
				}
			}
		}
		
		if (final_ptns.size() == 0)
		{
			logger.info("no pattern hit.");
			return -1;
		}
		//鎵цpattern 骞惰绠楄鐩栫巼
		params.hit_tags = new HashSet();
		for (PatternResult final_ptn : final_ptns)
		{
			logger.info(String.format("final hit pattern:%d\tpriority:%d\tscore:%d", final_ptn._pat.m_gid, final_ptn._pat.m_priority ,final_ptn._score));
			int gid = final_ptn._pat.m_gid;
			Vector<ConditionAction> v_act_con = map_ptn_act.get(gid);
			params._ptn_res = final_ptn;
			for (ConditionAction act_con : v_act_con)
			{
				act_con.execute(params);
			}
			//瑕嗙洊鐨勫瓧娈�
			//params.hit_tags.addAll(params.input.m_sentence.substring(final_ptn._b, final_ptn._e + 1));
			params.hit_tags.add(params.input.m_sentence.substring(final_ptn._b, final_ptn._e + 1));
			//System.out.println(params.input.m_sentence.substring(final_ptn._b, final_ptn._e + 1));
			//System.out.println(String.format("length:%d", final_ptn._e - final_ptn._b + 1));
		}
		
		return 0;
	}
	
	public int selectPatterns1(ExpParams params, patternSelectMode type)
	{
		Map<Integer, Vector<ConditionAction> > map_ptn_act = getPtnAcitons();
		Map<String, Map<Integer, PatternResult>> map_hit_ptns = Maps.newHashMap();
		Vector<PatternResult> final_ptns = new Vector();
		Map<String, Integer> map_max_score = Maps.newHashMap();
		Map<String, Integer> map_max_score_index = Maps.newHashMap();
		Map<String, Integer> map_max_priority = Maps.newHashMap();
		int index = 0;
		// pattern鍘婚噸, 鐩稿悓GID pattern 淇濈暀寰楀垎鏈�楂�
		for (PatternResult pat : params.p_hit_ptns)
		{
			/* 璁板綍姣忎釜绫诲埆鏈�澶у緱鍒唒attern鍜屾瘡涓被鍒笅鐨勬渶楂樹紭鍏堢骇 */
			//寰楀垎
			if (map_max_score.containsKey(pat._pat.scene))
			{
				if (pat._score > map_max_score.get(pat._pat.scene))
				{
					map_max_score.put(pat._pat.scene, pat._score);
					map_max_score_index.put(pat._pat.scene, index);
				}
			}
			else
			{
				map_max_score.put(pat._pat.scene, pat._score);
				map_max_score_index.put(pat._pat.scene, index);
			}
			//浼樺厛绾�
			if (map_max_priority.containsKey(pat._pat.scene))
			{
				if (pat._pat.m_priority > map_max_priority.get(pat._pat.scene))
				{
					map_max_priority.put(pat._pat.scene, pat._pat.m_priority);
				}
			}
			else
			{
				map_max_priority.put(pat._pat.scene, pat._pat.m_priority);
			}
			//鍚屼竴Gid淇濆瓨寰楀垎鏈�澶х殑pattern
			if (map_hit_ptns.containsKey(pat._pat.scene))
			{
				if (map_hit_ptns.get(pat._pat.scene).containsKey(pat._pat.m_gid))
				{
					if (pat._score > map_hit_ptns.get(pat._pat.scene).get(pat._pat.m_gid)._score)
					{
						map_hit_ptns.get(pat._pat.scene).put(pat._pat.m_gid, pat);
					}
				}
				else
				{
					map_hit_ptns.get(pat._pat.scene).put(pat._pat.m_gid, pat);
				}
			}
			else
			{
				Map<Integer, PatternResult> map = Maps.newHashMap();
				map.put(pat._pat.m_gid, pat);
				map_hit_ptns.put(pat._pat.scene, map);
			}
			index++;
		}
		
		// pattern select 鍩轰簬pattern鍒嗗��
		if (type == patternSelectMode.BYSCORE)
		{
			for (Map.Entry<String, Integer> ent : map_max_score.entrySet())
			{
				final_ptns.add(params.p_hit_ptns.get(ent.getValue()));
			}
		}
		else if (type == patternSelectMode.BYPRIORITY) //鍩轰簬pattern浼樺厛绾�
		{
			for (Map.Entry<String, Map<Integer, PatternResult>> ent : map_hit_ptns.entrySet())
			{
				//姣忎釜绫诲埆鍒烽��
				if (map_max_priority.containsKey(ent.getKey()))
				{
					int sceneScore = 0;
					int gid = -1;
					//淇濈暀榛樿浼樺厛绾�(50)鐨刾attern, 瀵逛簬澶т簬50鐨刾attern浼樺厛绾у彇鏈�楂樼殑,浼樺厛绾х浉鍚屾椂鍙栧緱鍒嗘渶楂樼殑
					//for (PatternResult pat : ent.getValue().values())
					for(Map.Entry<Integer, PatternResult> gid_pat : ent.getValue().entrySet())
					{
						if (gid_pat.getValue()._pat.m_priority == defaultPriority) {
							final_ptns.add(gid_pat.getValue());
						}
						else if (gid_pat.getValue()._pat.m_priority == map_max_priority.get(ent.getKey())) {
							if (gid_pat.getValue()._score > sceneScore) {
								sceneScore = gid_pat.getValue()._score;
								gid = gid_pat.getKey();
							}
						}
					}
					if (gid > -1) { //鎵惧嚭璇ョ被鍒笅鏈�楂樹紭鍏堢骇涓嬪緱鍒嗘渶楂榩attern
						final_ptns.add(ent.getValue().get(gid));
					}
				}
				else
				{
					logger.info(ent.getKey() + ": pattern select error.");
				}
			}
		}
		
		if (final_ptns.size() == 0)
		{
			logger.info("no pattern hit.");
			return -1;
		}
		//鎵цpattern
		params.hit_tags = new HashSet();
		for (PatternResult final_ptn : final_ptns)
		{
			logger.info(String.format("final hit pattern scene:%s\tid:%d\tpriority:%d\tscore:%d", final_ptn._pat.scene, final_ptn._pat.m_gid, final_ptn._pat.m_priority ,final_ptn._score));
			int gid = final_ptn._pat.m_gid;
			Vector<ConditionAction> v_act_con = map_ptn_act.get(gid);
			params._ptn_res = final_ptn;
			for (ConditionAction act_con : v_act_con)
			{
				act_con.execute(params);
			}
			//瑕嗙洊鐨勫瓧娈�
			params.hit_tags.add(params.input.m_sentence.substring(final_ptn._b, final_ptn._e + 1));
		}
		//璁＄畻瑕嗙洊鐜�
		for(String tag : params.hit_tags)
		{
			params.coverage += tag.length();
		}
		logger.info("hit tags:" + params.hit_tags.toString());
		return 0;
	}
	
	
	/**
	 * Inits the.
	 *
	 * @param path the path
	 * @param ftags the ftags
	 * @param fvariables the fvariables
	 * @param fpatterns the fpatterns
	 * @return true, if successful
	 */
	public boolean init(String path, Vector<String>ftags, Vector<String>fvariables, 
			Vector<String>fpatterns, LuaState L)
	{
		//鍔犺浇tag.define
		_tag_ctrl = new tagControler();
		for(String ftag : ftags)
		{
			if(!_tag_ctrl.init(path, ftag))
			{
				logger.error("[ERROR]:Load TAGS Failed");
				return false;
			}
		}
		
		//鍔犺浇variable.define
		_var_ctrl = new variableControler();
		for(String fvariable : fvariables)
		{
			if (!_var_ctrl.init(path, fvariable, _tag_ctrl))
			{
				logger.error("[ERROR]:load Variable Failed");
				return false;
			}
		}
				
		//鍔犺浇pattern.define
		_item_ctrl = new patternItemControler();
		if (!loadPattern(path, fpatterns, L))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Adds the roots.
	 *
	 * @param patt the patt
	 */
	private void addRoots(pattern patt)
	{
		for (int i = 0; i < patt.m_items.size(); i++)
		{
			if (patt.m_items.get(i).searchType == VAR_SEARCH_MODE.SM_VARIABLE)
			{
				if (m_roots.containsKey((patt.m_items.get(i).variable)))
				{
					m_roots.get(patt.m_items.get(i).variable).add(patt.m_id);
				}
				else
				{
					Vector<Integer> vc = new Vector();
					vc.add(patt.m_id);
					m_roots.put(patt.m_items.get(i).variable, vc);
				}
			}
		}
		return;
	}
	
	private String makeOrItem(Stack<String> stack, int b, int e)
	{
		int i,j,k;
		String res = "";
		Vector<String> path = new Vector();
		Vector<String> path_tmp = new Vector();
		path.add("");
		for (i = b; i <= e; i++){
			if (stack.get(i).length() == 0)
				continue;
			path_tmp.clear();
			for (j = 0; j < path.size(); j++){
				String []arr = stack.get(i).split("\\|", -1);
				Vector<String> orItems = new Vector<String>(Arrays.asList(arr));
				for (k=0; k < orItems.size(); k++){
					path_tmp.add(path.get(j) + orItems.get(k));
				}
			}
			path.clear();
			path.addAll(0, path_tmp);
			stack.set(i, "");
		}
		for(i = 0; i < path.size(); i++){
			if (i != 0)
				res += "|";
			res += path.get(i);
		}
		return res;
	}
	
	
	/**
	 * Analyze pattern.
	 *
	 * @param pattern the pattern
	 * @return the vector
	 */
	private Vector<String> analyzePattern(String pattern)
	{
		Vector<String> res;
		int i, j;
		//Vector<String> stack = new Vector();
		Stack<String> stack = new Stack();
		stack.setSize(50);

		int stackTop = -1;
		
		for (i = 0; i < pattern.length(); i++)
		{
			//纰板埌"("鍏ユ爤
			if (pattern.charAt(i) == '(')
			{
				if (stackTop < 0 || stack.get(stackTop).length() != 0)
				{
					stackTop++;
					if (stackTop >= stack.size())
					{
						stack.setSize(stack.size() + 50);
					}
				}
				stack.set(stackTop, "(");
				stackTop++;
				if (stackTop >= stack.size())
				{
					stack.setSize(stack.size() + 50);
				}
				stack.set(stackTop, "");
			}
			else if (pattern.charAt(i) == ')') //纰板埌")"鍑烘爤
			{
				String tmpStr = "";
				int b = 0;
				int e = 0;
				e = stackTop;
				boolean hasContent = false;
				for (j = stackTop; j >= 0; j--)
				{
					if(stack.get(j) == "(") //鎵惧埌鏈�鍐呭眰宸︽嫭鍙�
					{
						b = j + 1;
						if (!hasContent)
						{
							tmpStr = makeOrItem(stack, b, e);
							hasContent = true;
						}
						else
						{
							tmpStr = makeOrItem(stack,b,e) + "|" + tmpStr;
						}
						stack.set(j, tmpStr);
						break;
					}
					else if (stack.get(j) == "|")
					{
						b = j + 1;
						if (!hasContent)
						{
							tmpStr = makeOrItem(stack,b,e);
							hasContent = true;
						}
						else
						{
							tmpStr = makeOrItem(stack,b,e) + "|" + tmpStr;
						}
						stack.set(j, "");
						e = j - 1;
					}
					
				}
				if (j < 0)
				{
					logger.error(String.format("[Error]:Pattern Format is Wrong! %s", pattern));
					return null;
				}
				stackTop  = j + 1;
				stack.set(stackTop, "");
			}
			else if (pattern.charAt(i) == '|')
			{
				stackTop++;
				if (stackTop >= stack.size())
				{
					stack.setSize(stack.size() + 50);
				}
				stack.set(stackTop, "|");
				stackTop++;
				if (stackTop >= stack.size())
				{
					stack.setSize(stack.size() + 50);
				}
				stack.set(stackTop, "");
			}
			else
			{
				stack.set(stackTop, stack.get(stackTop) + pattern.charAt(i));
			}
		}
		if (stackTop != 1)
		{
			logger.error(String.format("[Error]:Pattern Format is Wrong! %s", pattern));
			return null;
		}
		String[] res_arr = stack.get(0).split("\\|");
		res = new Vector<String>(Arrays.asList(res_arr));
		return res;
	}
	
	/**
	 * Load pattern.
	 *
	 * @param path the path
	 * @param f_patterns the f patterns
	 * @param L the l
	 * @return true, if successful
	 */
	public boolean loadPattern(String path, Vector<String> f_patterns, LuaState L)
	{
		m_nodes.clear();
		PatternItem item;
		pattern patt;
		Set<String> uni_patts = Sets.newHashSet();
		Set<String> uni_patts_gid = Sets.newHashSet();
		ConditionAction con_act = null;
		int gid = -1;
		int pos;
		String str_scene = "";
		String line = "";
		int line_num = 0;
		STATE_LOAD_FILE load_state = STATE_LOAD_FILE.SLF_NORMAL;
		
		for (String f_pattern : f_patterns)
		{
			String file = path + f_pattern;
			try
			{
				@SuppressWarnings("resource")
				BufferedReader reader = new BufferedReader(new FileReader(new File(file)));
				while ((line = reader.readLine()) != null)
				{
					line_num++;
					line = line.trim();
					if (line.length() == 0 || line.charAt(0) == '#') continue;
					//GID
					if (line.indexOf("GID:") == 0)
					{
						gid = Integer.parseInt(line.substring(4));
						if (str_scene.equals(""))
						{
							logger.error(String.format("[ERROR]cant define pattern without scene,define scene first!in file:%s:%d", file, line_num));
							return false;
						}
						if (gid < 0)
						{
							logger.error(String.format("[ERROR]invalid gid!in file:%s:%d", file, line_num));
							return false;
						}
						continue;
					}
					//PAT:
					else if (line.indexOf("PAT:") == 0)
					{
						if(gid <= 0){
							logger.error(String.format("[ERROR]cant define pattern without gid,define GID first!in file:%s:%d", file, line_num));
							return false;
						}
						if(load_state != STATE_LOAD_FILE.SLF_NORMAL){
							logger.error(String.format("[ERROR]load state error,check condition IF END!in file:%s:%d", file, line_num));
							return false;					
						}
						int priority = 50;
						String f_pat = line.substring(4);
						pos = f_pat.indexOf("\t");
						if (pos > -1)
						{
							priority = Integer.parseInt(f_pat.substring(pos + 1));
							f_pat = f_pat.substring(0, pos);
						}
						//analyzePattern
						Vector<String> exp_patts = analyzePattern("(" + f_pat + ")");
						for (String exp_patt : exp_patts)
						{
							String exp_gid = exp_patt + String.valueOf(gid);
							if (uni_patts_gid.contains(exp_gid))
							{
								logger.error(String.format("[WARNING]:Pattern Repeat->%s", exp_patt));
								continue;
							}
							else
							{
								uni_patts_gid.add(exp_gid);
							}
							if (!uni_patts.contains(exp_patt))
							{
								uni_patts.add(exp_patt);
							}
							patt = new pattern();
							//pattern浼樺厛绾у垽鏂�
							if (priority < 50) {
								logger.error("[ERROR]:pattern priority must't less than 50!");
								return false;
							}
							patt.m_priority = priority;
							patt.m_text = exp_patt;
							patt.m_id = m_nodes.size();
							patt.m_gid = gid;
							patt.scene = str_scene;
							if (!patt.init(_var_ctrl, _item_ctrl))
							{
								logger.error("[ERROR]:pattern init failed!");
								return false;
							}
							m_nodes.add(patt);
							addRoots(patt);
						}
						exp_patts.clear();
					}
					// SCE:
					else if (line.indexOf("SCE:") == 0)
					{
						str_scene = line.substring(4);
					}
					//IF:
					else if (line.indexOf("IF:") == 0)
					{
						if(load_state != STATE_LOAD_FILE.SLF_NORMAL || gid <= 0)
						{
							logger.error(String.format("[ERROR]IF state error, please check config !in file:%s:%d", file, line_num));
							return false;
						}
						con_act = new ConditionAction();
						if (!m_map_con_act.containsKey(gid))
						{
							Vector<ConditionAction> vca = new Vector();
							vca.add(con_act);
							m_map_con_act.put(gid, vca);
						}
						else
						{
							m_map_con_act.get(gid).add(con_act);
						}
						load_state = STATE_LOAD_FILE.SLF_IF;
					}
					//THEN:
					else if (line.indexOf("THEN:") == 0)
					{
						if (load_state != STATE_LOAD_FILE.SLF_IF_OK || gid <= 0 || null == con_act)
						{
							logger.error(String.format("[ERROR]THEN state error, please check config !in file:%s:%d", file, line_num));
							return false;
						}
						load_state = STATE_LOAD_FILE.SLF_THEN;
					}
					//ELSE:
					else if (line.indexOf("ELSE:") == 0)
					{
						if (load_state != STATE_LOAD_FILE.SLF_THEN_OK || gid <= 0 || null == con_act)
						{
							logger.error(String.format("[ERROR]ELSE state error, please check config !in file:%s:%d", file, line_num));
							return false;
						}
						load_state = STATE_LOAD_FILE.SLF_ELSE;
					}
					//END
					else if (line.indexOf("END") == 0)
					{
						if ((load_state != STATE_LOAD_FILE.SLF_THEN_OK && load_state != STATE_LOAD_FILE.SLF_ELSE_OK) || gid <= 0 || null == con_act)
						{
							logger.error(String.format("[ERROR]END state error, please check config !in file:%s:%d", file, line_num));
							return false;
						}
						con_act = null;
						load_state = STATE_LOAD_FILE.SLF_NORMAL;
					}
					//澶勭悊姝ｆ枃
					else
					{
						switch(load_state){
							case SLF_IF:
							{
								Condition new_con = new Condition();
								new_con.setExpression(line);
								if (new_con.init(L, str_scene) < 0)
								{
									logger.error(String.format("[ERROR]exp init error, please check config !in file:%s:%d", file, line_num));
									return false;
								}
								con_act.setCondition(new_con);
								load_state = STATE_LOAD_FILE.SLF_IF_OK;
								break;
							}
							case SLF_THEN:
								load_state = STATE_LOAD_FILE.SLF_THEN_OK;
							case SLF_THEN_OK:
							{
								Action new_act_if = new Action();
								new_act_if.setActionLine(line);
								if (!new_act_if.initAction(L, str_scene))
								{
									logger.error(String.format("[ERROR]exp init error, please check config !in file:%s:%d", file, line_num));
									return false;
								}
								con_act.addActionIf(new_act_if);
								break;
							}
							case SLF_ELSE:
								load_state = STATE_LOAD_FILE.SLF_ELSE_OK;
							case SLF_ELSE_OK:
							{
								Action new_act_else = new Action();
								new_act_else.setActionLine(line);
								if (!new_act_else.initAction(L, str_scene))
								{
									logger.error(String.format("[ERROR]exp init error, please check config !in file:%s:%d", file, line_num));
									return false;
								}
								con_act.addActionElse(new_act_else);
								break;
							}
							default:
								logger.error(String.format("[ERROR]unknown line!in file:%s:%d", file, line_num));
								break;
						}
					}
				}
				reader.close();
			}
			catch (Exception e)
			{
				logger.error(String.format("cant load pattern file:%s.", file));
				return false;
			}
			uni_patts.clear();
		}
		return true;
	}
	
	private boolean matchConstItem(Sentence y, String itemName, Vector<Vector<HIT_Y_INFO>> hitInfos)
	{
		int i = 0;
		int pos;
		Vector<HIT_Y_INFO> hitYs = new Vector();
		while(i < y.m_sentence.length()){
			pos = y.m_sentence.indexOf(itemName, i);
			if (pos < 0)
				break;
			HIT_Y_INFO hitInfo = new HIT_Y_INFO();
			hitInfo.yPos = pos;
			hitInfo.yLength = itemName.length();
			hitInfo.normalizations = null;
			hitInfo.tagID = -1;
			hitYs.add(hitInfo);
			i = pos + itemName.length();
		}
		if (hitYs.size() == 0)
			return false;
		hitInfos.add(hitYs);
		return true;
	}
	
	private boolean matchAllXItem(Sentence y, pattern pat, Map<String, Vector<HIT_Y_INFO>> cache, Vector<Vector<HIT_Y_INFO>> hitInfos)
	{
		int i;
		for(PatternItem item : pat.m_items)
		{
			if (item.searchType == VAR_SEARCH_MODE.SM_CONST)
			{
				String curWord = item.name;
				if (cache.containsKey(curWord))
				{
					hitInfos.add(cache.get(curWord));
				}
				else
				{
					if (!matchConstItem(y, curWord, hitInfos))
					{
						return false;
					}
					else
					{
						cache.put(curWord, hitInfos.lastElement());
					}
				}
			}
			else if(item.searchType == VAR_SEARCH_MODE.SM_BEGIN)
			{
				HIT_Y_INFO beginHYI = new HIT_Y_INFO();
				beginHYI.yPos = 0;
				beginHYI.yLength = 0;
				beginHYI.normalizations = null;
				beginHYI.tagID = -1;
				Vector<HIT_Y_INFO> tmp = new Vector();
				tmp.add(beginHYI);
				hitInfos.add(tmp);
			}
			else if (item.searchType == VAR_SEARCH_MODE.SM_END)
			{
				HIT_Y_INFO endHYI = new HIT_Y_INFO();
				endHYI.yPos = y.m_sentence.length();
				endHYI.yLength = 0;
				endHYI.normalizations = null;
				endHYI.tagID = -1;
				Vector<HIT_Y_INFO> tmp = new Vector();
				tmp.add(endHYI);
				hitInfos.add(tmp);
			}
			else if(item.searchType == VAR_SEARCH_MODE.SM_ALL)
			{
				HIT_Y_INFO allHYI = new HIT_Y_INFO();
				allHYI.yPos = 0;
				allHYI.yLength = y.m_sentence.length();
				allHYI.normalizations = null;
				allHYI.tagID = 0;
				Vector<HIT_Y_INFO> tmp = new Vector();
				tmp.add(allHYI);
				hitInfos.add(tmp);
			}
			else if (item.searchType == VAR_SEARCH_MODE.SM_VARIABLE)
			{
				String curWord = item.variable.name;
				if (cache.containsKey(curWord))
				{
					hitInfos.add(cache.get(curWord));
				}
				else
				{
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean makeResult(Sentence y, pattern pat, Vector<Vector<HIT_Y_INFO>> hitInfos, Vector<PatternResult> result)
	{
		int i,j,k;
		Vector<Vector<HIT_Y_INFO>> old_res = new Vector();
		Vector<Vector<HIT_Y_INFO>> new_res = new Vector();
		Vector<HIT_Y_INFO> tmpPath = new Vector();
		HIT_Y_INFO tmpHitInfo;
		
		for(i = 0; i < hitInfos.get(0).size(); i++){
			tmpPath.clear();;
			tmpHitInfo = hitInfos.get(0).get(i);
			tmpPath.add(tmpHitInfo);
			old_res.add((Vector<HIT_Y_INFO>) tmpPath.clone());
		}
		
		for(i = 1;i < hitInfos.size(); i++){
			if(old_res.size() == 0)break;
			new_res.clear();
			for(k = 0; k< old_res.size(); k++){
				int lastEndPos = (old_res.get(k).lastElement()).yPos + (old_res.get(k).lastElement()).yLength;
				for(j = 0; j < hitInfos.get(i).size(); j++){
					//妫�鏌ユ柊Path node鏄惁鍚堟硶,浠ュ悗闇�瑕佹敼杩�
					if (hitInfos.get(i).get(j).yPos != lastEndPos){
						if (pat.m_items.get(i-1).searchType == VAR_SEARCH_MODE.SM_ALL){	//鍓嶉潰鑺傜偣鏄惁鏄�$[*]
							int yLength = hitInfos.get(i).get(j).yPos - (old_res.get(k).lastElement()).yPos;
							if (yLength > 0)
								(old_res.get(k).lastElement()).yLength = yLength;
							else
								continue;
						}else if (pat.m_items.get(i).searchType == VAR_SEARCH_MODE.SM_ALL){			//褰撳墠鑺傜偣鏄惁鏄�$[*]
							if (y.m_sentence.length()==lastEndPos)
								continue;
							hitInfos.get(i).get(j).yPos = lastEndPos;
							hitInfos.get(i).get(j).yLength = y.m_sentence.length()-lastEndPos;
						}else{
							continue;
						}
					}
					//娣诲姞鏂皃ath node
					tmpPath.clear();
					tmpPath = (Vector<HIT_Y_INFO>)old_res.get(k).clone();
					tmpHitInfo = hitInfos.get(i).get(j);
					tmpPath.add(tmpHitInfo);
					new_res.add((Vector<HIT_Y_INFO>) tmpPath.clone());
				}
			}	//j
			old_res = (Vector<Vector<HIT_Y_INFO>>) new_res.clone();
		}	//i
		new_res.clear();
		
		if (old_res.size()<0)
			return false;
		//璁＄畻姣忔潯缁撴灉HITPATH鐨勫緱鍒�
		for(i = 0; i < old_res.size();i++){
			int score = 0;
			PatternResult res = new PatternResult();
			for (j = 0;j < pat.m_items.size(); j++){
				if (pat.m_items.get(j).variable != null){
					score += old_res.get(i).get(j).yLength*pat.m_items.get(j).variable.weight;
				}else{
					score += old_res.get(i).get(j).yLength*100;
				}
				if (pat.m_items.get(j).searchType != VAR_SEARCH_MODE.SM_ALL && pat.m_items.get(j).searchType != VAR_SEARCH_MODE.SM_VARIABLE)
					continue;
				HIT_Y_INFO cur_item = old_res.get(i).get(j);
				//鍙橀噺鏈韩
				res._hit.put(pat.m_items.get(j).name, y.m_sentence.substring(cur_item.yPos, cur_item.yPos + cur_item.yLength));
				//鍙橀噺鍖归厤TAG
				res._hit_tag.put(pat.m_items.get(j).name, cur_item.tagID);
				//鍙橀噺鐨勫綊涓�鍖�
				if (cur_item.normalizations == null)
					continue;
				res._hit_json.put(pat.m_items.get(j).name, cur_item.normalizations);
			}
			
			res._pat = pat;
			res._score = (int) (score*(((double)res._pat.m_priority)/100.0));//鏃犺pat浼樺厛绾� + res._pat->m_priority*10000;
			//璁＄畻鍖归厤濮嬬粓鐐�
			res._b = old_res.get(i).get(0).yPos;
			j--;
			res._e = old_res.get(i).get(j).yPos + old_res.get(i).get(j).yLength-1;
			result.add(res);
		}
		return true;
	}
	
	private void matchNodes(Sentence input, pattern node, Map<String, Vector<HIT_Y_INFO>> cache, Vector<PatternResult> result)
	{
		if (m_patternStats.get(node.m_id) != null)
		{
			return;
		}
		PatternResult res;
		//hitInfo 椤哄簭璁板綍node(pattern)涓殑鎵�鏈塸atternItem
		//matchAllXItem 濡傛灉pattern涓璸atternItem鍙橀噺绫诲瀷涓嶅湪cache涓� false
		Vector<Vector<HIT_Y_INFO>> hitInfos = new Vector();
		if (matchAllXItem(input, node, cache, hitInfos))
		{
			if (makeResult(input, node, hitInfos, result))
			{
				m_patternStats.set(node.m_id, 1);
			}
			else
			{
				m_patternStats.set(node.m_id, -1);
			}
		}
		else
		{
			m_patternStats.set(node.m_id, -1);
		}
	}
	
	
	private boolean matchVarItem(Sentence y, Variable var, Vector<Integer> mapping, Map<String, Vector<HIT_Y_INFO>> cache)
	{
		int i,j,k;
		boolean res = false;
		Vector<HIT_Y_INFO> hitYs = new Vector();
		HIT_Y_INFO curHit;
		JSONObject hitNormal = null;
		
		int tSize = y.m_tokens.size();
		for(i=0;i < tSize;i++){
			curHit = new HIT_Y_INFO();
			boolean isHitOneToken = false;

			boolean isHit = true;
			if (!y.m_tokens.get(i).TagIDs.contains(var.tagId)){
				isHit = false;
			}
			if (isHit){
				isHitOneToken = true;
				hitNormal = y.m_tokens.get(i).getNormalization(var.tagId);
				curHit.normalizations = hitNormal;
				curHit.tagID = var.tagId;
			}
			
			if (isHitOneToken){
				res = true;
				curHit.yPos = y.m_tokens.get(i).pos;
				curHit.yLength = y.m_tokens.get(i).len;
				hitYs.add(curHit);
			}
		}	//y.m_token
		cache.put(var.name, hitYs);
		
		//璁剧疆涓嶅彲鑳藉尮閰嶄笂鐨勮妭鐐圭殑鏍囧織  mapping涓簃_ids
		if (!res){
			for(i=0; i < mapping.size(); i++){
				m_patternStats.set(mapping.get(i), -1);
			}
		}
		return res;
	}
	
	public boolean doMatchPattern(Sentence sent, Vector<PatternResult> res)
	{
		//涓�涓狦ID鏈�缁堟媶鍒嗕负澶氫釜pattern, m_id, 姣忎釜pattern鍖呭惈澶氫釜PatternItem
		//PatternItem 鍖呭惈鍙橀噺, 甯搁噺绛�
		//m_nodes 瀛樺偍pattern
		//m_roots 瀛樺偍<Variable v, m_ids>
		m_patternStats.clear();
		m_patternStats.setSize(m_nodes.size());
		// 鍖归厤鎵�鏈夌殑鍗曞彉閲�, 鐢熸垚cache   cache key涓簍ag name, value涓哄懡涓殑token
		Map<String, Vector<HIT_Y_INFO>> cache = Maps.newHashMap();
		for (Map.Entry<Variable, Vector<Integer>> root : m_roots.entrySet())
		{
			matchVarItem(sent, root.getKey(), root.getValue(), cache);
		}
		// pattern鍖归厤
		for (pattern node : m_nodes)
		{
			matchNodes(sent, node, cache, res);
		}
		return true;
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws LuaException 
	 */
	public static void main(String[] args) throws LuaException {
		// TODO Auto-generated method stub
	    LuaState L = LuaStateFactory.newLuaState();
	    L.openLibs();
	   
	    int err = L.LdoFile("src/main/java/com/datayes/search/intentionrecongnize/hello.lua");
	    if(err != 0)
	    {
	    	System.out.println("load file fail.");
	    }
	    String func = "function sub(a,b) return a-b end";
	    err = L.LloadBuffer(func.getBytes(), "line");
	    L.pcall(0, 0, 0);
	    
	    L.getField(LuaState.LUA_GLOBALSINDEX, "sub");
	    
	    L.pushNumber(100);  
        
        // 鍙傛暟2鍘嬫爤  
        L.pushNumber(50);  
          
        // 璋冪敤锛屽叡2涓弬鏁�1涓繑鍥炲��  
        L.call(2, 1);  
        // 淇濆瓨杩斿洖鍊煎埌result涓�  
        L.setField(LuaState.LUA_GLOBALSINDEX, "result");  
          
        // 璇诲叆result  
        LuaObject lobj = L.getLuaObject("result");  
        
        // 鎵撳嵃缁撴灉   
        System.out.println(lobj.getNumber()); 
	    
	    
        func = "function f0(v0, v1) return  v0, v1 end";
	    err = L.LloadBuffer(func.getBytes(), "line");
	    L.pcall(0, 0, 0);
	    
	    /*
	    L.getField(LuaState.LUA_GLOBALSINDEX, "f0");
	    L.pushNumber(100);  
	    L.pushNumber(10);  
	    L.call(1, 1); 
	    L.setField(LuaState.LUA_GLOBALSINDEX, "result");  
	    lobj = L.getLuaObject("result"); 
	    System.out.println(lobj.getBoolean()); 
	    */
	    System.out.println("test.");
	    L.setTop(0);
	    L.getGlobal("f0");
	    L.pushNumber(100);
	    L.pushNumber(10);
	    L.call(2, 2);
	    double a = L.toNumber(-1);
	    double b = L.toNumber(-2);
	    System.out.println(String.format("return: %f, %f", a, b));
	    
	    System.out.println("test1.");
	    L.setTop(0);
	    L.getGlobal("f0");
	    L.pushNumber(110);
	    L.pushNumber(11);
	    L.pcall(2, 2, 0);
	    Object a1 =  L.toJavaObject(-1);
	    
	    Object b1 =  L.toJavaObject(-2);
	    System.out.println(String.format("return test1: %f, %s", a1, b1));
	    
        //---------------------------------------------鍊间紶閫掓祴璇�  
        // 鎵惧埌鍑芥暟 sum  
        L.getField(LuaState.LUA_GLOBALSINDEX, "sum");

        // 鍙傛暟1鍘嬫爤  
        L.pushNumber(100);  
          
        // 鍙傛暟2鍘嬫爤  
        L.pushNumber(50);  
          
        // 璋冪敤锛屽叡2涓弬鏁�1涓繑鍥炲��  
        L.call(2, 1);  
        System.out.println("call finish.");
        // 淇濆瓨杩斿洖鍊煎埌result涓�  
        L.setField(LuaState.LUA_GLOBALSINDEX, "result");  
          
        // 璇诲叆result  
        LuaObject lobj1 = L.getLuaObject("result");  
        
        
        // 鎵撳嵃缁撴灉   
        System.out.println(lobj1.getNumber());  
        L.close();
	    System.out.println("Hello World from Java!");
	    
	}

}
