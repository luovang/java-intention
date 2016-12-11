
package intention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

enum VarLifeType{
	LIFE_APP,
	LIFE_SESSION,
	LIFE_SCENE,
	LIFE_REQUEST,
	LIFE_UNKNOWN
};

enum StateVarType{
	SV_TYPE_SINGLE,
	SV_TYPE_JSON
};


class StateVar{
	int id;
	String scene;
	String name;
	StateVarType type;
	VarLifeType life_type;
};


public class stateCtrl {

	public static Logger logger = Logger.getLogger(stateCtrl.class);
	
	public static int id_cnt;
	public static Vector<StateVar> var_list = new Vector();
	public static Map<String, Integer> map_full_id = Maps.newHashMap();
	public JSONObject jv_stat;
	
	//鍔犺浇mapping 鍙橀噺鏂囦欢, 璇ュ彉閲忕敤浜庡瓨鍌ㄨ绠楃粨鏋滃拰鏈�缁坥utput
	@SuppressWarnings("resource")
	public static boolean loadFiles(String path, Vector<String> f_states)
	{
		id_cnt = 0;
		String[] vec_str;
		BufferedReader reader = null;
		String line = "";
		int line_num = 0;
		String scene = "";
		try{
			for (String f_state : f_states)
			{
				line_num = 0;
				scene = "";
				reader = new BufferedReader(new FileReader(new File(path + f_state)));
				while ((line = reader.readLine()) != null)
				{
					line_num++;
					if (line.length() == 0) continue;
					if (line.charAt(0) == '#') continue;
					if (line.contains("SCE:"))
					{
						scene = line.substring(4);
						continue;
					}
					if (scene.equals(""))
					{
						logger.error(String.format("[ERROR]scene define required ,in file:%s:%d", f_state, line_num));
						return false;
					}
					vec_str = line.split("\t");
					StateVar new_var = new StateVar();
					if (vec_str.length >= 2)
					{
						new_var.life_type = VarLifeType.values()[Integer.parseInt(vec_str[1])];
					}
					if (vec_str.length == 3)
					{
						if (vec_str[2].equals("SINGLE"))
						{
							new_var.type = StateVarType.SV_TYPE_SINGLE;
						}
						else if (vec_str[2].equals("JSON"))
						{
							new_var.type = StateVarType.SV_TYPE_JSON;
						}
						else
						{
							logger.error(String.format("[ERROR]bad var type define ,in file:%s:%d", f_state, line_num));
							return false;
						}
					}
					new_var.name = vec_str[0];
					new_var.scene = scene;
					new_var.id = id_cnt;
					String full = scene  +"::" + new_var.name;
					if (!map_full_id.containsKey(full))
					{
						id_cnt++;
						map_full_id.put(full, new_var.id);
					}
					var_list.add(new_var);
				}
			}
		}
		catch (Exception e)
		{
			logger.error("load mapping.stat file:%s fail.");
			return false;
		}
		return true;
	}
	
	public static int getIdByNameSce(String name, String scene)
	{
		String full = scene + "::" + name;
		if (map_full_id.containsKey(full))
		{
			return map_full_id.get(full);
		}
		return -1;
	}
	
	
	private boolean addVar(String name, String scene, VarLifeType life_type, StateVarType type)
	{
		String s_var_fullname=scene+"::"+name;
		StateVar var = new StateVar();
		var.name = name;
		var.id = id_cnt;
		var.type = type;
		var.life_type = life_type;
		var.scene = scene;
		if (!map_full_id.containsKey(s_var_fullname))
		{
			var_list.add(var);
			map_full_id.put(s_var_fullname, id_cnt);
			id_cnt++;
		}
		else
		{
			logger.error(String.format("[ERROR]duplicat var:%s", s_var_fullname));
			return false;
		}
		return true;
	}
	
	public void setState(JSONObject jv)
	{
		jv_stat = jv;
	}
	
	public void setVarValue(int id, Object value)
	{
		if (id > id_cnt || id < 0)
		{
			logger.error(String.format("[WARNING]state var more than id_cnt:%d\tcnt:%d", id_cnt, id));
			return;
		}
		if (jv_stat.isEmpty())
		{
			JSONObject obj = new JSONObject();
			obj.put(var_list.get(id).name, value);
			jv_stat.put(var_list.get(id).scene, obj);
		}
		//else if (jv_stat.getJSONObject(var_list.get(id).scene).isEmpty())
		else if (!jv_stat.containsKey(var_list.get(id).scene))
		{
			JSONObject obj = new JSONObject();
			obj.put(var_list.get(id).name, value);
			jv_stat.put(var_list.get(id).scene, obj);
		}
		else
		{
			JSONObject obj = jv_stat.getJSONObject(var_list.get(id).scene);
			obj.put(var_list.get(id).name, value);
			jv_stat.put(var_list.get(id).scene, obj);
			
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "hello, world. 楠嗘柟";
		Vector<String> vc = new Vector(Arrays.asList(str.split("|")));
		System.out.println(vc.toString());
		
		Vector<Vector<String>> a = new Vector();
		Vector<String> item  = new Vector();
		item.add("luo");
		a.add(item);
		item.add("fang");
		a.add(item);
		Vector<Vector<String>> b = new Vector(); 
		b = (Vector<Vector<String>>) a.clone();
		a.clear();
		return;

	}

}
