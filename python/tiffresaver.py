import numpy as np 
from tifffile import imread,imsave
import os
import threading
import queue
class tiffresaver:
    def __init__(self,port_start,num_threads):
        self.tcpport = port
        self.command_queue = Queue()
        self.
    def resave_data(data,shape=(500,2048,2048),filename = ""):
        if len(data)!=shape[0]*shape[1]*shape[2]:
            print("The file size does not match the length of the array")
            return
        else:
            data = np.reshape(data,shape)
            imsave(os.path.splitext(filename)+".tif",data)
            return
    def worker(self,outport,)