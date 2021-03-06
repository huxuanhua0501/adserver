package com.racetime.xsad.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.racetime.xsad.dao.StockDaoMapper;
import com.racetime.xsad.model.PmpResource;
import com.racetime.xsad.model.ResponseJson;
import com.racetime.xsad.service.StockService;

/**
 *库存管理业务类
 */
@Service
public class StockServiceImpl implements StockService{
	@Autowired
	private StockDaoMapper stockDaoMapper;
	
	@Override
	public String getStockInfoByAdx(Map<String, Object> param) {
		List<Map<String,Object>> json = new ArrayList<>();
		List<Map<String,Object>> deviceInfo = stockDaoMapper.getPmpResource(param);
		if(deviceInfo != null && deviceInfo.size() >0){
			Map<String,Object> stockParam = null;
			for (int i = 0; i < deviceInfo.size(); i++) {
				Map<String,Object> value = new HashMap<>();
				value.put("ssp_app_id", deviceInfo.get(i).get("ssp_app_id"));//下游appid
				value.put("ssp_adslot_id", deviceInfo.get(i).get("ssp_adslot_id"));//下游广告位ID
				value.put("scene_id", deviceInfo.get(i).get("scene_id"));
				value.put("pv", deviceInfo.get(i).get("pv"));
				value.put("uv", deviceInfo.get(i).get("uv"));
				value.put("device_num", deviceInfo.get(i).get("device_num"));
				value.put("id", deviceInfo.get(i).get("id"));
				value.put("cpm", deviceInfo.get(i).get("cpm")); //下游CPM
				value.put("price", deviceInfo.get(i).get("price")); //下游价格
				value.put("adx_adslot_id",deviceInfo.get(i).get("adx_adslot_id")); //上游广告位ID
				try {
					value.put("name", URLEncoder.encode(deviceInfo.get(i).get("name").toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				value.put("city_code", deviceInfo.get(i).get("city_code"));
				//获取该售卖单元在订单中的时间段
				List<Map<String,Object>> orderInfo = stockDaoMapper.getOrderInfo(deviceInfo.get(i).get("id").toString());
				//获取该售卖单元下的库存和投放时间
				stockParam = new HashMap<>();
				stockParam.put("pmp_resource_id", deviceInfo.get(i).get("id").toString());
				if(param.get("sdate") !=null)
				stockParam.put("sdate", param.get("sdate").toString());
				if((param.get("edate") !=null))
				stockParam.put("edate", param.get("edate").toString());
				List<Map<String,Object>> stockInfo = stockDaoMapper.getPmpResouceStock(stockParam);
				Map<String,Object> mdate = new TreeMap<String,Object>();
				//组装mdate
				for (int j = 0; j < stockInfo.size(); j++) {
					PmpResource source = new PmpResource();
					source.setId(Integer.parseInt(stockInfo.get(j).get("id").toString()));
					source.setTotal(Integer.parseInt(stockInfo.get(j).get("stock").toString()));
					if(orderInfo.size()>0){
						for (int k = 0; k < orderInfo.size(); k++) {
							if(stockInfo.get(j).get("mdate").toString().replaceAll("-","").equals(orderInfo.get(k).get("put_time").toString().replaceAll("-", ""))){
								source.setUsed(Integer.parseInt(orderInfo.get(k).get("stock").toString()));
							}
						}
					}else{
						source.setUsed(0);
					}
					
					mdate.put(stockInfo.get(j).get("mdate").toString(),source);
				}
				value.put("mdate", mdate);
				json.add(value);
			}
		}else{
			return "0";
		}
		System.out.println(new Gson().toJson(json));
		return new Gson().toJson(json);
	}

	@Override
	public String getAppStockInfo(Map<String,Object> param) {
		List<Map<String,Object>> json = new ArrayList<>();
		List<Map<String,Object>> app_info = stockDaoMapper.getAllAppStock(param);
		if(app_info.size()>0){
			for (int i = 0; i < app_info.size(); i++) {
				//Map<String,Object> app_key = new HashMap<>();
				Map<String,Object> appInfo = new HashMap<>();
				appInfo.put("total", app_info.get(i).get("total"));
				appInfo.put("app_id",app_info.get(i).get("app_id"));
				Map<String,Object> orderInfoParam = new HashMap<>();
				orderInfoParam.put("ad_app_id", app_info.get(i).get("app_id").toString());
				orderInfoParam.put("sdate", param.get("sdate").toString());
				orderInfoParam.put("edate", param.get("edate").toString());
				int remain = 0;
				orderInfoParam.put("type", "1");
				String orderAppYDStock = stockDaoMapper.getOderAppStock(orderInfoParam);
				orderInfoParam.put("type", "2");
				String orderAppSDStock = stockDaoMapper.getOderAppStock(orderInfoParam);
				if(orderAppSDStock != null){
					appInfo.put("lock", orderAppSDStock);
					remain += Integer.parseInt(orderAppSDStock);
				}else{
					appInfo.put("lock", 0);
				}
				if(orderAppYDStock != null){
					appInfo.put("reserve", orderAppYDStock);
					remain += Integer.parseInt(orderAppYDStock);
				}else{
					appInfo.put("reserve", 0);
				}
				if(remain>0){
					appInfo.put("remain",Integer.parseInt(app_info.get(i).get("total").toString())-remain);
				}else{
					appInfo.put("remain", app_info.get(i).get("total"));
				}
				
				/*Map<String,Object> orderAppStock = stockDaoMapper.getOderAppStock(orderInfoParam);
				if(orderAppStock != null){
					if(orderAppStock.get("sdnum") == null){
						appInfo.put("lock", 0);
					}else{
						appInfo.put("lock", orderAppStock.get("sdnum"));
						remain += Integer.parseInt(orderAppStock.get("sdnum").toString());
					}
					if(orderAppStock.get("ydnum") == null){
						appInfo.put("reserve", 0);
					}else{
						appInfo.put("reserve", orderAppStock.get("ydnum"));
						remain += Integer.parseInt(orderAppStock.get("ydnum").toString());
					}
					appInfo.put("remain",Integer.parseInt(app_info.get(i).get("total").toString())-remain);
				}else{
					appInfo.put("lock", 0);
					appInfo.put("reserve", 0);
					appInfo.put("remain", app_info.get(i).get("total"));
				}*/
				
				
				
				
				
				
				json.add(appInfo);
			}
			
		}else{
			return "0";
		}
		//System.out.println(new Gson().toJson(json));
		return new Gson().toJson(json);
	}

	@Override
	@Transactional
	public ResponseJson updatePmpResouceStock(String ids, String stock) {
		ResponseJson json = new ResponseJson();
		int k = 0;
		List<String> pmpResourceIds = Arrays.asList(ids.split(","));
		Map<String,Object> param = new HashMap<>();
		param.put("list",pmpResourceIds);
		param.put("stock", stock);
		List<Integer> listIds = validatePmpResouceStock(param);
		//string.Join(",", list.ToArray());
		//验证是否有超过设备
		if(listIds.size()>0){
			json.setCode(300);
			String str = "";
			for (int i = 0; i < listIds.size(); i++) {
				str += listIds.get(i)+",";
			}
			json.setData(str.substring(0,str.length()-1));
		}else{
			k = stockDaoMapper.updatePmpResouceStock(param);
			if(k >0){
				json.setCode(200);
			}else{
				json.setCode(500);
			}
			json.setData("");
		}
		
		return json;
	}
	/**
	 * 
	 * @param stock //前端库存
	 * @param param //
	 * @return
	 */
	//验证是该资源组是否满足不超过库存使用量返回资源ID
	public List<Integer> validatePmpResouceStock(Map<String,Object> param){
		int stock = Integer.parseInt(param.get("stock").toString());
		List<Integer> ids  = new ArrayList<>();
		List<Map<String,Object>> resouceDateInfo = stockDaoMapper.getPmpResouceDatas(param);
		for (int i = 0; i < resouceDateInfo.size(); i++) {
			//获取该资源组ID和日期
			List<Map<String,Object>> orderInfo = stockDaoMapper.getOrderInfo(resouceDateInfo.get(i).get("pmp_resource_id").toString());
				for (int j = 0; j < orderInfo.size(); j++) {
					if(resouceDateInfo.get(i).get("mdate").toString().equals(orderInfo.get(j).get("put_time").toString())){
						if(stock < Integer.parseInt(orderInfo.get(j).get("stock").toString())){
							ids.add(Integer.parseInt(resouceDateInfo.get(i).get("pmp_resource_stock_id").toString()));
						}
					}
					
				}
		}
		return ids;
	}

	@Override
	public String getStockInfoSsp(Map<String, Object> param) {
		List<Map<String,Object>> json = new ArrayList<>();
		List<Map<String,Object>> deviceInfo = stockDaoMapper.getPmpResourceBySSP(param);
		if(deviceInfo != null && deviceInfo.size() >0){
			Map<String,Object> stockParam = null;
			for (int i = 0; i < deviceInfo.size(); i++) {
				Map<String,Object> value = new HashMap<>();
				value.put("ssp_app_id", deviceInfo.get(i).get("ssp_app_id"));//下游appid
				value.put("ssp_adslot_id", deviceInfo.get(i).get("ssp_adslot_id"));//下游广告位ID
				value.put("scene_id", deviceInfo.get(i).get("scene_id"));
				value.put("pv", deviceInfo.get(i).get("pv"));
				value.put("uv", deviceInfo.get(i).get("uv"));
				value.put("device_num", deviceInfo.get(i).get("device_num"));
				value.put("id", deviceInfo.get(i).get("id"));
				value.put("cpm", deviceInfo.get(i).get("cpm")); //下游CPM
				value.put("price", deviceInfo.get(i).get("price")); //下游价格
				try {
					value.put("name", URLEncoder.encode(deviceInfo.get(i).get("name").toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				value.put("city_code", deviceInfo.get(i).get("city_code"));
				//获取该售卖单元在订单中的时间段
				List<Map<String,Object>> orderInfo = stockDaoMapper.getOrderInfo(deviceInfo.get(i).get("id").toString());
				//获取该售卖单元下的库存和投放时间
				stockParam = new HashMap<>();
				stockParam.put("pmp_resource_id", deviceInfo.get(i).get("id").toString());
				if(param.get("sdate") !=null)
				stockParam.put("sdate", param.get("sdate").toString());
				if((param.get("edate") !=null))
				stockParam.put("edate", param.get("edate").toString());
				List<Map<String,Object>> stockInfo = stockDaoMapper.getPmpResouceStock(stockParam);
				Map<String,Object> mdate = new TreeMap<String,Object>();
				//组装mdate
				for (int j = 0; j < stockInfo.size(); j++) {
					PmpResource source = new PmpResource();
					source.setId(Integer.parseInt(stockInfo.get(j).get("id").toString()));
					source.setTotal(Integer.parseInt(stockInfo.get(j).get("stock").toString()));
					if(orderInfo.size()>0){
						for (int k = 0; k < orderInfo.size(); k++) {
							if(stockInfo.get(j).get("mdate").toString().replaceAll("-","").equals(orderInfo.get(k).get("put_time").toString().replaceAll("-", ""))){
								source.setUsed(Integer.parseInt(orderInfo.get(k).get("stock").toString()));
							}
						}
					}else{
						source.setUsed(0);
					}
					
					mdate.put(stockInfo.get(j).get("mdate").toString(),source);
				}
				value.put("mdate", mdate);
				json.add(value);
			}
		}else{
			return "0";
		}
		System.out.println(new Gson().toJson(json));
		return new Gson().toJson(json);
	}
	
	
	

}
