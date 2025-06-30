package com.pewee.neteasemusic.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.models.common.RespEntity;
import com.pewee.neteasemusic.models.dtos.AlbumAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.PlaylistAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.SingleMusicAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.TrackDTO;
import com.pewee.neteasemusic.service.AnalysisService;
import com.pewee.neteasemusic.service.NeteaseAPIService;

/**
 * 整体api接口和https://github.com/Suxiaoqinx/Netease_url相同
 * @author pewee
 *
 */
@RestController
public class AnalysisController {
	
	@Autowired
    private AnalysisService analysisService;
	
	@RequestMapping(value = "/setCookie", method = {RequestMethod.GET, RequestMethod.POST})
	public RespEntity<?> refreshCookie(@RequestParam(value = "cookie",required = true) String cookie) {
		analysisService.refreshCookie(cookie);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}

	
	
	@RequestMapping(value = "/Album", method = {RequestMethod.GET, RequestMethod.POST})
    public RespEntity<?> album(@RequestParam(required = true) Long id) {
		AlbumAnalysisRespDTO  result = analysisService.analyzeAlbum(id);
        return RespEntity.apply(CommonRespInfo.SUCCESS,result);
    }

    @RequestMapping(value = "/Playlist", method = {RequestMethod.GET, RequestMethod.POST})
    public RespEntity<?> playlist(@RequestParam(required = true) Long id) {
    	PlaylistAnalysisRespDTO  result = analysisService.analyzePlaylist(id);
            return RespEntity.apply(CommonRespInfo.SUCCESS,result);
    }
    
    
    /**
     * 搜索 
     * @param keyword 关键词
     * @param limit 每页条数
     * @param offset 偏移量
     * @param type  搜索类型
     * 	单曲	1
		歌手	100
		专辑	10
		歌单	1000
		用户	1002
		MV	1004
		歌词	1006
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/Search", method = {RequestMethod.GET, RequestMethod.POST})
    public RespEntity<?> search(@RequestParam(required = true) String keywords,
                                    @RequestParam(required = false, defaultValue = "50") int limit,
                                    @RequestParam(required = false, defaultValue = "0") int offset,@RequestParam(required = false) Integer type) {
    	List<?> result = analysisService.searchMusic(keywords, limit,offset,type);
            return RespEntity.apply(CommonRespInfo.SUCCESS,result);
       
    }

    @RequestMapping(value = "/Song_V1", method = {RequestMethod.GET, RequestMethod.POST})
    public RespEntity<?> songV1(@RequestParam(required = true) Long id,
                                    @RequestParam(required = true) String level,
                                    @RequestParam(required = false, defaultValue = "json") String type) {
    	SingleMusicAnalysisRespDTO songInfo = analysisService.analyzeSingleSong(id, level);
        return RespEntity.apply(CommonRespInfo.SUCCESS,songInfo);
    }
    
    
    @RequestMapping(value = "/MyPlaylist", method = {RequestMethod.GET, RequestMethod.POST})
    public RespEntity<?> getUserPlaylists(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return RespEntity.apply(CommonRespInfo.SUCCESS,analysisService.getUserPlaylists(limit, offset));
    }
}
