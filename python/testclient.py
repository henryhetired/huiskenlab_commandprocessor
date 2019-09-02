import numpy as np 
from tifffile import imread,imsave
import os
from threading import Thread
from queue import Queue
import socket
data = imread("/mnt/fileserver/Henry speed test/test.tif").flatten()
print("generated")
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ('localhost',20000)
sock.connect(server_address)
sock.sendall("filewritingrequest 20 2048 2048 /mnt/fileserver/Henry-SPIM/test.raw".encode())
msg = sock.recv(1024)
port = int(msg.decode().split()[2])
sock.close()
server_address = ('localhost',port)
while True:
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect(server_address)
        for i in range(10):
            sock.sendall(data[i*2048*2048*2:(i+1)*2048*2048*2])
        sock.close()
        print("data sent")
        break
    except ImportError:
        print("wat")
    
