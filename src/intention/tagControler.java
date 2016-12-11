
package intention;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.google.common.collect.Maps;

// TODO: Auto-generated Javadoc

public class tagControler {

	/** The logger. */
	public static Logger logger = Logger.getLogger(tagControler.class);

	/** The m map tag name ID. */
	private static Map<String, Integer> m_map_TagNameID = Maps.newHashMap();

	/** The m map tag ID name. */
	private static Map<Integer, String> m_map_TagIDName = Maps.newHashMap();

	/**
	 * Inits the.
	 *
	 * @param path the path
	 * @param fileName the file name
	 * @return true, if successful
	 */
	public  boolean init(String path, String fileName)
	{
		try
		{
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader(new File(path + fileName)));
			String line = "";
			String []arr;
			String name = "";
			int id = 0;
			int line_num = 0;
			while ((line = reader.readLine()) != null)
			{
				line_num++;
				if (0 == line.length()) continue;
				if ('#' == line.charAt(0)) continue;
				arr = line.split("\t");
				if (2 != arr.length)
				{
					logger.info(String.format("ERROR: parse tag file fail! line:%s", line_num));
					return false;
				}
				name = arr[0];
				id = Integer.parseInt(arr[1]);
				if(m_map_TagNameID.containsKey(name))
				{
					logger.info("ERROR: Tag Name Redefined!");
					return false;
				}
				if(m_map_TagIDName.containsKey(id))
				{
					logger.info("ERROR: Tag ID Redefined!");
					return false;
				}
				m_map_TagNameID.put(name, id);
				m_map_TagIDName.put(id, name);
				
			}
			reader.close();
		}
		catch (Exception e)
		{
			logger.info("load tag file fail.");
			return false;
		}
		return true;
	}

	/**
	 * Gets the tag id.
	 *
	 * @param name
	 *            the name
	 * @return the tag id
	 */
	public int getTagId(String name) {
		if (m_map_TagNameID.containsKey(name)) {
			return m_map_TagNameID.get(name);
		}
		return -1;
	}

	public static String getTagName(int tagId) {
		if (m_map_TagIDName.containsKey(tagId)) {
			return m_map_TagIDName.get(tagId);
		}
		return null;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str = "abc|";
		String[] arr = str.split("\\|", -1);

		System.out.println(arr.length);
		System.out.println(arr[1].equals(""));

	}

}
