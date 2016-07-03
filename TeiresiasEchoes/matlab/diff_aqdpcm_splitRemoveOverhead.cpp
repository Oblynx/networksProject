#include <iostream>
#include <array>
#include <cstdio>
#include <cstring>
#include <string>
using namespace std;

#define MSG_L (128+4)

array<int8_t, 2*MSG_L> getDiff(int8_t* msg){
  array<int8_t, 2*MSG_L> decodedMsg;

  int8_t d1= msg[0]>>4 & 0x0F;
  int8_t d2= msg[0]    & 0x0F;
  decodedMsg[0]= d1;
  decodedMsg[1]= d2;
  for(int i=0; i<MSG_L; i++){
    d1= msg[i]>>4 & 0x0F;
    d2= msg[i]    & 0x0F;
    decodedMsg[2*i]= d1;
    decodedMsg[2*i+1]= d2;
  }
  return decodedMsg;
}

int main(int argc, char** argv){
  if(argc < 3) { std::cerr << "Supply file name and 0 for DPCM or 1 for AQDPCM\n"; return 1; }
  string fname(argv[1]);

  FILE* fin= fopen(fname.c_str(), "r");
  if(!fin) { std::cerr << "Error opening fin\n"; return 1; }
  FILE* fout= fopen((fname+"_split").c_str(), "w");
  if(!fout) { std::cerr << "Error opening fout\n"; return 2; }
  FILE* fout2= fopen((fname+"_mu").c_str(), "w");
  if(!fout2) { std::cerr << "Error opening fout2\n"; return 2; }
  FILE* fout3= fopen((fname+"_beta").c_str(), "w");
  if(!fout3) { std::cerr << "Error opening fout3\n"; return 2; }
  
  bool aq= strcmp(argv[2], "0");
  int8_t msg[MSG_L];
  while( fread(msg, 1, MSG_L, fin) == MSG_L){
    auto diff= (!aq)? getDiff(msg): getDiff(msg+4);
    fwrite(&diff[0], 1, MSG_L-4, fout);
    if (aq) {
      int16_t mu=   msg[1]<<8 | (msg[0] & 0x00FF);
      int16_t beta= msg[3]<<8 | (msg[2] & 0x00FF);
      fwrite(&mu, 2, 1, fout2);
      fwrite(&beta, 2, 1, fout3);
    }
  }
  return 0;
}
