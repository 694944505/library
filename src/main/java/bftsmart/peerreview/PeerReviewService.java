package bftsmart.peerreview;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
import java.util.Map.Entry;

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

import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.Executable;
import bftsmart.tom.server.Recoverable;
import bftsmart.communication.SystemMessage;
import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawSerializable;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.selector.TimerTask;

public class PeerReviewService implements PeerReviewCallback<HandleImpl, IdImpl> {

    Logger logger;
    Player player;
    Environment env;
    Random rand;
    long nextSendTime = 0;
    //private HandleImpl dest;
    private ServiceReplica replica;
    private ServiceReplica replica_copy = null;
    private PeerReviewTransport tl;
    
    public PeerReviewService(ServiceReplica replica, PeerReviewTransport tl) throws Exception {
      super();
      this.replica = replica;
      this.tl = tl;

      this.env = tl.getEnv();
      this.logger = env.getLogManager().getLogger(PeerReviewService.class, null);
      logger.log("new bogus app "+player);
      //dest = player.destHandle;
      //tl.setCallback(this); 

      player = tl.getPlayer(); 
      //player.setApp(this);
    }
    

    public void init() {
     
    }
    
    
    
    public void storeCheckpoint(OutputBuffer buffer) throws IOException {
      buffer.writeInt(31173);
    }

    public boolean loadCheckpoint(InputBuffer buffer) throws IOException {
      buffer.readInt();
      return true;
    }
    
    public void destroy() {
      throw new RuntimeException("implement");
    }

    public void notifyCertificateAvailable(IdImpl id) {
      throw new RuntimeException("implement");
    }

    public void receive(HandleImpl source, boolean datagram, ByteBuffer msg) {
      throw new RuntimeException("implement");
    }

    public void sendComplete(long id) {
      throw new RuntimeException("implement");
    }

    public void incomingSocket(P2PSocket<HandleImpl> s) throws IOException {
      throw new RuntimeException("implement");
    }

    public void messageReceived(HandleImpl i, ByteBuffer message,
        Map<String, Object> options) throws IOException {
      // read data length
      
      // -1 because we don't want to read the MAC
      byte[] data = new byte[message.remaining()];
      System.arraycopy(message.array(), message.position(), data, 0, data.length);
      //logger.trace("Read: {}, HasMAC: {}", read, hasMAC);
      try {
        SystemMessage sm = (SystemMessage) (new ObjectInputStream(new ByteArrayInputStream(data))
								.readObject());
        sm.authenticated = true;
        replica.getServerCommunicationSystem().getMessageHandler().processData(sm);
      } catch (ClassNotFoundException ex) {
        //logger.log("Failed to deserialize message", ex);
      } 
      
    }

    public void getWitnesses(IdImpl subject,
        WitnessListener<HandleImpl, IdImpl> callback) {
      callback.notifyWitnessSet(subject, replica.getSVController().getCurrentView().getWitnesses(subject.id));
    }

    public void notifyStatusChange(
        IdImpl id,
        int newStatus) {
      if (logger.level <= Logger.INFO) logger.log("notifyStatusChange("+id+","+PeerReviewImpl.getStatusString(newStatus)+")");
      if (newStatus != STATUS_TRUSTED) {
        logger.log("Failure, Node not trusted: "+id+" at "+player.localHandle);
        //System.exit(1);
      }
      //TODO
      //addStatusNotification(this.player.localHandle,id,newStatus);
    }

    public Collection<HandleImpl> getMyWitnessedNodes() {
      return replica.getSVController().getCurrentView().getWitnesses();
    }

    public PeerReviewCallback<HandleImpl, IdImpl> getReplayInstance(Verifier<HandleImpl> v) {
      try {
        return null;
        /*if(replica_copy == null) {
          replica_copy = new ServiceReplica(replica.getSVController().getStaticConf().getProcessId(), replica.getExecutor(), replica.getRecoverer(), true);
        }
        return replica_copy.getServerCommunicationSystem().getPRServersConn().getPlayer().getApp();*/
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
