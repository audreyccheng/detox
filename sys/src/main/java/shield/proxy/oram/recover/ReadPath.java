package shield.proxy.oram.recover;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReadPath implements LogEntry {

  private Long key;

  private Integer oldPath;

  private Integer newPath;

  private List<Integer> indicesAccessed;

  private boolean dummy;

  public ReadPath(Long key, Integer oldPath, Integer newPath, int treeLevels, boolean dummy) {
    this.key = key;
    this.oldPath = oldPath;
    this.newPath = newPath;
    this.indicesAccessed = new ArrayList<>(treeLevels);
    this.dummy = dummy;
  }
  public Long getKey() {
    return key;
  }

  public Integer getOldPath() {
    return oldPath;
  }

  public Integer getNewPath() {
    return newPath;
  }

  public boolean isDummy() {
    return dummy;
  }

  public List<Integer> getIndicesAccessed() {
    return indicesAccessed;
  }

  public void addIndexAccessed(Integer index) {
    indicesAccessed.add(index);
  }

  public void addIndicesAccessed(List<Integer> indices) {
    indicesAccessed.addAll(indices);
  }

  @Override
  public byte[] serialize() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(Integer.BYTES + Integer.BYTES);
      DataOutputStream dos = new DataOutputStream(bos);
      try {
        dos.writeLong(key);
        dos.writeInt(oldPath);
        dos.writeInt(newPath);
        dos.writeByte((dummy) ? 1 : 0);
        for (int i = 0; i < indicesAccessed.size(); ++i) {
          dos.writeInt(indicesAccessed.get(i));
        }
      } catch (IOException e) {
        System.err.printf("Unexpected IOException occurred:\n");
        e.printStackTrace(System.err);
        System.exit(1);
      } catch (NullPointerException e) {
        e.printStackTrace();
        System.exit(1);
      }
      return bos.toByteArray();
    }

  public static ReadPath deserialize(byte[] b, int treeLevels) {
    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
    try {
      ReadPath rp = new ReadPath(dis.readLong(), dis.readInt(), dis.readInt(), treeLevels, dis.readByte() == 1);
      for (int i = 0; i < treeLevels; ++i) {
        rp.addIndexAccessed(dis.readInt());
      }
      return rp;
    } catch (IOException e) {
      System.err.printf("Unexpected IOException occurred:\n");
      e.printStackTrace(System.err);
      System.exit(1);
    }
    return null;
  }
}
