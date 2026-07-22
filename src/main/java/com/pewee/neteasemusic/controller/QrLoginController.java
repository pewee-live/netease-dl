package com.pewee.neteasemusic.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.models.common.RespEntity;
import com.pewee.neteasemusic.service.NeteaseAPIService;
import com.pewee.neteasemusic.utils.QrUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 提供扫码登录功能和SPA页面路由
 * @author pewee
 *
 */
@Controller
@Slf4j
public class QrLoginController {
	
	@Autowired
    private NeteaseAPIService neteaseAPIService;
	
	@GetMapping("/")
    public String index() {
        return "app"; 
    }
	
	@GetMapping("/home")
    public String home() {
		return "app";
    }
	
	/**
     * API: 获取二维码key和Base64图片（供SPA调用）
     */
    @ResponseBody
    @GetMapping("/api/qr/key")
    public RespEntity<?> getQrKey() {
    	try {
            String reqp = neteaseAPIService.getLoginQrKey();
			JSONObject jsonObject = JSON.parseObject(reqp);
			String unikey = jsonObject.getString("unikey");
			
            String qrUrl = "https://music.163.com/login?codekey=" + unikey;
            String qrImageBase64 = QrUtils.generateQrBase64(qrUrl);
            
            Map<String, String> data = new HashMap<>();
            data.put("unikey", unikey);
            data.put("qrUrl", qrUrl);
            data.put("qrImage", qrImageBase64);
            
            return RespEntity.apply(CommonRespInfo.SUCCESS, data);
    	} catch (Exception e) {
            log.error("生成二维码失败", e);
            return RespEntity.apply(CommonRespInfo.SYS_ERROR, "生成二维码失败: " + e.getMessage());
        }
    }

    /**
     * 轮询查询二维码扫码状态
     */
    @ResponseBody
    @GetMapping("/qr/status")
    public RespEntity<Boolean> checkQrStatus(@RequestParam("unikey") String unikey) throws Exception {
    	if (!neteaseAPIService.checkReady()) {
    		neteaseAPIService.checkLoginQrStatus(unikey);
    	}
        return RespEntity.apply(CommonRespInfo.SUCCESS, neteaseAPIService.checkReady());
    }
    
    /**
     * 查询当前登录状态
     */
    @ResponseBody
    @GetMapping("/login/status")
    public RespEntity<Boolean> checkLoginStatus() throws Exception {
        return RespEntity.apply(CommonRespInfo.SUCCESS, neteaseAPIService.checkReady());
    }
}
