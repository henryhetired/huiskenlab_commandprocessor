#include <iostream>
#include <fstream>
#include <chrono>
using namespace std;
int main(void){
    const long length = 838860800*2;
    uint16_t* data = NULL;
    data = new uint16_t[length];
    for (int i=0;i<length;i++){
        data[i] = (uint16_t) 0;
    }
    ofstream myfile;
    auto startTime = chrono::high_resolution_clock::now();
    myfile.open("/mnt/smb/Henry-SPIM/testcpp.raw",ios::out|ios::binary);
    cout<<"File opened"<<endl;
    if (myfile.is_open()){
        myfile.write((char*) data,sizeof(uint16_t)*length);
    }
    auto writeTime = chrono::high_resolution_clock::now();
    auto elapsedtime1 = chrono::duration_cast<chrono::milliseconds>(writeTime-startTime);
    cout<<"File writing took:"<<elapsedtime1.count()<<" ms"<<endl;
    cout<<"File writing complete"<<endl;
    myfile.close();
    auto endTime = chrono::high_resolution_clock::now();
    auto elapsedtime2 = chrono::duration_cast<chrono::milliseconds>(endTime-writeTime);
    cout<<"File closed"<<endl;
    cout<<"File closing took "<<elapsedtime2.count()<<" ms"<<endl;
    return(0);
    




}