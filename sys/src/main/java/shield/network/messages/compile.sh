protoc -I=. --java_out=. Msg.proto
cp shield/network/messages/Msg.java .
rm -r shield/
