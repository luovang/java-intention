

package intention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.collect.Maps;

// TODO: Auto-generated Javadoc
class Variable {
	String name;
	int tagId;
	int weight;
	Variable(String name, int tagId, int weight)
	{
		this.name = name;
		this.tagId = tagId;
		this.weight = weight;
	}
}

/**
 * The Class variableControler.
 */
public class variableControler {
	
	/** The logger. */
	public static Logger logger = Logger.getLogger(variableControler.class);
	
	/** The m map vars. */
	public static  Map<String, Variable> m_map_vars = Maps.newHashMap();
	
	/** The variable begin string. */
	public final static String VARIABLE_BEGIN_STRING = "$[";
	
	/** The variable end string. */
	public final static String VARIABLE_END_STRING = "]";
	
	/**
	 * Inits the.
	 *
	 * @param path the path
	 * @param fileName the file name
	 * @param _tag_Ctrl the tag ctrl
	 * @return true, if successful
	 */
	public boolean init(String path, String fileName, tagControler _tag_Ctrl)
	{
		try
		{
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader(new File(path + fileName)));
			String line = "";
			String []items;
			String name = "";
			int id = 0;
			int weight = 0;
			int line_num = 0;
			while ((line = reader.readLine()) != null)
			{
				line_num++;
				if (0 == line.length()) continue;
				if ('#' == line.charAt(0)) continue;
				items = line.split("\t");
				if (3 != items.length)
				{
					logger.error(String.format("ERROR: parse variable file fail! line:%d", line_num));
					return false;
				}
				if (!items[0].startsWith(VARIABLE_BEGIN_STRING) || !items[0].endsWith(VARIABLE_END_STRING))
				{
					logger.error(String.format("bad format:%s", line));
					return false;
				}
				name = items[0];
				id = _tag_Ctrl.getTagId(items[1]);
				if (id < 0)
				{
					logger.error(String.format("no tag defined :%s", items[1]));
					return false;
				}
				weight = Integer.parseInt(items[2]);
				Variable curvar = new Variable(name, id, weight);
				if (m_map_vars.containsKey(name))
				{
					logger.error("ERROR: Variable Redefined!");
					return false;
				}
				m_map_vars.put(name, curvar);
			}
			reader.close();
		}
		catch (Exception e)
		{
			logger.error("load variable file fail.");
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the variable.
	 *
	 * @param name the name
	 * @return the variable
	 */
	public Variable getVariable(String name)
	{
		Variable ret = null;
		ret = m_map_vars.get(name);
		return ret;
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
