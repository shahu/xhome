
#include ../Rules.mk

# Import the variables from the DVSDK so that you can find the DVSDK components
DVSDK=/home/patron/dvsdk_2_10_01_18
include $(DVSDK)/Rules.make
#MVTOOL_PREFIX=

TARGET = libxhome.so
C_FLAGS += -Wall -g
LD_FLAGS += -lpthread
RELTARGET = release/$(TARGET)

COMPILE.c = $(MVTOOL_PREFIX)gcc $(C_FLAGS) $(CPP_FLAGS) -c
LINK.c = $(MVTOOL_PREFIX)gcc -shared -fPIC $(LD_FLAGS) 
#RELCFLAGS = -O2 -fno-strict-aliasing

SOURCES = $(wildcard *.c)
HEADERS = $(wildcard *.h) 
RELOBJFILES = $(SOURCES:%.c=release/%.o)
.PHONY: clean release install test

all:    release test
test:test/main.c
	$(MVTOOL_PREFIX)gcc $(C_FLAGS) $(CPP_FLAGS) -o release/main -I. test/main.c -L./release -lxhome
release:    $(RELTARGET)

$(RELTARGET):   $(RELOBJFILES) 
	$(LINK.c) -o $@ $^ $(RELLDFLAGS)


$(RELOBJFILES): release/%.o: %.c $(HEADERS)
	@mkdir -p release
	$(COMPILE.c) $(RELCFLAGS) -o $@ $<

clean:
	-$(RM) -rf release  *.d


