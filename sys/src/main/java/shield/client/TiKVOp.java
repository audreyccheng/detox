package shield.client;

import org.tikv.shade.com.google.protobuf.ByteString;
import org.tikv.txn.TwoPhaseCommitter;

public class TiKVOp {
    public ByteString key;
    public byte[] value;
    public boolean is_read_op;
    public boolean is_read_for_update = false;
    public byte[] result; // not used
    public long version; // not used anymore (timestamp taken a time of prewrite and commit separately)

    // for use when committing the txn (if it is a write)
    public TwoPhaseCommitter twoPC;

    public TiKVOp(ByteString key, byte[] value, boolean is_read_op, byte[] result) {
        this.key = key;
        this.value = value;
        this.is_read_op = is_read_op;
        this.result = result;
    }
}
