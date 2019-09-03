import numpy as np 
from tifffile import imread,imsave
import os
from threading import Thread
from queue import Queue
from tqdm import tqdm
import socket
import time
queuesize = 10
start_port = 40000
ip = 'localhost'
port_queue = Queue(maxsize=queuesize)
for i in range(queuesize):
    port_queue.put(start_port+i)
socks = []
for i in range(queuesize):
    socks.append(socket.socket(socket.AF_INET,socket.SOCK_STREAM))
    socks[i].bind((ip,start_port+i))
    socks[i].listen(1)
def file_writing_worker(zsize,ysize,xsize,filename,port):
    connection,_ = socks[port-start_port].accept()
    print("connection made")
    length = zsize*ysize*xsize
    buffersize = length*2
    data = b''
    while len(data) < buffersize:
        # doing it in batches is generally better than trying
        # to do it all in one go, so I believe.
        to_read = buffersize - len(data)
        data += connection.recv(
            4096 if to_read > 4096 else to_read)
    print("data received")
    with open(filename,'wb') as f:
        f.write(data)
    connection.close()
    return
def file_writing_worker_multi(zsize,ysize,xsize,filename,port):
    connection,_ = socks[port-start_port].accept()
    print("connection made")
    start = time.clock()
    chunksize = xsize*ysize*2
    arr = bytearray(chunksize)
    mid = time.clock()
    print("Buffer allocation took: %f seconds" %(mid-start))
    # with open(filename,'wb') as f:
    for i in tqdm(range(zsize)):
        pos = 0
        while pos<chunksize: 
            arr[pos:pos+4096] = connection.recv(4096)
            pos+=4096
            # f.write(arr)
    end = time.clock()
    print("Writing data took: %f seconds" % (end-start))
    return
    
class Worker(Thread):
    """ Thread executing tasks from a given tasks queue """
    def __init__(self, tasks):
        Thread.__init__(self)
        self.tasks = tasks
        self.daemon = True
        self.start()
    def run(self):
        while True:
            func, args, kargs = self.tasks.get()
            try:
                func(*args, **kargs)
            except Exception as e:
                # An exception happened in this thread
                print(e)
            finally:
                # Mark this task as done, whether an exception happened or not
                self.tasks.task_done()
class ThreadPool:
    """ Pool of threads consuming tasks from a queue """
    def __init__(self, num_threads):
        self.tasks = Queue(num_threads)
        for i in range(num_threads):
            Worker(self.tasks)

    def add_task(self, func, *args, **kargs):
        """ Add a task to the queue """
        self.tasks.put((func, args, kargs))

    def map(self, func, args_list):
        """ Add a list of tasks to the queue """
        for args in args_list:
            self.add_task(func, args)

    def wait_completion(self):
        """ Wait for completion of all the tasks in the queue """
        self.tasks.join()
class command_handler:
    def __init__(self,ip,listenerport):
        self.command_queue = Queue()
        self.threadpool = ThreadPool(10)
        self.socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        self.socket.bind((ip,listenerport))
        self.terminate=False
    def listener_start(self):
        self.socket.listen(5)
        while not self.terminate:
            connection,client_address = self.socket.accept()
            print("Connection made with ", client_address)
            self.command_parser(connection)
            
    def command_parser(self,connection):
        message = connection.recv(1024)
        message = message.decode()
        print(message)
        if "Terminate server" in message:
            connection.send("Message received\n".encode())
            connection.close()
            self.threadpool.wait_completion()
            self.socket.close()
            self.terminate=True
        elif "filewritingrequest" in message:
            #message format "file_writing_request zsize ysize xsize filename"
            config = message.split()
            port = port_queue.get()
            print(port)
            self.threadpool.add_task(file_writing_worker_multi,zsize = int(config[1]),ysize = int(config[2]),xsize = int(config[3]),filename = config[4],port = port)
            connection.send(("Connect to %d\n" % port).encode())
        

if __name__== "__main__":
    ch = command_handler('127.0.0.1',20000)
    ch.listener_start()
 #   def resave_data(data,shape=(500,2048,2048),filename = ""):
        # if len(data)!=shape[0]*shape[1]*shape[2]:
        #     print("The file size does not match the length of the array")
        #     return
        # else:
        #     data = np.reshape(data,shape)
        #     imsave(os.path.splitext(filename)+".tif",data)
        #     return
