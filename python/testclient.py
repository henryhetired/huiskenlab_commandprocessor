import numpy as np 
from tifffile import imread,imsave
import os
from threading import Thread
from queue import Queue
import socket
import time
data = imread("/home/henryhe/Documents/test2.tif").flatten()
# data = bytearray(os.urandom(2048*2048*5*2))
#data can be any data array of length 2048x2048x400 , 2 bytes each entry
print("generated")
num_planes = 410
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# ip = '10.129.11.254'
ip = '127.0.0.1'
server_address = (ip,53705)
sock.connect(server_address)
# msgout = ("runcommand filewritingrequest %d 2048 2048 /home/henryhe/Documents/testpython2.ome.raw false\n"%num_planes).encode()
msgout = ("runcommand convertandrender %d 2048 2048 /home/henryhe/Documents/\n"%num_planes).encode()
# msgout = ("runcommand convertandrender %d 2048 2048\n"%num_planes).encode()
sock.sendall(msgout)
msg = sock.recv(1024)
print(msg.decode().split())
port = int(msg.decode().split()[2])
sock.shutdown(socket.SHUT_RD)
sock.close()
server_address = (ip,port)
time.sleep(1)
while True:
    try:
        sock2 = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock2.connect(server_address)
        for i in range(num_planes):
            sock2.sendall(data[i*2048*2048:(i+1)*2048*2048])
            # time.sleep(0.01)
        print("data sent")
        # msg = sock2.recv(1024).decode()
        # print(msg)
        sock2.close()
        
        break
    except ImportError:
        print("wat")
    
