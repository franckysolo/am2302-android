/**
 * Script for reading Adadfruit am2302 sensor
 * @author franckysolo
 */
#include <SimpleDHT.h>

int DHT22_PIN = 2;
SimpleDHT22 dht;

void setup() {
  Serial.begin(115200);
}

void loop() {
  float temperature = 0;
  float humidity = 0;
  int error = SimpleDHTErrSuccess;
  if ((error = dht.read2(DHT22_PIN, &temperature, &humidity, NULL)) != SimpleDHTErrSuccess) {
    Serial.print("Read DHT22 failed, err=");
    Serial.println(error);
    delay(2000);
  }
  Serial.print((float)humidity);
  Serial.print(":");
  Serial.print((float)temperature);
  Serial.println();
  delay(2500);
}
