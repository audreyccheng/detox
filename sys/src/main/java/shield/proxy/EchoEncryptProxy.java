package shield.proxy;

import static shield.network.messages.Msg.Statement.Type.WRITE;
import static shield.proxy.oram.ORAMConfig.clientKey;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.ClientMessageResp.RespType;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Statement;
import shield.util.Logging;
import com.google.common.primitives.Ints;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Proxy class used for benchmarking: does nothing and simply replies "ok" to the appropriate
 * message type.
 *
 * It is currently used to measure the max throughput of our networking layer
 *
 * @author ncrooks
 */
public final class EchoEncryptProxy extends BaseNode {

  class EncryptionTools {
     byte[] clientKey;
     byte[] nonce;
    // SecureRandom rng;
    Random rng;
    MessageDigest shaJavaSec;
    MessageDigest shaApacheCodec;
    Digest shaBouncyCastle = new SHA256Digest();
    HMac hMac = new HMac(shaBouncyCastle);
    Lock lock = new ReentrantLock();
    public int count  = 0 ;
    byte[] fixedMac;

    EncryptionTools() throws NoSuchAlgorithmException {
      clientKey = new byte[32];
      nonce = new byte[32];
      fixedMac = new byte[1000];
      new Random().nextBytes(clientKey);
      new Random().nextBytes(fixedMac);
      shaJavaSec = MessageDigest.getInstance("SHA-256");
      shaApacheCodec = DigestUtils.getSha256Digest();
      KeyParameter param = new KeyParameter(clientKey);
      hMac.init(param);
      // rng = new SecureRandom();
      rng = new Random();
      rng.nextBytes(nonce);
     }
  }

  byte[] value;
  ConcurrentHashMap<Long, EncryptionTools> clientEncryptionData;
  ConcurrentHashMap<Long, InetSocketAddress> clientMap;

  /**
   * For testing only
   */
  public EchoEncryptProxy() throws InterruptedException, NoSuchAlgorithmException {
    super();
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
  }

  public EchoEncryptProxy(String configFileName)
      throws InterruptedException, IOException, ParseException, NoSuchAlgorithmException {
    super(configFileName);
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
   }


  @Override
  public void handleMsg(Message msg) {

    Message.Type ty;

    ty = msg.getMessageType();

    switch (ty) {
      case ClientReqMessage:
        byte[] encryptedValue = encrypt(msg.getClientReqMsg());
        sendEchoResponse(msg.getClientReqMsg(), encryptedValue);
        break;
      default:
        logErr("Unrecognised message type " + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }

  }


  private byte[] encrypt(ClientMessageReq req) {
    byte[] msg = null;
    EncryptionTools enc = clientEncryptionData.computeIfAbsent(req.getClientId(), x -> {
      try {
        return new EncryptionTools();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
      return null;
    });
    enc.lock.lock();
    for (Statement op: req.getOperationsList()) {
        if (op.getOpType() == WRITE) {
            msg = op.getValue().toByteArray();
            //TODO(natacha): specify option to change crypto in NodeConfig
      //      encryptUsingJavaSec(msg, enc);
      //      encryptUsingApacheCodec(msg, enc);
       //      encryptUsingBC(msg, enc);
            encryptUsingBCHmac(msg, enc);
           }
    }
    enc.lock.unlock();
    return msg;

  }

  private void encryptUsingJavaSec(byte[] msg, EncryptionTools enc) {

    byte[] mask = new byte[msg.length];
    int i = 0;
    for (; i + enc.shaJavaSec.getDigestLength() < msg.length; i += enc.shaJavaSec.getDigestLength()) {
      enc.shaJavaSec.update(enc.clientKey);
      enc.shaJavaSec.update(enc.nonce);
      enc.shaJavaSec.update(Ints.toByteArray(i));
      System.arraycopy(enc.shaJavaSec.digest(), 0, mask, i, enc.shaJavaSec.getDigestLength());
    }
    enc.shaJavaSec.update(enc.clientKey);
    enc.shaJavaSec.update(enc.nonce);
    enc.shaJavaSec.update(Ints.toByteArray(i));
    System.arraycopy(enc.shaJavaSec.digest(), 0, mask, i, mask.length - i);
    for (int j = 0; j < msg.length; ++j) {
      msg[j] ^= mask[j];
    }
    enc.rng.nextBytes(enc.nonce);
  }

  private void encryptUsingApacheCodec(byte[] msg, EncryptionTools enc) {
    byte[] mask = new byte[msg.length];
    int i = 0;
    for (; i + enc.shaApacheCodec.getDigestLength() < msg.length; i += enc.shaApacheCodec.getDigestLength()) {
      enc.shaApacheCodec.update(clientKey);
      enc.shaApacheCodec.update(enc.nonce);
      enc.shaApacheCodec.update(Ints.toByteArray(i));
      System.arraycopy(enc.shaApacheCodec.digest(), 0, mask, i, enc.shaApacheCodec.getDigestLength());
    }
    enc.shaApacheCodec.update(clientKey);
    enc.shaApacheCodec.update(enc.nonce);
    enc.shaApacheCodec.update(Ints.toByteArray(i));
    System.arraycopy(enc.shaApacheCodec.digest(), 0, mask, i, mask.length - i);
    for (int j = 0; j < msg.length; ++j) {
      msg[j] ^= mask[j];
    }
    enc.rng.nextBytes(enc.nonce);
   }


  public void encryptUsingBC(byte[] msg, EncryptionTools enc) {

    byte[] mask = new byte[msg.length];
    int i;
    for (i =0 ; i + enc.shaBouncyCastle.getDigestSize() < msg.length; i += enc.shaBouncyCastle.getDigestSize()) {
      enc.shaBouncyCastle.update(clientKey, 0, clientKey.length);
      enc.shaBouncyCastle.update(enc.nonce, 0, enc.nonce.length);
      enc.shaBouncyCastle.update(Ints.toByteArray(i), 0, Ints.BYTES);
      enc.shaBouncyCastle.doFinal(mask,i);
    }
    enc.shaBouncyCastle.update(clientKey, 0, clientKey.length);
    enc.shaBouncyCastle.update(enc.nonce, 0, enc.nonce.length);
    enc.shaBouncyCastle.update(Ints.toByteArray(i), 0, Ints.BYTES);
    enc.shaBouncyCastle.doFinal(mask, i);
    for (int j = 0; j < msg.length; ++j) {
      msg[j] ^= mask[j];
    }
    enc.rng.nextBytes(enc.nonce);
   }


  public void encryptUsingBCHmac(byte[] msg, EncryptionTools enc) {

    byte[] mask = new byte[msg.length];

    generateMask(mask,enc);
    doXor(mask,msg);
    enc.nonce = Ints.toByteArray(enc.count++);
   }

   private void doXor(byte[] mask, byte[] msg) {
    for (int j = 0; j < msg.length; ++j) {
      msg[j] ^= mask[j];
    }
   }

   private void generateMask(byte[] mask, EncryptionTools enc) {

    int i = 0;

    for (; i + enc.hMac.getMacSize() < mask.length; i += enc.hMac.getMacSize()) {
      enc.hMac.update(enc.nonce, 0, enc.nonce.length);
      enc.hMac.update(Ints.toByteArray(i), 0, Ints.BYTES);
      enc.hMac.doFinal(mask,i);
    }
    enc.hMac.update(enc.nonce, 0, enc.nonce.length);
    enc.hMac.update(Ints.toByteArray(i), 0, Ints.BYTES);
    enc.hMac.doFinal(mask,mask.length -i);

    }


  private void sendEchoResponse(ClientMessageReq clientReqMsg, byte[] result) {
    if (clientReqMsg.hasRegister()) {
      ClientMessageResp.Builder clientMsg;
      Message.Builder respMsg;
      InetSocketAddress addr =
          new InetSocketAddress(clientReqMsg.getClientHost(),
              clientReqMsg.getClientPort());
      clientMap.put(clientReqMsg.getClientId(), addr);
      clientMsg = ClientMessageResp.newBuilder();
      clientMsg.setRespType(RespType.REGISTER);
      clientMsg.setIsError(false);
      respMsg = Message.newBuilder();
      respMsg.setClientRespMsg(clientMsg);
      respMsg.setMessageType(Message.Type.ClientRespMessage);
      sendMsg(respMsg.build(), addr, true);
      } else {
      ClientMessageResp resp
          = ClientMessageResp.newBuilder().setIsError(false).addReadValues(ByteString.copyFrom(result)).setRespType(RespType.OPERATION).build();
      Message rsp = Message.newBuilder().setClientRespMsg(resp).setMessageType(
          Message.Type.ClientRespMessage).build();
      sendMsg(rsp, clientMap.get(clientReqMsg.getClientId()));
    }
  }

  public void startProxy() {
  }

  /*
  public void mask(byte[] value) {
    assert value != null;
    //apply mask to value in-place
    if (encrypt) {
      if (mask == null) {
        mask = new byte[value.length];
        sha1.reset();
        sha1.update(ORAMConfig.clientKey);
        sha1.update(nonce);
        int i = 0;
        for (; i + SHA1_DIGEST_LENGTH < value.length; i += SHA1_DIGEST_LENGTH) {
          System.arraycopy(sha1.digest(), 0, mask, i, SHA1_DIGEST_LENGTH);
          sha1.update(nonce);
        }
        System.arraycopy(sha1.digest(), 0, mask, i, mask.length - i);
      }
      assert mask.length == value.length;
      for (int j = 0; j < value.length; ++j) {
        value[j] ^= mask[j];
      }
    }
  } */

}
