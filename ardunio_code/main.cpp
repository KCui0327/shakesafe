#include <DHT.h>  // temperature sensor library

//definitions
#define BUZZER A1
#define TEMP_SENSOR 2
#define DHTTYPE DHT11
#define SOUND_SENSOR A0

DHT dht(TEMP_SENSOR, DHTTYPE);

void setup() {
Serial.begin(9600);
  pinMode(8, OUTPUT); // put your setup code here, to run once:
  pinMode(BUZZER, OUTPUT);
  dht.begin();
}

void loop() {
  //sound sensor
  int soundValue = 0; //create variable to store many different readings
  for (int i = 0; i < 32; i++) //create a for loop to read 
  { soundValue += analogRead(SOUND_SENSOR);  } //read the sound sensor
 
  soundValue >>= 5; //bitshift operation 
  
  //send values to Snapdragon
  Serial.print("Sound Value: ");
  Serial.print(soundValue); //print the value of sound sensor
  Serial.println("dB");

  //temp sensor
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  //Send values to Snapdragon
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.println("*C");
  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.println("%");

  
  if(Serial.available()>=0) {
    char data = Serial.read(); // reading the data received from the bluetooth module

    switch(data) {
      case '1': {  // Button press to activate buzzer and LED
        digitalWrite(8, HIGH); 
        SerialUSB.println("HIGH");
        digitalWrite(BUZZER, HIGH); //write buzzer high
        break;
      }
      case '2': { // Button press to deactivate buzzer and LED
        digitalWrite(8, LOW); 
        SerialUSB.println("LOW");
        digitalWrite(BUZZER, LOW); //write buzzer low
        break; // when d is pressed on the app on your smart phone
      }
      default : break;
    }
  }
  delay(1000);
} 
