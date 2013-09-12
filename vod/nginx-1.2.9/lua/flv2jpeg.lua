-----------------------------------------------------------------------------
-- Shared dict variable
-----------------------------------------------------------------------------

local lua_conf_dict = ngx.shared.lua_conf_dict

-----------------------------------------------------------------------------
-- Url Parse
-----------------------------------------------------------------------------

local uri_image_width = ngx.re.sub(ngx.var.uri, "/image/(.*)/(.*)/(.*)/(.*).jpeg", "$1")
local uri_image_height = ngx.re.sub(ngx.var.uri, "/image/(.*)/(.*)/(.*)/(.*).jpeg", "$2")
local uri_image_streamid = ngx.re.sub(ngx.var.uri, "/image/(.*)/(.*)/(.*)/(.*).jpeg", "$3")
local uri_image_timestamp = ngx.re.sub(ngx.var.uri, "/image/(.*)/(.*)/(.*)/(.*).jpeg", "$4")

local flv_timestamp = math.floor(uri_image_timestamp/5) * 5
local flv_timeoffset = uri_image_timestamp % 5

local uri_flv = string.format("http://127.0.0.1/live/%s/%s.flv", uri_image_streamid, flv_timestamp)

local ffmpeg_cmd = string.format("/usr/local/ffmpeg-rtmp/bin/ffmpeg -i %s -y -f image2 -ss %s -s %s*%s -t 0.001 -", uri_flv, flv_timeoffset, uri_image_width, uri_image_height)

local pipe = io.popen(ffmpeg_cmd, 'r')
local image_jpeg = pipe:read("*a")
local image_length = string.len(image_jpeg)

if (image_length > 0) then
	ngx.header.content_type = "image/jpeg"
	ngx.header.content_length = image_length

	ngx.print(image_jpeg)
else
	ngx.exit(ngx.HTTP_NOT_FOUND)
end
