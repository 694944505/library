cd build/install/library
#!/bin/bash
rep=({0..0})
if [[ $# -gt 0 ]]; then
    rep=($@)
fi

# avoid stack overflow as in our simple demo the bootstrapping replica will
# potentially have a long chain of promise resolution
ulimit -s unlimited
# ./smartrun.cmd bftsmart.demo.microbenchmarks.ThroughputLatencyClient  ${i} 1 2 0 0 false false nosig > log${i} 2>&1 &
for i in "${rep[@]}"; do
    echo "starting client $i"
    #valgrind --leak-check=full ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    #gdb -ex r -ex bt -ex q --args ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyClient  ${i} 2 3000 0 0 false false nosig > logcl${i} 2>&1 &
                                                                    #id threads ops reqsize interval
done
wait
