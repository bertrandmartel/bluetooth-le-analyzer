OBJECTS=adverter.o rfduino-makefile/RFduino/libraries/RFduinoBLE/RFduinoBLE.o
HEADERS=-Irfduino-makefile/RFduino/libraries/RFduinoBLE

export OBJECTS
export HEADERS

.PHONY: all

all:
	$(MAKE) -C rfduino-build/rfduino-makefile 

build:
	$(MAKE) -C rfduino-build/rfduino-makefile rfduino_lib build

clean:
	$(MAKE) -C rfduino-build/rfduino-makefile clean

distclean:
	$(MAKE) -C rfduino-build/rfduino-makefile distclean
