LIB_DIR=libraries
JARS=$LIB_DIR/commons-crypto-1.0.0.jar:$LIB_DIR/json-simple-1.1.1.jar:$LIB_DIR/json-simple-1.1.jar:$LIB_DIR/mapdb-3.1.0-SNAPSHOT.jar:$LIB_DIR/mockito-all-2.0.2-beta.jar:$LIB_DIR/netty-3.2.0.Final.jar:$LIB_DIR/netty-all-4.1.9.Final.jar:$LIB_DIR/netty.jar:$LIB_DIR/protobuf.jar:$LIB_DIR/protobuf-java-2.5.0.jar:$LIB_DIR/protobuf-java-2.5.0-javadoc.jar
OUT_DIR=out/production/shield
PREFIX=benchmarks/cs6410

for i in `ls $PREFIX`; do
echo "Running microbenchmark for config $i";
java -classpath $OUT_DIR:$JARS shield.benchmarks.micro.BackingStoreMicrobenchmark $PREFIX/$i;
done
