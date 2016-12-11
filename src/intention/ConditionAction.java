package intention;

import java.util.Arrays;
import java.util.Vector;

import org.keplerproject.luajava.LuaState;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


// TODO: Auto-generated Javadoc
class Condition extends Expression {
	public boolean judge(ExpParams params)
	{
		Vector<Object> jv = doScript(params);
		if (jv.size() > 0 && (jv.get(0) instanceof Boolean))
		{
			return (boolean) jv.get(0);
		}
		return false;
	}
}

enum ActionType{
	ACT_TYPE_ASSIGN,
	ACT_TYPE_EXIT_ENT,
	ACT_TYPE_OUTPUT
};

class Action extends Expression {
	public String action_line;
	
	public Vector<Integer> vec_target_var_id = new Vector();
	public Vector<String> vec_target_state_var = new Vector();
	ActionType act_type;
	
	
	public void setActionLine(String line)
	{
		this.action_line = line;
	}
	
	public int doAction(ExpParams params)
	{
		int i = 0;
		if(act_type == ActionType.ACT_TYPE_EXIT_ENT)
		{
			return 1;
		}else if(act_type == ActionType.ACT_TYPE_OUTPUT){
			//build output json
			//DialogFlowCtrl::buildOutput(params.output,params.p_dsc,output_des);
			return 2;
		}
		Vector<Object> 	exp_value = doScript(params);
		if (exp_value.size() != vec_target_var_id.size())
		{
			logger.info("[WARNING]action return num not match!");
		}
		for (i = 0; i< exp_value.size() && i < vec_target_var_id.size(); i++)
		{
			params.p_dsc.setVarValue(vec_target_var_id.get(i), exp_value.get(i));
		}
		return 0;
	}
	
	public boolean initAction(LuaState L, String scene)
	{
		action_line = action_line.trim();
		if (action_line == ACTION_EXIT_ENT_STRING)
		{
			return true;
		}
		else if (action_line == ACTION_OUTPUT_STRING)
		{
			return true;
		}
		int pos = action_line.indexOf("=");
		if (pos < 0)
		{
			logger.error(String.format("[ERROR]bad act line in sce:%s:%s", scene, action_line));
			return false;
		}
		String target_var_str = action_line.substring(0, pos);
		vec_target_state_var = new Vector<String>(Arrays.asList(target_var_str.split(",")));
		int target_var_id=-1;
		for (String target_state_var : vec_target_state_var)
		{
			target_state_var = target_state_var.trim();
			if(target_state_var.length() <= 3 || target_state_var.charAt(0) !='$' || target_state_var.charAt(1) !='[' || !target_state_var.endsWith("]"))
			{
				logger.error(String.format("bad act line,no target in sce:%s:%s", scene, action_line));
				return false;
			}
			target_var_str = target_state_var.substring(2, target_var_str.length() -1);
			Vector<String> tmpVec = new Vector<String>(Arrays.asList(target_var_str.split("::")));
			if (tmpVec.size() == 1)
			{
				target_var_id = stateCtrl.getIdByNameSce(scene, tmpVec.get(0));
				if (target_var_id < 0)
				{
					logger.error(String.format("bad action var:%s:%s", scene, tmpVec.get(0)));
					return false;
				}
			}
			else if (tmpVec.size() == 2)
			{
				target_var_id = stateCtrl.getIdByNameSce( tmpVec.get(1), tmpVec.get(0));
				if (target_var_id < 0)
				{
					logger.error(String.format("bad action var:%s:%s", tmpVec.get(0), tmpVec.get(1)));
					return false;
				}
			}
			else
			{
				logger.error(String.format("bad act line:%s", action_line));
				return false;
			}
			vec_target_var_id.add(target_var_id);
		}
		setResNum(vec_target_var_id.size());
		expression = action_line.substring( pos + 1);
		act_type = ActionType.ACT_TYPE_ASSIGN;
		return (0 == init(L, scene));
	}
	
}

/**
 * The Class ConditionAction.
 */
public class ConditionAction {

	private Condition con;
	private Vector<Action> v_actions_if = new Vector();
	private Vector<Action> v_actions_else = new Vector();
	
	public int execute(ExpParams params)
	{
		int i = 0;
		int res = 0;
		if (con.judge(params))
		{
			for (Action act : v_actions_if)
			{
				res = act.doAction(params);
			}
		}
		else
		{
			for (Action act : v_actions_else)
			{
				res = act.doAction(params);
			}
		}
		return 0;
	}
	
	public void addActionIf(Action act)
	{
		v_actions_if.add(act);
	}
	
	public void addActionElse(Action act)
	{
		v_actions_else.add(act);
	}
	
	void setCondition(Condition con)
	{
		this.con = con;
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
