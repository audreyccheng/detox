package shield.proxy;

import static shield.network.messages.Msg.Statement.Type.WRITE;
import static shield.proxy.oram.ORAMConfig.clientKey;

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.prng.X931SecureRandomBuilder;
import org.bouncycastle.pqc.math.linearalgebra.Permutation;
import org.json.simple.parser.ParseException;
import shield.BaseNode;
import shield.network.messages.Msg.ClientMessageReq;
import shield.network.messages.Msg.ClientMessageResp;
import shield.network.messages.Msg.ClientMessageResp.RespType;
import shield.network.messages.Msg.Message;
import shield.network.messages.Msg.Statement;
import shield.proxy.data.async.IAsyncBackingStore.BackingStoreType;
import shield.util.Logging;
import com.google.common.primitives.Ints;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Proxy class used for benchmarking: does nothing and simply replies "ok" to the appropriate
 * message type.
 *
 * It is currently used to measure the max throughput of our networking layer
 *
 * @author ncrooks
 */
public final class EchoShuffleProxy extends BaseNode {

  class EncryptionTools {
     byte[] clientKey;
     byte[] nonce;
    SecureRandom rng;
    Lock lock = new ReentrantLock();
    ArrayList<Integer> indices;

    EncryptionTools() throws NoSuchAlgorithmException {
      count = 0;
      clientKey = new byte[32];
      nonce = new byte[32];
      new Random().nextBytes(clientKey);
      // System.out.println(rng.getProvider());
       rng = SecureRandom.getInstance("SHA1PRNG");
       // rng = new X931SecureRandomBuilder().build(new AESFastEngine(),
       //   new KeyParameter(seed), false);
     }
  }

  byte[] value;
  int count = 0;
  ConcurrentHashMap<Long, EncryptionTools> clientEncryptionData;
  ConcurrentHashMap<Long, InetSocketAddress> clientMap;
  SecureRandom blockingRng = new SecureRandom();
  byte[] seed ;
  /**
   * For testing only
   */
  public EchoShuffleProxy() throws InterruptedException, NoSuchAlgorithmException {
    super();
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
    seed = blockingRng.generateSeed(32);
  }

  public EchoShuffleProxy(String configFileName)
      throws InterruptedException, IOException, ParseException, NoSuchAlgorithmException {
    super(configFileName);
    value = new byte[config.ORAM_VALUE_SIZE];
    new Random().nextBytes(value);
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
    seed = blockingRng.generateSeed(32);
    }

/**
   * For testing only
   */
  public EchoShuffleProxy(String addr, int port)
      throws InterruptedException, NoSuchAlgorithmException {
    super(addr, port);
    value = new byte[config.ORAM_VALUE_SIZE];
    clientKey = new byte[32];
    new Random().nextBytes(value);
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
    seed = blockingRng.generateSeed(32);
    }

  public EchoShuffleProxy(String addr, int port, BackingStoreType ty)
      throws InterruptedException, NoSuchAlgorithmException {
    super(addr, port);
    getConfig().BACKING_STORE_TYPE = ty;
    value = new byte[config.ORAM_VALUE_SIZE];
    clientKey = new byte[32];
    clientEncryptionData = new ConcurrentHashMap<Long,EncryptionTools>(100,0.9f, 32);
    clientMap = new ConcurrentHashMap<>(100,0.9f,32);
    seed = blockingRng.generateSeed(32);
    }

  @Override
  public void handleMsg(Message msg) {

    Message.Type ty;

    ty = msg.getMessageType();

    switch (ty) {
      case ClientReqMessage:
        Integer result = encrypt(msg.getClientReqMsg());
        sendEchoResponse(msg.getClientReqMsg(), Ints.toByteArray(result));
        break;
      default:
        logErr("Unrecognised message type " + ty, Logging.Level.CRITICAL);
        System.exit(-1);
    }

  }


  private int encrypt(ClientMessageReq req) {
    int result = 0;
    EncryptionTools enc = clientEncryptionData.computeIfAbsent(req.getClientId(), x -> {
      try {
        return new EncryptionTools();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
      return null;
    });
    // enc.lock.lock();
        for (Statement op: req.getOperationsList()) {
        if (op.getOpType() == WRITE) {
            //TODO(natacha): specify option to change crypto in NodeConfig
          // result =  shuffleUsingJava(enc);
          result = shuffleUsingBC(enc);
         }
    }
  //  enc.lock.unlock();
    return result;

  }

  private int shuffleUsingJava(EncryptionTools enc) {
      // ArrayList<Integer> index = (ArrayList<Integer>) enc.indices.clone();
      Collections.shuffle(enc.indices, enc.rng);
      return enc.indices.get(0);
  }

  private int shuffleUsingBC(EncryptionTools enc) {
      Permutation p = new Permutation(298, enc.rng);
      int[] v  = p.getVector();
      assert (v.length == 298);
      return v[0];
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
