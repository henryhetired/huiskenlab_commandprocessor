#fileswriting test
import numpy as np 
import time
data = np.zeros(2048*2048*800,dtype=np.uint16)
data = data.tobytes()
print("data generated")
start = time.clock()
f = open("/mnt/smb/Henry-SPIM/test.dat",'wb')
f.write(data)
print("filewritten")
mid = time.clock()
f.close()
end = time.clock()

print("File writing took %f seconds"% (mid-start))
print("File closing took %f seconds"% (end-mid))

