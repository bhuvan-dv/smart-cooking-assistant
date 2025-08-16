
# Node 1
: > node1.log && mvn exec:java -Dexec.args="25251 node1 seed" 2>&1 | tee -a node1.log
#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
# Node 2 (in another terminal)
sleep 5; : > node2.log && mvn exec:java -Dexec.args="25252 node2" 2>&1 | tee -a node2.log
: > interactive.log && mvn exec:java -Dexec.args="interactive" 2&1 | tee -a interactive.log
: > testinmain.log && mvn exec:java -Dexec.args="akka-tests" 2>&1 | tee -a testinmain.log
: > akkatests.log && mvn exec:java -Dexec.args="akka-tests" 2>&1 | tee -a akkatests.log
mvn exec:java -Dexec.args="akka-tests"
mvn exec:java -Dexec.args="test"
mvn exec:java -Dexec.args="interactive"