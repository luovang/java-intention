
package intention;

import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;



// TODO: Auto-generated Javadoc

enum ExpVarType{
	EXP_VAR_NORMAL,
	EXP_VAR_HIT_PTN,
	EXP_VAR_HIT_CNT,
	EXP_VAR_HIT_TXT,
	EXP_VAR_HIT_JV,
	EXP_VAR_INTER_CALL,
	EXP_VAR_REQ,
	EXP_VAR_ENT_HIT_PTN,
	EXP_VAR_ENT_COV
};

class ExpParams{
	LuaState L;
	stateCtrl p_dsc;
	JSONObject request;
	JSONObject output;
	Sentence input;
	patternControler p_ptn_ctrl;
	int hit_cnt;
	PatternResult _ptn_res;
	Vector<PatternResult> p_hit_ptns;
	Map<Integer, Integer> p_map_gid_cnt;
	int input_length;
	float coverage; //瑕嗙洊鐜�
	Set<String> hit_tags;
	
	ExpParams(LuaState L, stateCtrl p_dsc)
	{
		this.L = L;
		this.p_dsc = p_dsc;
	}
}

class InterCall{
	String name;
	Vector<JSONObject> params;
	InterCall()
	{
		this.name = "";
	}
};

class ExpressionVar{
	public int id;
	public String name;
	public String scene;
	public Vector<String> var_path;
	public String lua_name;
	public ExpVarType type;
	public Vector<Integer> hit_pattern_id;//have value when type==1
	public InterCall inter_call;
	public int ptn_info_id;
	public int ptn_info_cnt;
	public String ptn_info_param;
	ExpressionVar()
	{
		this.name = "";
		this.scene = "";
	}

};


public class Expression {
	public static Logger logger = Logger.getLogger(Expression.class);
	final static String TAG_TXT = "TAG_TXT ";
	final static String TAG_JV = "TAG_JV ";
	final static String VAR_HEAD = "$[";
	final static String VAR_END = "]";
	final static String ACTION_OUTPUT_STRING = "OUTPUT";
	final static String ACTION_EXIT_ENT_STRING = "EXIT_ENT";
	
	public static int exp_cnt;
	
	public String expression;
	public Vector<ExpressionVar> vars = new Vector();
	public Map<Integer, Integer> map_ptn_act = Maps.newHashMap();
	String expID;
	int res_num;
	
	public Expression()
	{
		this.res_num = 1;
	}
	
	
	public void setResNum(int num)
	{
		res_num = num;
	}
	
	public void setExpression(String exp)
	{
		this.expression = exp;
	}
	
	private String convertToLuaFunction(String exp_id, String expression, String scene)
	{
		expID = exp_id;
		String func_str = "function " + exp_id + "(";
		String cal_str = "return  ";
		String tmpStr = "";
		int pos,posHead,posEnd=-1;
		pos = 0;
		int varcnt=0;
		
		posHead = expression.indexOf(VAR_HEAD);
		if (posHead > -1)
		{
			posEnd = expression.indexOf(VAR_END, posHead);
		}
		while(posHead > -1 && posEnd > -1 && posEnd > posHead)
		{
			if (pos < posHead)
			{
				tmpStr = expression.substring(pos, posHead);
				cal_str += tmpStr;
			}
			tmpStr = expression.substring(posHead+VAR_HEAD.length(), posEnd);
			ExpressionVar new_var = new ExpressionVar();
			new_var.name = tmpStr;
			if (tmpStr.contains(TAG_TXT))
			{
				new_var.name = tmpStr.substring(TAG_TXT.length());
				new_var.type = ExpVarType.EXP_VAR_HIT_TXT;
			}
			else if (tmpStr.contains(TAG_JV))
			{
				new_var.name = tmpStr.substring(TAG_JV.length());
				new_var.type = ExpVarType.EXP_VAR_HIT_JV;
			}
			else
			{
				if (!tmpStr.contains("::"))
				{
					new_var.name = tmpStr;
					new_var.scene = scene;
				}
				else
				{
					String[] arr = tmpStr.split("::");
					if (arr.length !=2)
					{
						logger.error(String.format("var:%s define invalid.", tmpStr));
						return null;
					}
					new_var.scene = arr[0];
					new_var.name = arr[1];
				}
				new_var.type = ExpVarType.EXP_VAR_NORMAL;
				new_var.id = stateCtrl.getIdByNameSce(new_var.name,new_var.scene);
			}
			new_var.lua_name = "v" + String.valueOf(varcnt);
			//
			if(new_var.type == ExpVarType.EXP_VAR_HIT_PTN){
			}
			else if (new_var.type == ExpVarType.EXP_VAR_ENT_HIT_PTN){
				
			}
			else if (new_var.type == ExpVarType.EXP_VAR_INTER_CALL){
				
			}
			varcnt++;
			vars.add(new_var);
			cal_str += new_var.lua_name;
			func_str += new_var.lua_name;
			func_str += ",";
			//get next var
			pos = posEnd + VAR_END.length();
			posEnd = -1;
			posHead = expression.indexOf(VAR_HEAD, pos);
			if (posHead > -1)
			{
				posEnd = expression.indexOf(VAR_END, posHead);
			}
		}
		if (pos < expression.length())
		{
			tmpStr = expression.substring(pos, expression.length());
			cal_str += tmpStr;
		}
		func_str += "dummy_var) ";
		cal_str += " end";
		System.out.println(func_str+cal_str);
		return func_str + cal_str;
	}
	
	private int call_lua_line(LuaState L, String line)
	{
		int error = L.LloadBuffer(line.getBytes(), "line");
		error += L.pcall(0, 0, 0);
		if (0 != error)
		{
			logger.error(String.format("load lua function:%s fail.", line));
		}
		return error;
	}
	
	public int init(LuaState L, String scene)
	{
		String name = "f" + String.valueOf(exp_cnt);
		exp_cnt++;
		int error = call_lua_line(L, convertToLuaFunction(name, expression, scene));
		if (error != 0)
		{
			logger.error(String.format("bad lua expression:%s", expression));
			return -1;
		}
		return 0;
	}
	

	
	public Vector<Object> doScript(ExpParams params)
	{
		Vector<Object> vec_vars = new Vector();
		Map<Integer, Integer> map_hit_cnt;
		if (params.p_map_gid_cnt != null)
		{
			map_hit_cnt = params.p_map_gid_cnt;
		}
		for (ExpressionVar var : vars)
		{
			if (var.type == ExpVarType.EXP_VAR_HIT_TXT)
			{
				String full = VAR_HEAD + var.name + VAR_END;
				if (params._ptn_res._hit.containsKey(full))
				{
					vec_vars.add(params._ptn_res._hit.get(full));
				}
				else
				{
					vec_vars.add("");
				}
			}
			else if (var.type == ExpVarType.EXP_VAR_HIT_JV)
			{
				String[]arr = var.name.split("::");
				if (arr.length != 2)
				{
					logger.error(String.format("json var:%s bad format.", var.name));
					vec_vars.add("");
				}
				
				String full = VAR_HEAD + arr[0] + VAR_END;
				if (params._ptn_res._hit_json.containsKey(full))
				{
					
					vec_vars.add(params._ptn_res._hit_json.get(full).get(arr[1]));
				}
				else
				{
					vec_vars.add("");
				}
			}
			
		}
		
		Vector<Object> ret = new Vector();
		params.L.setTop(0);
		params.L.getGlobal(expID);
		for (Object var : vec_vars)
		{
			if (var instanceof Integer || var instanceof Double)
			{
				params.L.pushNumber((double) var);
			}
			else if (var instanceof String)
			{
				if (var.equals(""))
				{
					params.L.pushNil();
				}
				else
				{
					params.L.pushString((String) var);
				}
			}
			else if (var instanceof Boolean)
			{
				params.L.pushBoolean((Boolean) var);
			}
			else
			{
				logger.error("unknow var type and quit.");
				return null;
			}

		}
		if ( 0 != params.L.pcall(vars.size(), res_num, 0))
		{
			logger.error(String.format("error in execute: %s", expID));
			return null;
		}
		Object obj;
		try{
			for (int i = res_num; i >= 1; i--)
			{
				obj = params.L.toJavaObject(-i);
				ret.add(obj);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.error("do lua script err.");
			return null;
		}
		params.L.setTop(0);
		return ret;
	}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Vector<Object> vc = new Vector();
		vc.add("1");
		vc.add(1);
		vc.add("");
		for (Object ob : vc)
		{
			if (ob instanceof JSONObject)
			{
				System.out.println("Object");
			}
			else if (ob instanceof Integer)
			{
				System.out.println("Integer");
			}
			else if (ob instanceof String)
			{
				System.out.println("String");
				if (ob.equals(""))
				{
					System.out.println("null");
				}
			}
	
		}
		

	}

}
