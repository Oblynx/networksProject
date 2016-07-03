#include <iostream>
#include <cstdio>
#include <array>
using namespace std;

#define MSG_L 128+4

array<int16_t, 2*MSG_L> decode(int8_t* msg, int16_t mu, int16_t beta){
  array<int16_t, 2*MSG_L> decodedMsg;

  int8_t d1= msg[0]>>4 & 0x0F;
  int8_t d2= msg[0]    & 0x0F;
  decodedMsg[0]= (d1-8)*beta;
  decodedMsg[1]= (d2-8)*beta+ decodedMsg[0];
  for(int i=0; i<MSG_L; i++){
    d1= msg[i]>>4 & 0x0F;
    d2= msg[i]    & 0x0F;
    decodedMsg[2*i]= (d1-8)*beta + decodedMsg[2*i-1];
    decodedMsg[2*i+1]= (d2-8)*beta + decodedMsg[2*i];
  }
  return decodedMsg;
}

int main(){
  FILE* f= fopen("permLogs/10-06 02:18:59/musicV2065.log_diff", "r");
  FILE* fout= fopen("permLogs/10-06 02:18:59/musicV2065.log_samp", "w");
  if(!f) {cerr << "Error opening fin!\n"; return 1; }
  if(!fout) {cerr << "Error opening fout!\n"; return 1; }
  int8_t msg[MSG_L];

  int count=0;
  while(fread(msg, 1, MSG_L, f) == MSG_L){
    cout << '.';
    int16_t mu=   msg[1]<<8 | (msg[0] & 0x00FF);
    int16_t beta= msg[3]<<8 | (msg[2] & 0x00FF);
    auto dmsg= decode(msg+4, mu, beta);
    fwrite(&dmsg[0], 2, 2*MSG_L, fout);
    count++;
  }
  cout << "\n"<<count<<" messages\n";
  return 0;
}
