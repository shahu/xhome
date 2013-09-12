-----------------------------------------------------------------------------
-- Shared dict variable
-----------------------------------------------------------------------------

local lua_conf_dict = ngx.shared.lua_conf_dict

local rtmp_bin = "/usr/local/bin/rtmpdump"
local rtmp_url = "rtmp://218.78.215.214/live/shanghaiATRSingleChannel"
local rtmp_cmd = string.format("%s -v -r %s", rtmp_bin, rtmp_url)
local pipe = io.popen(rtmp_cmd, 'r')

function abort_cleanup()
	pipe:close()
end

local ok, err = ngx.on_abort(abort_cleanup)
if not ok then
	ngx.log(ngx.ERR, "failed to register the on_abort callback: ", err)
	ngx.exit(ngx.HTTP_SERVICE_UNAVAILABLE)
end

local flv_header = pipe:read(3)
if "FLV" == flv_header then
	#ngx.print(flv_header)
	ngx.flush(true)

	--while true do
		flv_data = pipe:read(65536)
		--ngx.print(flv_data)
		ngx.flush(true)
		flv_data = pipe:read(65536)
		--ngx.print(flv_data)
		ngx.flush(true)
	--end
	
	pipe:close()
else
	ngx.exit(ngx.HTTP_SERVICE_UNAVAILABLE)
end
