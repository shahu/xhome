-----------------------------------------------------------------------------
-- Shared dict variable
-----------------------------------------------------------------------------

local lua_conf_dict = ngx.shared.lua_conf_dict

local pptransfer_program = lua_conf_dict:get("pptransfer_program")
local ramdisk_path = lua_conf_dict:get("ramdisk_path")

-----------------------------------------------------------------------------
-- Url Parse
-----------------------------------------------------------------------------

local uri_flv = ngx.re.sub(ngx.var.uri, ".ts", ".flv")
local uri_streamid = ngx.re.sub(ngx.var.uri, "/live/(.*)/(.*).ts", "$1")
local uri_timestamp = ngx.re.sub(ngx.var.uri, "/live/(.*)/(.*).ts", "$2")

local http_flv = ngx.location.capture(uri_flv)

if (http_flv.status == ngx.HTTP_OK) then
	local file_path = string.format("%s/%s", ramdisk_path, uri_streamid)
	os.execute("mkdir -p "..file_path)

	local file_flv = string.format("%s/%s.flv", file_path, uri_timestamp)
	local file_ts = string.format("%s/%s.ts", file_path, uri_timestamp)

	local fp_flv = io.open(file_flv, "wb")
	if (fp_flv) then
		fp_flv:write(http_flv.body)
		fp_flv:close()

	end
	
	local flv2ts_cmd = string.format("%s --TransferModule.input_file=ppfile-flv://%s --TransferModule.output_format=ts --TransferModule.output_file=ppfile://%s", pptransfer_program, file_flv, file_ts)

	local err = os.execute(flv2ts_cmd)
	if (err > 0) then
		ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)

	else 	
		local fp_ts = io.open(file_ts, "rb")
		if (fp_ts) then
			data_ts = fp_ts:read("*a")
			fp_ts:close()

			ngx.header.content_type = "application/octet-stream"
			ngx.header.content_length = string.len(data_ts)

			ngx.print(data_ts)
		end
	end
else
	ngx.exit(http_flv.status)
end
