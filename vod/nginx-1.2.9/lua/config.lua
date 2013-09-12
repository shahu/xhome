-----------------------------------------------------------------------------
-- Imports and dependencies
-----------------------------------------------------------------------------

--cjson = require "cjson"

-----------------------------------------------------------------------------
-- Configure shared dict variable
-----------------------------------------------------------------------------

local lua_conf_dict = ngx.shared.lua_conf_dict

lua_conf_dict:set("ramdisk_path", "/mnt/resource/ramdisk")
lua_conf_dict:set("pptransfer_program", "/usr/local/nginx/wsgi/ppbox_transfer-linux-x64-gcc44-mt")
