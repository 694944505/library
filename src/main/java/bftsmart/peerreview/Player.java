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
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

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
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawSerializable;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.selector.TimerTask;

public class Player {    
    Logger logger;
    HandleImpl localHandle;
    
    PeerReview<HandleImpl, IdImpl> pr;
    MyIdTL transport;
    public Collection<HandleImpl> witnessed = new ArrayList<HandleImpl>();
    KeyPair pair;    
    X509Certificate cert;
    PeerReviewTransport t1;
    Environment env;
    
//    int id;
    
    PeerReviewService app;
    //HandleImpl destHandle;
    
    public Player(PeerReviewTransport tl, final Environment env2) throws Exception {
      super();
      //this.destHandle = dstHandle;
//      this.id = id;
      this.localHandle = tl.getLocalIdentifier();
      env = env2;//cloneEnvironment(env2, localHandle.name, localHandle.id.id);
      
      this.logger = env.getLogManager().getLogger(Player.class, null);
      
      File f = new File(localHandle.name);
      if (f.exists()) {
        File f2 = new File(f,"peers");
        File[] foo = f2.listFiles();
        if (foo != null) {
          for (int c = 0; c < foo.length; c++) {
            foo[c].delete();
          }
        }
        
        foo = f.listFiles();
        if (foo != null) {
          for (int c = 0; c < foo.length; c++) {
            foo[c].delete();
          }
        }
        
//        System.out.println("Delete "+f+","+f.delete());        
      }
//      File f = new File(name+".data");
//      if (f.exists()) f.delete();
//      f = new File(name+".index");
//      if (f.exists()) f.delete();
      
      //playerTable.put(localHandle, this);
      
      pair = new KeyPair(tl.controller.getStaticConf().getRSAPublicKey(), tl.controller.getStaticConf().getRSAPrivateKey());   
      cert = Material.caTool.sign("Node"+localHandle.id.id,pair.getPublic(), new Date(0), 1000000);
      // store the cert in the keystore

      this.t1 = tl;//getTL();
            
      transport = getIdTransport();
        
      //idTLTable.put(localHandle, transport);
      pr = getPeerReview(this, transport, env);
      tl.setPR(pr);
      
    }
    public void setApp(PeerReviewService app) {
      this.app = app;
      pr.setApp(app);
      
      try {
        System.out.println("======init start"+localHandle.name);
        pr.init(Player.this.localHandle.name);
        System.out.println("===init end");
      } catch (IOException ioe) {
        ioe.printStackTrace();
        
      }
       
      
    }
    public PeerReviewService getApp() {
      return app;
    }
    public IdStrTranslator<IdImpl> getIdStrTranslator() {
        return new IdStrTranslator<IdImpl>(){

            public IdImpl readIdentifierFromString(String s) {
            return new IdImpl(Integer.parseInt(s));
            }

            public String toString(IdImpl id) {
            return Integer.toString(id.id);
            }};
    }
    protected PeerReviewImpl<HandleImpl, IdImpl> getPeerReview(Player player, MyIdTL transport, Environment env) {
        return new PeerReviewImpl<HandleImpl, IdImpl>(transport, env, new HandleSerializer(), new IdSerializer(), new IdExtractor(), getIdStrTranslator()
    //        ,new AuthenticatorSerializerImpl(20,96), new EvidenceSerializerImpl<HandleImpl, IdImpl>(new HandleSerializer(),new IdSerializer(),transport.getHashSizeBytes(),transport.getSignatureSizeBytes())
            );
    }    
    
        
    // to be overridden
    public Environment cloneEnvironment(Environment env2, String name, int id) {
      return env2.cloneEnvironment(name);    
    }


    public void buildPlayerCryptoStuff() {
      
    }

    
    public MyIdTL getIdTransport() throws Exception {
      return new MyIdTL(
        new IdSerializer(), new X509SerializerImpl(),this.localHandle.id,
          cert,pair.getPrivate(),t1,new SHA1HashProvider(),env) {      
      };
    }
    
    
  }
