import os
import numpy as np
def get_size(start_path = '.'):
    size = []
    for dirpath, dirnames, filenames in os.walk(start_path):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            # skip if it is symbolic link
            if not os.path.islink(fp):
                size.append(os.path.getsize(fp))

    return(size)
size = get_size("/mnt/fileserver/Henryspeedtest/09062019/e13/data")
unique,counts = np.unique(size,return_counts=True)
dic = dict(zip(unique,counts))
print(np.max(size)-np.min(size))
print(dic)

