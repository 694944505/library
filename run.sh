cd build/install/library
#!/bin/bash
rep=({0..3})
if [[ $# -gt 0 ]]; then
    rep=($@)
fi

# avoid stack overflow as in our simple demo the bootstrapping replica will
# potentially have a long chain of promise resolution
# ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyServer 0 50000 0 0 false nosig > log0 2>&1
# ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyServer 1 50000 0 0 false nosig > log1 2>&1
# ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyServer 2 50000 0 0 false nosig > log2 2>&1
# ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyServer 3 50000 0 0 false nosig > log3 2>&1

ulimit -s unlimited

for i in "${rep[@]}"; do
    echo "starting replica $i"
    #valgrind --leak-check=full ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    #gdb -ex r -ex bt -ex q --args ./examples/hotstuff-app --conf hotstuff-sec${i}.conf > log${i} 2>&1 &
    ./smartrun.sh bftsmart.demo.microbenchmarks.ThroughputLatencyServer ${i} 100 0 0 true nosig> log${i} 2>&1 &
done
wait
#$SERVER_NUMBER 50 100 100 1