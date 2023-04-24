package shield.proxy.trx.data;

public class Write {


  public boolean isWrite() {
    return ty == Type.WRITE;
  }

  public boolean isDelete() {
    return ty == Type.DELETE;
  }

  public enum Type {
    WRITE,
    DELETE
  }

  private Long key;
  private Type ty;
  private byte[] value;

  public Write(Long key, byte[] value, Type ty) {
      this.key = key;
      this.ty = ty;
      this.value = value;
  }


  public Type getTy() {
    return ty;
  }

  public Long getKey() {
    return key;
  }

  public byte[] getValue() {
    return value;
  }


}
