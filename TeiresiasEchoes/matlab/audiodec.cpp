#include <iostream>
#include <cstdio>
#include <array>
using namespace std;

#define MSG_L 128+4

array<int16_t, 2*MSG_L> decode(int8_t* msg, int16_t mu, int16_t beta){
  array<int16_t, 2*MSG_L> decodedMsg;

  int8_t d1= msg[0]>>4 & 0x0F;
  int8_t d2= msg[0]    & 0x0F;
  int mean= 0;
  decodedMsg[0]= (d1-8)*beta + mu;
  decodedMsg[1]= (d2-8)*beta + decodedMsg[0];
  mean+= decodedMsg[0]+decodedMsg[1];
  for(int i=1; i<MSG_L; i++){
    d1= msg[i]>>4 & 0x0F;
    d2= msg[i]    & 0x0F;
    decodedMsg[2*i]=   (d1-8)*beta + decodedMsg[2*i-1];
    decodedMsg[2*i+1]= (d2-8)*beta + decodedMsg[2*i];
    mean+= decodedMsg[2*i]+decodedMsg[2*i+1];
  }
  mean/= 2*MSG_L;
  for(int i=0; i<2*MSG_L; i++) decodedMsg[i]+= mu-mean;
  return decodedMsg;
}

int main(int argc, char** argv){
  if(argc < 2) { std::cerr << "Supply file name\n"; return 1; }
  string fname(argv[1]);
  FILE* f= fopen(fname.c_str(), "r");
  FILE* fout= fopen((fname+"_samp").c_str(), "w");
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
