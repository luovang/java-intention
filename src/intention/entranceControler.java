package intention;

import java.util.Map;
import java.util.Vector;

import com.google.common.collect.Maps;

class EntranceResult {
	public Map<Integer, Vector<PatternResult>> map_hit_ptn;
	
}


public class entranceControler {

	public int matchEntrances(Vector<EntranceResult> ret_vec_res, ExpParams params)
	{
		int i = 0;
		int pid = 0;
		Map<Integer, Integer> map_hit_cnt = Maps.newHashMap();
		if (params.p_hit_ptns == null)
		{
			return -1;
		}
		for (PatternResult ptr : params.p_hit_ptns)
		{
			pid = ptr._pat.m_gid;
			if (!map_hit_cnt.containsKey(pid))
			{
				map_hit_cnt.put(pid, 1);
			}
			else
			{
				int cnt = map_hit_cnt.get(pid) + 1;
				map_hit_cnt.put(pid, cnt);
			}
		}
		params.p_map_gid_cnt = map_hit_cnt;
		return 0;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
