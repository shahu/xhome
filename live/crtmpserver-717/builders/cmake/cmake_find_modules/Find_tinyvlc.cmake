MESSAGE(STATUS "Looking for tinyvlc")
FIND_PATH(TINYVLC_INCLUDE_PATH 
	NAMES
		tinyvlc.h
	PATHS
		/usr/include
		/usr/local/include
		/sw/include
		/opt/local/include
		NO_DEFAULT_PATH)

FIND_LIBRARY(TINYVLC_LIBRARY_PATH
	NAMES
		tinyvlc
	PATHS
		/usr/lib
		/usr/lib64
		/usr/local/lib
		/usr/local/lib64
		/sw/lib
		/opt/local/lib
		NO_DEFAULT_PATH)

IF(TINYVLC_INCLUDE_PATH)
	MESSAGE(STATUS "Looking for tinyvlc headers - found")
ELSE(TINYVLC_INCLUDE_PATH)
	MESSAGE(STATUS "Looking for tinyvlc headers - not found")
ENDIF(TINYVLC_INCLUDE_PATH)

IF(TINYVLC_LIBRARY_PATH)
	MESSAGE(STATUS "Looking for tinyvlc library - found")
ELSE(TINYVLC_LIBRARY_PATH)
	MESSAGE(STATUS "Looking for tinyvlc library - not found")
ENDIF(TINYVLC_LIBRARY_PATH)

IF(TINYVLC_INCLUDE_PATH AND TINYVLC_LIBRARY_PATH)
	SET(TINYVLC_FOUND 1 CACHE STRING "Set to 1 if tinyvlc is found, 0 otherwise")
ELSE(TINYVLC_INCLUDE_PATH AND TINYVLC_LIBRARY_PATH)
	SET(TINYVLC_FOUND 0 CACHE STRING "Set to 1 if tinyvlc is found, 0 otherwise")
	SET(TINYVLC_LIBRARY_PATH "tinyvlc")
	SET(TINYVLC_INCLUDE_PATH "${CRTMPSERVER_3RDPARTY_ROOT}/tinyvlc")
	MESSAGE(STATUS "Defaulting to ${CRTMPSERVER_3RDPARTY_ROOT}/tinyvlc")
ENDIF(TINYVLC_INCLUDE_PATH AND TINYVLC_LIBRARY_PATH)

MARK_AS_ADVANCED(TINYVLC_FOUND)
