#include "Arduino.h"
#include <RFduinoBLE.h>

int advertisement_interval = 20;

int interupt_pin = 31;

void advertise(int interval) {

	Serial.println("advertise()");
	
	//stop advertizing
	RFduinoBLE.end();

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

	RFduinoBLE.advertisementData = "RFduino adverter";
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

	if (strcmp(data,"bubulle")==0) {

		char dataRet[]= "La porte s'ouvre...";
		RFduinoBLE.send(dataRet,strlen(dataRet));

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
