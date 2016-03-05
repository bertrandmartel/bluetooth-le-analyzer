#include "Arduino.h"
#include <RFduinoBLE.h>

uint16_t advertisement_interval = 80;

int interupt_pin = 31;

uint8_t advdata[] =
{
  0x02,  // length
  0x01,  // flags type
  0x04,  // br edr not supported
  
  0x08,  //length
  0x09,  //device name
  0x52,  //RFdroid
  0x46,
  0x64,
  0x72,
  0x6f,
  0x69,
  0x64,

  0x0A,  // length
  0xFF,  // manufacturer data type
  
  0x52,
  0x46,
  0x64,
  0x72,
  0x6f,
  0x69,
  0x64,
  0x00,
  0x50
};

void advertise(uint16_t interval) {

	Serial.println("advertise()");
	
	//stop advertizing
	RFduinoBLE.end();

	advdata[21]=(uint8_t)(advertisement_interval>>8);
	advdata[22]=(uint8_t)(advertisement_interval);
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
	Serial.println("onDisconnect");
	interrupt();
}

void RFduinoBLE_onConnect() {
  Serial.println("onConnect");
}
void RFduinoBLE_onReceive(char *data, int len) {

	Serial.println("onReceive()");

	if (data!=0 && len > 1) {

		uint16_t ad_interval = (uint16_t)((data[0] << 8) + data[1]);
		Serial.print("setting interval to : ");
		Serial.print(ad_interval);
		Serial.println(" ms. Take effect on next disconnection");
		advertisement_interval = ad_interval;
		char dataRet[]= "OK";
		RFduinoBLE.send(dataRet,strlen(dataRet));
	}
}

int main() {
	init();
	setup();
	while(1)
		loop();
	return 0;
}
