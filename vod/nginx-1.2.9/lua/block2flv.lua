-----------------------------------------------------------------------------
-- Shared dict variable
-----------------------------------------------------------------------------

local lua_conf_dict = ngx.shared.lua_conf_dict

-----------------------------------------------------------------------------
-- Url Parse
-----------------------------------------------------------------------------

local uri_block = ngx.re.sub(ngx.var.uri, ".flv", ".block")

local http_block = ngx.location.capture(uri_block)

if (http_block.status == ngx.HTTP_OK) then
	flv = string.sub(http_block.body, 1401, -1)

	ngx.header.content_type = "video/flv"
	ngx.header.content_length = string.len(flv)

	ngx.print(flv)
else
	ngx.exit(http_block.status)
end
