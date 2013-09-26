-- Start of the configuration. This is the only node in the config file. 
-- The rest of them are sub-nodes
configuration=
{
	-- if true, the server will run as a daemon.
	-- NOTE: all console appenders will be ignored if this is a daemon
	daemon=true,
	-- the OS's path separator. Used in composing paths
	pathSeparator="/",

	-- this is the place where all the logging facilities are setted up
	-- you can add/remove any number of locations
	streamMonitor={
		monitorPath="/mnt/resource/logs/crtmpserver/streamMonitor.log",
		speedPath="/mnt/resource/logs/crtmpserver/speed.log",
	},
	logAppenders=
	{
		{
			-- name of the appender. Not too important, but is mandatory
			name="console appender",
			-- type of the appender. We can have the following values:
			-- console, coloredConsole and file
			-- NOTE: console appenders will be ignored if we run the server
			-- as a daemon
			type="coloredConsole",
			-- the level of logging. 6 is the FINEST message, 0 is FATAL message.
			-- The appender will "catch" all the messages below or equal to this level
			-- bigger the level, more messages are recorded
			level=6
		},
		{
			name="file appender",
			type="file",
			level=6,
			-- the file where the log messages are going to land
			fileName="/mnt/resource/logs/crtmpserver/crtmpserver",
			--newLineCharacters="\r\n",
			fileHistorySize=10,
			fileLength=512*1024*1024,
			singleLine=true
		}
	},
	
	-- this node holds all the RTMP applications
	applications=
	{
		-- this is the root directory of all applications
		-- usually this is relative to the binary execuable
		rootDirectory="applications",
		
		--this is where the applications array starts
		{
			-- The name of the application. It is mandatory and must be unique 
			name="appselector",
			-- Short description of the application. Optional
			description="Application for selecting the rest of the applications",
			
			-- The type of the application. Possible values are:
			-- dynamiclinklibrary - the application is a shared library
			protocol="dynamiclinklibrary",
			-- the complete path to the library. This is optional. If not provided, 
			-- the server will try to load the library from here
			-- <rootDirectory>/<name>/lib<name>.{so|dll|dylib}
			-- library="/some/path/to/some/shared/library.so"
			
			-- Tells the server to validate the clien's handshake before going further. 
			-- It is optional, defaulted to true
			validateHandshake=true,
			-- this is the folder from where the current application gets it's content.
			-- It is optional. If not specified, it will be defaulted to:
			-- <rootDirectory>/<name>/mediaFolder
			-- mediaFolder="/some/directory/where/media/files/are/stored"
			-- the application will also be known by that names. It is optional
			--aliases=
			--{
			--	"simpleLive",
			--	"vod",
			--	"live",
			--},
			-- This flag designates the default application. The default application
			-- is responsable of analyzing the "connect" request and distribute 
			-- the future connection to the correct application.

			--generateMetaFiles=false, --this will generate seek/meta files on application startup
			--renameBadFiles=false,
			--enableCheckBandwidth=false,
			--default=true,
			acceptors = 
			{
				{
					ip="0.0.0.0",
					port=1935,
					protocol="inboundRtmp"
				},
			}
		},
		{
			description="Forwarding streams to another RTMP server",
			name="proxypublish",
			protocol="dynamiclinklibrary",
			acceptors = 
			{
				{
					ip="0.0.0.0",
					port=6667,
					protocol="inboundLiveFlv",
					waitForMetadata=true
				},
			},

			abortOnConnectError=false,

			targetServers = {
				{ 
					targetUri="rtmp://42.159.6.172:1936/push", 
					targetStreamType="live"
				}
			},
 
			externalStreams = 
			{
				{
					--uri="rtmp://123.123.123.123/live/myStream",
					forceTcp=true
				}
			}
		},
		{
			description="FLV Playback Sample",
			name="flvplayback",
			protocol="dynamiclinklibrary",
			mediaFolder="/Volumes/android/backup/media/",
			aliases=
			{
				"simpleLive",
				"vod",
				"live",
				"WeeklyQuest",
				"SOSample",
				"oflaDemo",
			},
			acceptors = 
			{
				{
					ip="0.0.0.0",
					port=6666,
					protocol="inboundLiveFlv",
					waitForMetadata=true,
				},
				{
					ip="0.0.0.0",
					port=554,
					protocol="inboundRtsp",
					waitForMetadata=true,
				},
			},
			externalStreams = 
			{
				{
					upstream="rtmp://RTMPUpstream:1935/flvplayback/",
					localStreamName="rtmptest",
					forceTcp=true,
					maxIdle=100,
					--swfUrl="http://www.example.com/example.swf";
					--pageUrl="http://www.example.com/";
					--emulateUserAgent="MAC 10,1,82,76",
				}
			},
			validateHandshake=true,
			keyframeSeek=true,
			seekGranularity=1.5, --in seconds, between 0.1 and 600
			clientSideBuffer=12, --in seconds, between 5 and 30
			--generateMetaFiles=true, --this will generate seek/meta files on application startup
			--renameBadFiles=false,
			--enableCheckBandwidth=true,
			--[[authentication=
			{
				rtmp={
					type="adobe",
					encoderAgents=
					{
						"FMLE/3.0 (compatible; FMSc/1.0)",
						"My user agent",
					},
					usersFile="users.lua"
				},
				rtsp={
					usersFile="users.lua"
				}
			}, --]]
		},
		--#INSERTION_MARKER# DO NOT REMOVE THIS. USED BY appscaffold SCRIPT.
	}
}

