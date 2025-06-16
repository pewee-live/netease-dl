package com.pewee.neteasemusic.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.exceptions.ServiceException;
import com.pewee.neteasemusic.models.dtos.AlbumAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.AlbumInfoDTO;
import com.pewee.neteasemusic.models.dtos.PlaylistAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.PlaylistInfoDTO;
import com.pewee.neteasemusic.models.dtos.SingleMusicAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.TrackDTO;
import com.pewee.neteasemusic.models.dtos.UserPlaylistListRespDTO;
import com.pewee.neteasemusic.models.dtos.UserPlaylistSummaryDTO;

@Service
public class AnalysisService {
	
	@Autowired
    private NeteaseAPIService neteaseAPIService;
	
	public void refreshCookie(String c) {
		neteaseAPIService.refreshCookie(c);
	}

	public SingleMusicAnalysisRespDTO analyzeSingleSong(Long id, String level) {
		checkReady();
        try {
            String urlJsonStr = neteaseAPIService.urlV1(id, level);
            String nameJsonStr = neteaseAPIService.songDetail( Lists.newArrayList(id) );
            String lyricJsonStr = neteaseAPIService.getLyric(id);

            JSONObject urlJson = JSON.parseObject(urlJsonStr);
            JSONObject nameJson = JSON.parseObject(nameJsonStr);
            JSONObject lyricJson = JSON.parseObject(lyricJsonStr);

            JSONArray dataArray = urlJson.getJSONArray("data");
            if (dataArray.isEmpty() || dataArray.getJSONObject(0).get("url") == null) {
                return null;
            }

            JSONObject songUrlData = dataArray.getJSONObject(0);
            JSONObject songInfo = nameJson.getJSONArray("songs").getJSONObject(0);

            SingleMusicAnalysisRespDTO dto = new SingleMusicAnalysisRespDTO();
            dto.setName(songInfo.getString("name"));
            dto.setPic(songInfo.getJSONObject("al").getString("picUrl"));
            dto.setAl_name(songInfo.getJSONObject("al").getString("name"));
            dto.setAr_name(
                songInfo.getJSONArray("ar").stream()
                    .map(ar -> ((JSONObject) ar).getString("name"))
                    .collect(Collectors.joining("/"))
            );
            dto.setLyric(lyricJson.getJSONObject("lrc") != null ? lyricJson.getJSONObject("lrc").getString("lyric") : "");
            dto.setTlyric(lyricJson.containsKey("tlyric") ? lyricJson.getJSONObject("tlyric").getString("lyric") : null);
            dto.setUrl(songUrlData.getString("url").replace("http://", "https://"));
            dto.setSize(formatSize(songUrlData.getLongValue("size")));
            dto.setId(id);
            dto.setStatus(200);
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    public List<TrackDTO> searchMusic(String keywords, Integer limit,int offset) {
    	checkReady();
        try {
            String respStr = neteaseAPIService.searchMusic(keywords, limit,offset,1);
            JSONObject resp = JSON.parseObject(respStr);
            JSONArray songs = resp.getJSONObject("result").getJSONArray("songs");

            List<TrackDTO> trackList = new ArrayList<>();
            for (int i = 0; i < songs.size(); i++) {
                JSONObject song = songs.getJSONObject(i);
                TrackDTO dto = new TrackDTO();
                dto.setId(song.getLong("id"));
                dto.setName(song.getString("name"));
                dto.setPicUrl(song.getJSONObject("al").getString("picUrl"));
                dto.setAlbum(song.getJSONObject("al").getString("name"));
                dto.setArtists(song.getJSONArray("ar").stream()
                    .map(ar -> ((JSONObject) ar).getString("name"))
                    .collect(Collectors.joining("/")));
                trackList.add(dto);
            }
            return trackList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public PlaylistAnalysisRespDTO analyzePlaylist(Long playlistId) {
    	checkReady();
        PlaylistAnalysisRespDTO result = new PlaylistAnalysisRespDTO();
        try {
            String jsonStr = neteaseAPIService.getPlaylistDetail(playlistId);
            JSONObject json = JSON.parseObject(jsonStr);
            JSONObject playlist = json.getJSONObject("playlist");

            PlaylistInfoDTO playlistInfo = new PlaylistInfoDTO();
            playlistInfo.setId(playlist.getLong("id"));
            playlistInfo.setName(playlist.getString("name"));
            playlistInfo.setCoverImgUrl(playlist.getString("coverImgUrl"));
            playlistInfo.setDescription(playlist.getString("description"));
            playlistInfo.setCreator(playlist.getJSONObject("creator").getString("nickname"));
            playlistInfo.setTrackCount(playlist.getIntValue("trackCount"));

            JSONArray tracks = playlist.getJSONArray("tracks");
            List<TrackDTO> trackList = new ArrayList<>();
            for (int i = 0; i < tracks.size(); i++) {
                JSONObject track = tracks.getJSONObject(i);
                TrackDTO dto = new TrackDTO();
                dto.setId(track.getLong("id"));
                dto.setName(track.getString("name"));
                dto.setPicUrl(track.getJSONObject("al").getString("picUrl"));
                dto.setAlbum(track.getJSONObject("al").getString("name"));
                dto.setArtists(track.getJSONArray("ar").stream()
                    .map(ar -> ((JSONObject) ar).getString("name"))
                    .collect(Collectors.joining("/")));
                trackList.add(dto);
            }
            playlistInfo.setTracks(trackList);
            result.setPlaylist(playlistInfo);
            result.setStatus(200);
        } catch (Exception e) {
            result.setStatus(500);
        }
        return result;
    }

    public AlbumAnalysisRespDTO analyzeAlbum(Long albumId) {
    	checkReady();
        AlbumAnalysisRespDTO result = new AlbumAnalysisRespDTO();
        try {
            String jsonStr = neteaseAPIService.getAlbumDetail(albumId);
            JSONObject json = JSON.parseObject(jsonStr);
            JSONObject album = json.getJSONObject("album");

            AlbumInfoDTO albumInfo = new AlbumInfoDTO();
            albumInfo.setId(album.getLong("id"));
            albumInfo.setName(album.getString("name"));
            albumInfo.setCoverImgUrl(album.getString("picUrl"));
            albumInfo.setPublishTime(album.getLong("publishTime"));
            albumInfo.setArtist(album.getJSONObject("artist").getString("name"));

            JSONArray songs = json.getJSONArray("songs");
            List<TrackDTO> trackList = new ArrayList<>();
            for (int i = 0; i < songs.size(); i++) {
                JSONObject track = songs.getJSONObject(i);
                TrackDTO dto = new TrackDTO();
                dto.setId(track.getLong("id"));
                dto.setName(track.getString("name"));
                dto.setPicUrl(track.getJSONObject("al").getString("picUrl"));
                dto.setAlbum(track.getJSONObject("al").getString("name"));
                dto.setArtists(track.getJSONArray("ar").stream()
                    .map(ar -> ((JSONObject) ar).getString("name"))
                    .collect(Collectors.joining("/")));
                trackList.add(dto);
            }
            albumInfo.setSongs(trackList);
            result.setAlbum(albumInfo);
            result.setStatus(200);
        } catch (Exception e) {
            result.setStatus(500);
        }
        return result;
    }
    
    public UserPlaylistListRespDTO getUserPlaylists( int limit, int offset) {
    	checkReady();
        UserPlaylistListRespDTO resp = new UserPlaylistListRespDTO();
        List<UserPlaylistSummaryDTO> allPlaylists = new ArrayList<>();
        boolean hasMore = true;
        int currentOffset = offset;
        Long userUid = neteaseAPIService.getUserUid();
        try {
            while (hasMore) {
                String jsonStr = neteaseAPIService.getUserPlaylist(userUid, limit, currentOffset);
                JSONObject root = JSON.parseObject(jsonStr);

                JSONArray playlistArray = root.getJSONArray("playlist");
                hasMore = root.getBooleanValue("more");

                if (playlistArray != null) {
                    for (int i = 0; i < playlistArray.size(); i++) {
                        JSONObject pl = playlistArray.getJSONObject(i);
                        UserPlaylistSummaryDTO dto = new UserPlaylistSummaryDTO();
                        dto.setId(pl.getLong("id"));
                        dto.setName(pl.getString("name"));
                        dto.setCoverImgUrl(pl.getString("coverImgUrl"));

                        JSONObject creator = pl.getJSONObject("creator");
                        dto.setCreator(creator != null ? creator.getString("nickname") : null);

                        dto.setTrackCount(pl.getIntValue("trackCount"));
                        dto.setDescription(pl.getString("description"));
                        allPlaylists.add(dto);
                    }
                }

                currentOffset += limit; // 下一页偏移
            }

            resp.setPlaylists(allPlaylists);
            resp.setTotal(allPlaylists.size());
            resp.setStatus(200);
        } catch (Exception e) {
            resp.setPlaylists(Collections.emptyList());
            resp.setTotal(0);
            resp.setStatus(500);
        }
        return resp;
    }

    private String formatSize(long value) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = value;
        for (String unit : units) {
            if (size < 1024) {
                return String.format("%.2f%s", size, unit);
            }
            size /= 1024.0;
        }
        return String.format("%.2fPB", size);
    }
    
    private void checkReady() {
    	if (!neteaseAPIService.checkReady()) {
    		throw new ServiceException(CommonRespInfo.NO_COOKIE_ERROR);
    	}
    }
	
}
