import numpy as np 
from tifffile import imread,imsave
import os
from threading import Thread
from queue import Queue
import socket
data = imread("Z:/Henry speed test/test2.tif").flatten()
#data can be any data array of length 2048x2048x400 , 2 bytes each entry
print("generated")
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
ip = '10.129.11.254'
# ip = 'localhost'
server_address = (ip,53705)
sock.connect(server_address)
sock.sendall(("filewritingrequest 400 2048 2048 /mnt/smb/Henry-SPIM/testpython.raw").encode())
msg = sock.recv(1024)
port = int(msg.decode().split()[2])
sock.close()
server_address = (ip,port)
while True:
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(server_address)
        for i in range(400):
            sock.sendall(data[i*2048*2048:(i+1)*2048*2048])
        sock.close()
        print("data sent")
        break
    except ImportError:
        print("wat")
    
