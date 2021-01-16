
#include "HX711.h"
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <WiFi.h>

#define DEBUG false

#define LOADCELL_DOUT_PIN  5
#define LOADCELL_SCK_PIN  2

#define WEIGHT_SERVICE_UUID "dff971a9-142a-4021-a8d2-f5298ab2bdbb"
#define SETTINGS_CHARACTERISTIC_UUID "76053035-3aa1-4148-a70d-a73e35332418"
#define STATUS_CHARACTERISTIC_UUID "c5c78e8f-5963-4642-bd24-bbb8507e22ca"
#define CALIBRATION_WEIGHT_CHARACTERISTIC_UUID "18d456b3-3c7b-43fa-9d3c-db867d2a93b2"
#define CALIBRATION_VALUE_CHARACTERISTIC_UUID "e5c96eed-c523-4e81-9d8b-1f92f58603dc"
#define WEIGHT_CHARACTERISTIC_UUID "2A98"
#define ESPRESSO_WEIGHT_CHARACTERISTIC_UUID "d0dac8e6-cf56-4e0c-9823-0aed58dc9bfe"
#define ESPRESSO_TIME_CHARACTERISTIC_UUID "6e980e27-b771-485a-8396-42f1dab56506"

HX711 scale;

float calibration_factor = 4386.18; 
float tare_value = 0.0;
float weight = 0.0;
float tared_weight = 0.0;
float calibration_weight = 0.0;
boolean calibration_weight_is_set = false;

String modus = "WEIGHT_MODUS";
String state = "READY";

unsigned long espresso_start;
unsigned long espresso_end;
unsigned long espresso_time;

BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pCharacteristicWeight;
BLECharacteristic *pCharacteristicCalibrationWeight;
BLECharacteristic *pCharacteristicCalibrationValue;
BLECharacteristic *pCharacteristicSettings;
BLECharacteristic *pCharacteristicStatus;
BLECharacteristic *pCharacteristicEspressoWeight;
BLECharacteristic *pCharacteristicEspressoTime;

void setStatus(String state_param)
{
    state = state_param;
    pCharacteristicStatus->setValue(state.c_str());
    pCharacteristicStatus->notify();
}

void setModus(String modusParam)
{
  modus= modusParam;
  pCharacteristicSettings->setValue(modus.c_str());
  pCharacteristicSettings->notify();
}

void setCalibrationFaktor(float faktore)
{
   char result[50];
   sprintf(result,"%f",faktore);
   pCharacteristicCalibrationValue->setValue(result);
   pCharacteristicCalibrationValue->notify();
}

void setEspressoResult(long espresso_time,float tared_weight)
{
   String result = String();
   result.concat("{\"results\":[");
   char tmp_result[50];
   sprintf(tmp_result,"{\"t\":%d,\"w\":%.2f}",espresso_time,tared_weight);
   result.concat(String(tmp_result));
   result.concat("]}");
  
  char timeString[50];
  result.toCharArray(timeString,50); 
  pCharacteristicEspressoTime->setValue(timeString); 
  pCharacteristicEspressoTime->notify();
}

void setEspressoResults(unsigned long _times[], float weights[])
{
   char char_result[300];
   memset(char_result,'\0', 300);
   String result = String();
   result.concat("{\"results\":[");
   int delay_counter = 0;
   for(int i=0;i<10;i++)
   {
     if(weights[i] > -1.0)
     {
       delay_counter++;
       char tmp_result[35];
       memset(tmp_result,'\0', sizeof(tmp_result));
       sprintf(tmp_result,"{\"t\":%d,\"w\":%.2f}",_times[i],weights[i]);
       result.concat(tmp_result);
  
       if(i >= 9)
        continue;

       if(weights[i+1] >-1.0)
       {
         result.concat(',');
       }
       
     }
   }
   result.concat("]}");
   result.toCharArray(char_result,300); 

  pCharacteristicEspressoTime->setValue(char_result); 
  pCharacteristicEspressoTime->notify();
  delay(35*delay_counter); // Waiting until data is send
}

void setWeight(float weight_param)
{
  weight = weight_param;
  char floatString[20];
  sprintf(floatString,"%.2f",tared_weight);
  tared_weight = tareWeight(weight_param);
  pCharacteristicWeight->setValue(floatString);
  pCharacteristicWeight->notify();
#if DEBUG
  char output[50];
  sprintf(output,"Weight %.2f g",tared_weight);
  Serial.println(output);
#endif
}

class MyCallbacks: public BLECharacteristicCallbacks {
  
    void onWrite(BLECharacteristic *pCharacteristic) 
    {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) 
      {
        String val = "";
        for (int i = 0; i < value.length(); i++){
          val  = val + value[i];
        }
#if DEBUG        
        Serial.println("Get value:"+val);
#endif        
        if(value == "tare")
        {
#if DEBUG           
           Serial.println("Tare weight");
#endif
           tare_value = weight;
        }
        if(value == "WEIGHT_MODUS")
        {
#if DEBUG
           Serial.println("Set modus to WEIGHT_MODUS");
#endif
           setModus("WEIGHT_MODUS");
        }
        if(value == "ESPRESSO_MODUS")
        {
#if DEBUG 
           Serial.println("Set modus to ESPRESSO_MODUS");
#endif
           setModus("ESPRESSO_MODUS");
        }
        if(value == "CALIBRATION_MODUS")
        {
#if DEBUG 
           Serial.println("Set modus to CALIBRATION_MODUS");
#endif
           setModus("CALIBRATION_MODUS");
        }
        
      }
    }
};

class CalibrationWeightCallbacks: public BLECharacteristicCallbacks {
  
    void onWrite(BLECharacteristic *pCharacteristic) 
    {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) 
      {
        String val = "";
        for (int i = 0; i < value.length(); i++){
          val  = val + value[i];
        }
#if DEBUG 
        Serial.println("Get value:"+val);
#endif
        calibration_weight = val.toFloat();
        calibration_weight_is_set = true; 
      }
    }
};

class CalibrationValueCallbacks: public BLECharacteristicCallbacks {
  
    void onWrite(BLECharacteristic *pCharacteristic) 
    {      
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) 
      {
        String val = "";
        for (int i = 0; i < value.length(); i++){
          val  = val + value[i];
        }
#if DEBUG 
        Serial.println("Get value:"+val);
#endif
        
        calibration_factor = val.toFloat();
        scale.set_scale(calibration_factor);
      }
    }
};

void setup() {
  Serial.begin(9600);

  WiFi.mode( WIFI_MODE_NULL );

  int i = 0;
  while(!scale.is_ready())
  {
    scale.begin(LOADCELL_DOUT_PIN, LOADCELL_SCK_PIN);

    if(i > 0)
    {
      delay(100);
      Serial.println("Can't detect scale.");
    }
    i++;
  }
  scale.set_scale();
  scale.tare();
  scale.set_scale(calibration_factor);

  BLEDevice::init("ESPresso32");
  pServer = BLEDevice::createServer();
  pService = pServer->createService(WEIGHT_SERVICE_UUID);

  // BLECharacteristic::PROPERTY_WRITE
  pCharacteristicWeight = pService->createCharacteristic(
                                         WEIGHT_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                       );

                                       
  pCharacteristicSettings = pService->createCharacteristic(
                                         SETTINGS_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                       );
  pCharacteristicEspressoWeight = pService->createCharacteristic(
                                         ESPRESSO_WEIGHT_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                       );
  pCharacteristicEspressoTime = pService->createCharacteristic(
                                         ESPRESSO_TIME_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                       );
  pCharacteristicStatus = pService->createCharacteristic(
                                         STATUS_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                       );
  pCharacteristicCalibrationWeight = pService->createCharacteristic(
                                         CALIBRATION_WEIGHT_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE |
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                        );
  pCharacteristicCalibrationValue = pService->createCharacteristic(
                                         CALIBRATION_VALUE_CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE|
                                         BLECharacteristic::PROPERTY_NOTIFY |
                                         BLECharacteristic::PROPERTY_INDICATE
                                         );

  pCharacteristicSettings->setCallbacks(new MyCallbacks());
  pCharacteristicCalibrationWeight->setCallbacks(new CalibrationWeightCallbacks());
  pCharacteristicCalibrationValue->setCallbacks(new CalibrationValueCallbacks());
 
  
  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(WEIGHT_SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();

  setModus("WEIGHT_MODUS");
  setCalibrationFaktor(calibration_factor);
  setStatus("READY");
}

void tare()
{
  tare_value += tared_weight;
}

void messure_weight()
{
#if DEBUG
  Serial.print("Reading: ");
#endif
  setWeight(scale.get_units(10));
}

void messure_weight_fast()
{
#if DEBUG 
  Serial.print("Reading fast: ");
#endif
  setWeight(scale.get_units(1));
}

float tareWeight(float weight_param)
{
  return weight_param - tare_value;
}

//TODO  make a reset that we not get into infinite loop
// have in minde negative values (do a error flag and make a reboot posible from bluethooth?)
void messure_espresso()
{

  setStatus("TARE");
  do{
#if DEBUG
    Serial.println("Tare...");
#endif
    tare();
    delay(100);
    messure_weight();
  }while(tared_weight > 0.1);
#if DEBUG     
  Serial.print("Waiting for coffee..");
#endif
  setStatus("WAITING");
  
  float start_weight = scale.get_units(3);
  int sameCounter = 0;
  int tmp_index = 0;
  float tmp_weights[10] = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
  unsigned long tmp_times[10] = {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
  while(true)
  {
    float tmp_weight = scale.get_units(3);
    if(tmp_weight-start_weight > 0.2)
    {      
      
      if(sameCounter == 0)
      {
       espresso_start = millis();
      }

      setWeight(tmp_weight);
      tmp_weights[tmp_index] = tared_weight;
      tmp_times[tmp_index] = millis()-espresso_start;
      tmp_index++;
      sameCounter++;
    }
    else
    {
      for(int i=0;i<10;i++)
      {
        tmp_weights[i] = -1;
        tmp_times[i] = -1;
      }
      tmp_index = 0; 
      sameCounter=0;
    }

    
    if(start_weight-tmp_weight > 0.2 )
    {
#if DEBUG
      Serial.print("Seem to me that the weight of the cup has changed will tare this. ;-)");
#endif
      setWeight(scale.get_units(3));
      tare();
      start_weight = scale.get_units(3);
    }

    if(sameCounter >= 7)
    {
      break;  
    }
#if DEBUG   
    Serial.println("Waiting...");
#endif
  }
  setStatus("MEASSURE");
  
  // Sent stored tmp results to get complete diagram
  setEspressoResults(tmp_times,tmp_weights);


  for(int i=0;i<10;i++)
  {
    tmp_weights[i] = -1;
    tmp_times[i] = -1;
  }
  tmp_index = 0; 
  sameCounter = 0;
  unsigned long espresso_end_tmp;
  while(true)
  {
#if DEBUG 
    Serial.println("Coffee flows....");
#endif    
    float tmp_weight = scale.get_units(3);
    unsigned long time_on_weight = millis();
    float weight_diff = fabs(tmp_weight-weight);
    setWeight(tmp_weight);

    if(weight_diff < 0.2 )
    {
      if(sameCounter == 0)
      {
        espresso_end_tmp = time_on_weight;
      }
      tmp_weights[tmp_index] = tared_weight;
      tmp_times[tmp_index] = time_on_weight-espresso_start;
      tmp_index++;
      sameCounter++;
    }
    else if(sameCounter > 0) // It seem that there was a change again its not the end until now
    {
      setEspressoResults(tmp_times,tmp_weights);
      for(int i=0;i<10;i++)
      {
        tmp_weights[i] = -1;
        tmp_times[i] = -1;
      }
      tmp_index = 0; 
      sameCounter = 0;
      continue;
    }
    
    if(sameCounter >= 3)
    {
      espresso_end = espresso_end_tmp;
      break;
    }

    if(sameCounter == 0) // Without this check the diagram can have a back jump
    {
      setEspressoResult(time_on_weight-espresso_start,tared_weight);
    }
  }
  espresso_time = espresso_end - espresso_start;
#if DEBUG 
  Serial.print("Espresso is ready :-)");
#endif  
  setEspressoResult(espresso_time,tared_weight);
  setModus("WEIGHT_MODUS");
  setStatus("READY");
}

void calibrate()
{
  float allowed_delta = 20.00; // unit g
  float add_factor = 50.00;

#if DEBUG  
  Serial.print("Waiting for calibration weight");
#endif  
  pCharacteristicStatus->setValue("WAITING_FOR_CALIBRATION_WEIGHT");
  while(!calibration_weight_is_set)
  {
   Serial.println("Waiting..."); 
   delay(10);
  }
  char status_message[20];
#if DEBUG 
  char message[100];
  sprintf(message,"Callibration weight is set to %.2f. Start Auto calibration. Please wait...",calibration_weight);
#endif
  setStatus("AUTO_CALIBRATE");

 
  messure_weight_fast();
  int phase = 1;
  while(true)
  {
#if DEBUG 
    char message[100];
    sprintf(message,"Calibrate phase %d ... factor is now %.2f.",phase,calibration_factor);
    Serial.println(message);
#endif
    if(fabs(weight-calibration_weight) <= allowed_delta)
    {
      phase++;
      allowed_delta = allowed_delta / 10;
      add_factor = add_factor / 10;
      
      if(phase > 5)
      {
        break;
      }
      
      sprintf(status_message,"AUTO_CALIBRATE_PHASE_%d",phase);
      setStatus(status_message);
    }

    if(weight-calibration_weight > 0 )
    {
      calibration_factor += add_factor;
      scale.set_scale(calibration_factor);
    }
    else if (weight-calibration_weight < 0)
    {
      calibration_factor -= add_factor;
      scale.set_scale(calibration_factor);
    }
    
    if(phase < 4)
    {
      messure_weight_fast();
    }
    else
    {
      messure_weight();
    }
    delay(10);
  }

  delay(100);
  setCalibrationFaktor(calibration_factor);
  calibration_weight_is_set = false;
  setModus("WEIGHT_MODUS");
  setStatus("READY");
}

void loop() {

  if(modus == "WEIGHT_MODUS")
  {
    messure_weight();
  }
  if(modus == "ESPRESSO_MODUS")
  {
    messure_espresso();
  }
  if(modus == "CALIBRATION_MODUS")
  {
    calibrate();
  }
}
