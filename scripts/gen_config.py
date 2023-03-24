import os, re
import subprocess
import itertools
import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Generate configuration file for a batch of replicas')
    parser.add_argument('--ips', type=str, default=None)
    base_pport = 11000
    base_cport = 12000
    args = parser.parse_args()
    if args.ips is None:
        ips = ['127.0.0.1']
    else:
        ips = [l.strip() for l in open(args.ips, 'r').readlines()]
    replicas = []
    port_count = {}
    index = 0
    for ip in ips:
        i = port_count.setdefault(ip, 0)
        port_count[ip] += 1
        replicas.append("{} {} {} {}\n".format(index,ip, base_pport + i, base_cport + i))
        index = index + 1
    main_conf = open("host_config.conf", 'w')
    for rep in replicas:
        main_conf.write(rep)