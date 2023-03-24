package bftsmart.peerreview;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.mpisws.p2p.pki.x509.CATool;
import org.mpisws.p2p.pki.x509.CAToolImpl;
import org.mpisws.p2p.pki.x509.X509Serializer;
import org.mpisws.p2p.pki.x509.X509SerializerImpl;
import org.mpisws.p2p.transport.ErrorHandler;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.peerreview.IdentifierExtractor;
import org.mpisws.p2p.transport.peerreview.PeerReview;
import org.mpisws.p2p.transport.peerreview.PeerReviewCallback;
import org.mpisws.p2p.transport.peerreview.PeerReviewImpl;
import org.mpisws.p2p.transport.peerreview.WitnessListener;
import org.mpisws.p2p.transport.peerreview.commitment.Authenticator;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorSerializer;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorSerializerImpl;
import org.mpisws.p2p.transport.peerreview.commitment.AuthenticatorStore;
import org.mpisws.p2p.transport.peerreview.commitment.CommitmentProtocol;
import org.mpisws.p2p.transport.peerreview.commitment.CommitmentProtocolImpl;
import org.mpisws.p2p.transport.peerreview.evidence.EvidenceSerializerImpl;
import org.mpisws.p2p.transport.peerreview.history.HashProvider;
import org.mpisws.p2p.transport.peerreview.history.SecureHistory;
import org.mpisws.p2p.transport.peerreview.history.SecureHistoryFactory;
import org.mpisws.p2p.transport.peerreview.history.SecureHistoryFactoryImpl;
import org.mpisws.p2p.transport.peerreview.history.hasher.SHA1HashProvider;
import org.mpisws.p2p.transport.peerreview.history.stub.NullHashProvider;
import org.mpisws.p2p.transport.peerreview.identity.IdentityTransport;
import org.mpisws.p2p.transport.peerreview.identity.IdentityTransportCallback;
import org.mpisws.p2p.transport.peerreview.identity.IdentityTransportLayerImpl;
import org.mpisws.p2p.transport.peerreview.identity.UnknownCertificateException;
import org.mpisws.p2p.transport.peerreview.infostore.Evidence;
import org.mpisws.p2p.transport.peerreview.infostore.IdStrTranslator;
import org.mpisws.p2p.transport.peerreview.infostore.PeerInfoStore;
import org.mpisws.p2p.transport.peerreview.infostore.StatusChangeListener;
import org.mpisws.p2p.transport.peerreview.message.PeerReviewMessage;
import org.mpisws.p2p.transport.peerreview.replay.Verifier;
import org.mpisws.p2p.transport.peerreview.replay.record.RecordLayer;
import org.mpisws.p2p.transport.table.UnknownValueException;
import org.mpisws.p2p.transport.util.MessageRequestHandleImpl;
import org.mpisws.p2p.transport.util.Serializer;

import rice.Continuation;
import rice.environment.Environment;
// import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawSerializable;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.selector.TimerTask;


import bftsmart.communication.server.ServersCommunicationLayer;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.communication.SystemMessage;
public class PeerReviewTransport extends ServersCommunicationLayer implements TransportLayer<HandleImpl, ByteBuffer> {
    private HandleImpl localIdentifier;
    private Environment env;
    private PeerReview<HandleImpl, IdImpl> pr;
    private Map<Integer, HandleImpl> idMap = new HashMap<Integer, HandleImpl>();
    private Player player;
    private int num=0;
    private Vector<Map<String,Object>> options = new Vector<Map<String,Object>>(){{add(null);add(new HashMap(){{put(PeerReview.DONT_COMMIT, new Object());}});}};
    public PeerReviewTransport(ServerViewController controller,
            LinkedBlockingQueue<SystemMessage> inQueue, 
            LinkedBlockingQueue<Pair<HandleImpl, ByteBuffer>> prQueue,
            ServiceReplica replica, Environment env, boolean isPRcopy) throws Exception {
        super(controller, inQueue, prQueue, replica, isPRcopy);
        this.env = env;
        localIdentifier = new HandleImpl(controller.getStaticConf().getProcessId());
        player = new Player(this, env);   
        PeerReviewService service = new PeerReviewService(replica, this);
        player.setApp(service);
        setCallback(service); 
    }
    public void acceptMessages(boolean b) {
        throw new RuntimeException("implement");
    }
  
    public void acceptSockets(boolean b) {
        throw new RuntimeException("implement");
    }
  
    public HandleImpl getLocalIdentifier() {
        return localIdentifier;
    }
    public SocketRequestHandle<HandleImpl> openSocket(HandleImpl i,
        SocketCallback<HandleImpl> deliverSocketToMe,
        Map<String, Object> options) {
      //throw new RuntimeException("implement");
        System.out.println("openSocket"+i);
        return null;
    }

    public MessageRequestHandle<HandleImpl, ByteBuffer> sendMessage(
        HandleImpl i, ByteBuffer m,
        MessageCallback<HandleImpl, ByteBuffer> deliverAckToMe,
        Map<String, Object> options) {    
      int target = i.id.id;  
      try {
          if(target == me) {
              // sm.authenticated = true;
              prQueue.put(Pair.of(getHandle(target), m));
              logger.debug("Queueing (delivering) my own message, me:{}", target);
          }
          else{

              //System.out.println("sending to "+target);
              byte[] data = new byte[m.remaining()];
              System.arraycopy(m.array(), m.position(), data, 0, data.length);
              getConnection(target).send(data);
          }
      } catch (InterruptedException ex) {
        logger.error("Interruption while inserting message into inqueue", ex);
      }
      return null;
    }
    private HandleImpl getHandle(int id) {
      HandleImpl handle = idMap.get(id);
      if (handle == null) {
        handle = new HandleImpl(id);
        idMap.put(id, handle);
      }
      return handle;
    }
    @Override
    public void send(int[] targets, SystemMessage sm, boolean useMAC) {
      //System.out.println("sending to with"+ Arrays.toString(targets)+" "+ sm.toString());
      ByteArrayOutputStream bOut = new ByteArrayOutputStream(248);
      try {
          new ObjectOutputStream(bOut).writeObject(sm);
      } catch (IOException ex) {
          logger.error("Failed to serialize message", ex);
      }

      byte[] messageData = bOut.toByteArray();
      
      
      // this shuffling is done to prevent the replica with the lowest ID/index  from being always
      // the last one receiving the messages, which can result in that replica  to become consistently
      // delayed in relation to the others.
      /*Tulio A. Ribeiro*/
      Integer[] targetsShuffled = Arrays.stream( targets ).boxed().toArray( Integer[]::new );
      Collections.shuffle(Arrays.asList(targetsShuffled), new Random(System.nanoTime())); 
      for (int target : targetsShuffled) {
        
        ByteBuffer buf = ByteBuffer.wrap(messageData);
        pr.sendMessage(getHandle(target), buf, null, null );
        pr.sendMessage(getHandle(target), buf, null, options.get(1) );
        logger.debug("Sending message from:{} -> to:{}.", me,  target);
      }
    }

    TransportLayerCallback<HandleImpl, ByteBuffer> callback;

    public void setCallback(
        TransportLayerCallback<HandleImpl, ByteBuffer> callback) {
      this.callback = callback;
    }

    public void setErrorHandler(ErrorHandler<HandleImpl> handler) {
      throw new RuntimeException("implement");
    }

    public void destroy() {
      throw new RuntimeException("implement");
    }
    public void setPR(PeerReview<HandleImpl, IdImpl> pr){
      this.pr = pr;
    }
    public PeerReview<HandleImpl, IdImpl> getPR(){
      return pr;
    }
    public Environment getEnv(){
      return env;
    }
    public Player getPlayer(){
      return player;
    }
    
}