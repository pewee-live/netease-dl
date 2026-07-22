---
name: netease-dl
description: Interact with a local Netease Cloud Music downloader service via REST API to search, download albums, playlists, and singles, manage download queue, and configure settings. Use when the user wants to download music from Netease Cloud Music, search for songs/albums/playlists, check download progress, or manage the downloader service.
---

# Netease Music Downloader

Interact with the local Netease Cloud Music downloader service running at `http://ip.to.your.musictag.server:8080`.

## Response Format

All APIs return:
```json
{ "code": "000000", "msg": "success", "data": <varies> }
```
- `code` = `"000000"` → success; code starting with `-` → error

## Authentication

### Check Login
```
GET /login/status
```
Returns `boolean`. If `false`, must login first.

### QR Code Login
1. `GET /api/qr/key` → returns `{ unikey, qrUrl, qrImage(base64) }`
2. Show `qrUrl` or `qrImage` to user for scanning
3. Poll `GET /qr/status?unikey={unikey}` until returns `true`

### Cookie Login
```
GET /setCookie?cookie={cookie_string}
```

### Logout
```
POST /v2/logout
```

## Search

```
GET /Search?keywords={kw}&limit={limit}&offset={offset}&type={type}
```
| type | Content |
|------|---------|
| 1    | Song    |
| 10   | Album   |
| 100  | Artist  |
| 1000 | Playlist |

## Analyze (Preview Details)

```
GET /Album?id={album_id}
GET /Playlist?id={playlist_id}
```

## Download

```
GET /v2/single?id={song_id}
GET /v2/album?id={album_id}
GET /v2/playlist?id={playlist_id}
```
Downloads are queued asynchronously. Always poll queue to confirm completion.

## Queue Management

```
GET  /v2/queue?limit={limit}      # Recent tasks (default limit=50)
GET  /v2/queue/count              # { waiting, total }
POST /v2/queue/clear              # Clear finished/failed tasks
POST /v2/queue/clearAll           # Clear all tasks
```

## Configuration

```
GET  /v2/quality                              # Get quality level
POST /v2/quality?level={level}                # standard|higher|exhigh|lossless|hires
GET  /v2/path                                 # Get download path
GET  /v2/getRepeat                            # Get repeat flag
GET  /v2/setRepeat?repeat={true|false}        # Set repeat flag
GET  /v2/playlistAsAlbum                      # Get playlist-as-album mode
POST /v2/playlistAsAlbum?enabled={true|false}
GET  /MyPlaylist?limit=10&offset=0            # User's playlists
```

## Typical Workflows

### Download Album by Name
1. `GET /login/status` → verify logged in
2. `GET /Search?keywords={name}&type=10` → extract album ID
3. `GET /v2/album?id={id}` → start download
4. Poll `GET /v2/queue/count` until `waiting=0`

### Download Playlist by ID
1. `GET /login/status` → verify logged in
2. `GET /v2/playlist?id={id}` → start download
3. Poll `GET /v2/queue/count` until `waiting=0`

### Error Recovery
- Auth error → `POST /v2/logout` → re-login via QR code
- Connection refused → service not running, ask user to start it