import os
import sys
import re
import argparse
import numpy as np
from datetime import datetime, timedelta

# 遍历当前目录下所有的一级文件夹


def getserver(f):
    thr_pat = re.compile('(.*)Throughput = ([0-9.]*) operations(.*)$')
    lat_pat = re.compile('(.*)Total latency = ([0-9.]*)(.*)$')
    bat_pat = re.compile('(.*)Batch average size = ([0-9.]*)(.*)$')
    thr = []
    lat = []
    bat = []
    for line in f:
        if thr_pat.match(line):
            thr.append(float( thr_pat.match(line).group(2))/1000)
        if lat_pat.match(line):
            lat.append(float( lat_pat.match(line).group(2))/1000)
        if bat_pat.match(line):
            bat.append(float( bat_pat.match(line).group(2)))
        # thr最后一个元素除以bat最后一个元素
       
    avg_thr = []
    avg_lat = 0
    #print(f.name)
    #print (thr,lat,bat)
    for i in range(len(thr)):
        avg_thr.append([thr[i], lat[i], bat[i]])

    # avg_lat排序按照第一个元素排序
    #avg_thr.sort(key=lambda x:x[0])
    # 统计去掉前后20%数据后的平均值
    avg_thr = np.array(avg_thr)
    #数组长度
    if(len(avg_thr) == 0):
        return np.array([0,0,0])
    avg_thr = avg_thr[1:-1]

    avg_thr = np.mean(avg_thr, axis=0)
    return avg_thr
    
    
#Average time for 10000 executions (all samples) = 5725.4365414 us 
def getclient(f):
    pat = re.compile('(.*) Average time (.*)all samples(.*)= ([0-9.]*)(.*)$')
    for line in f:
        if pat.match(line):
            return float(pat.match(line).group(4))/1000
    return 0
#f = open("r8f10/remote/5/log/stdout")
#getserver(f)
#f = open("r4f100c/remote/0/log/stdout")
#getclient(f)


for dirc in ['r16pr','r4pr3']:
    # 判断是否为文件夹
    if os.path.isdir(dirc) and dirc[0] == 'r' :
        # 判断文件夹是否包含指定文件
        if dirc[-1] != 'c':
            num = 4 if dirc[1] == '4' else (8 if dirc[1] == '8' else 16)
            thr_lat = []
            lat = []
            for i in range(num):
                if os.path.exists(dirc + '/remote/' + str(i)+'/log/stdout'):
                    #打开文件
                    f = open(dirc + '/remote/' + str(i)+'/log/stdout', 'r')
                    res = getserver(f)
                    if (res[0]>0):
                        thr_lat.append(res)
                else :
                    print(dirc + '/remote/' + str(i)+'/log/stdout' + ' not exist')
                if os.path.exists(dirc + 'c/remote/' + str(i)+'/log/stdout'):
                    fc = open(dirc + 'c/remote/' + str(i)+'/log/stdout', 'r')
                    lat.append(getclient(fc))
            thr_lat_avg = np.mean(thr_lat, axis=0)
            lat_avg = np.mean(lat)
            print(dirc, thr_lat_avg, lat_avg)
                
    18.198361 17.27199765691
    4.74924  67.13022079076999