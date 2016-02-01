#include "Arduino.h"
#include <RFduinoBLE.h>

int advertisement_interval = 20;

int interupt_pin = 31;

uint8_t advdata[] =
{
  0x02,  // length
  0x01,  // flags type
  0x04,  // br edr not supported
  
  0x09,  // length
  0xFF,  // manufacturer data type
  
  0x52,
  0x46,
  0x64,
  0x72,
  0x6f,
  0x69,
  0x64,
  0x14
};

void advertise(int interval) {

	Serial.println("advertise()");
	
	//stop advertizing
	RFduinoBLE.end();

	advdata[12]=advertisement_interval;
	RFduinoBLE_advdata = advdata;
	RFduinoBLE_advdata_len = sizeof(advdata);
	RFduinoBLE.advertisementInterval = interval;

	//start advertizing again
	RFduinoBLE.begin();
}

bool interrupting(){

	if(RFduino_pinWoke(interupt_pin)){

		RFduino_resetPinWake(interupt_pin);

		return HIGH;
	}
	return LOW;
}

void interrupt(){

	Serial.println("interrupt()");

	NRF_GPIO->PIN_CNF[interupt_pin] = (GPIO_PIN_CNF_PULL_Pullup<<GPIO_PIN_CNF_PULL_Pos);

	RFduino_pinWake(interupt_pin,HIGH);

	NRF_GPIO->PIN_CNF[interupt_pin] = (GPIO_PIN_CNF_PULL_Pulldown<<GPIO_PIN_CNF_PULL_Pos);
}


void setup() {

	Serial.begin(9600);
	Serial.println("setup()");

	pinMode(interupt_pin, INPUT_PULLDOWN);
	NRF_GPIO->PIN_CNF[interupt_pin] = (GPIO_PIN_CNF_PULL_Pulldown<<GPIO_PIN_CNF_PULL_Pos);

	RFduinoBLE_advdata = advdata;
	RFduinoBLE_advdata_len = sizeof(advdata);

	RFduinoBLE.advertisementInterval = advertisement_interval;

	//start advertizing
	RFduinoBLE.begin();
}

void loop() {

	Serial.println("loop()");

	RFduino_ULPDelay(INFINITE); 

	if(interrupting()){

		advertise(advertisement_interval);
	}
}

void RFduinoBLE_onDisconnect() {
}

void RFduinoBLE_onReceive(char *data, int len) {

	Serial.println("onReceive()");

	if (data!=0) {

		Serial.print("setting interval to : ");
		Serial.print(atoi(data));
		Serial.println(" ms");

		advertisement_interval=atoi(data);
		interrupt();
	}
}

int main() {
	init();
	setup();
	while(1)
		loop();
	return 0;
}
