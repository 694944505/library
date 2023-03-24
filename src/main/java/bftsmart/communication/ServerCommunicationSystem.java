/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package bftsmart.communication;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.CommunicationSystemServerSideFactory;
import bftsmart.communication.client.RequestReceiver;
import bftsmart.communication.server.ServersCommunicationLayer;
import bftsmart.consensus.roles.Acceptor;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.TOMLayer;
import bftsmart.tom.core.messages.TOMMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.peerreview.Material;

import bftsmart.peerreview.HandleImpl;
import bftsmart.peerreview.IdImpl;
import bftsmart.peerreview.PeerReviewTransport;
import bftsmart.peerreview.Player;

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
//import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.commonapi.rawserialization.OutputBuffer;
import rice.p2p.commonapi.rawserialization.RawSerializable;
import rice.p2p.util.MathUtils;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.selector.TimerTask;
/**
 *
 * @author alysson
 */
public class ServerCommunicationSystem extends Thread {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean doWork = true;
    public final long MESSAGE_WAIT_TIME = 100;
    private LinkedBlockingQueue<SystemMessage> inQueue = null;//new LinkedBlockingQueue<SystemMessage>(IN_QUEUE_SIZE);
    private LinkedBlockingQueue<Pair<HandleImpl, ByteBuffer>> prQueue = null;
    protected MessageHandler messageHandler;
    
    private ServersCommunicationLayer serversConn;
    private PeerReview<HandleImpl, IdImpl> pr;
    private CommunicationSystemServerSide clientsConn;
    private ServerViewController controller;
    private boolean isPRcopy;

    /**
     * Creates a new instance of ServerCommunicationSystem
     */
    public ServerCommunicationSystem(ServerViewController controller, ServiceReplica replica, boolean isPRcopy)
            throws Exception {
        super("Server Comm. System");

        this.controller = controller;
        this.isPRcopy = isPRcopy;

        messageHandler = new MessageHandler();

        inQueue = new LinkedBlockingQueue<SystemMessage>(controller.getStaticConf().getInQueueSize());
        prQueue = new LinkedBlockingQueue<Pair<HandleImpl, ByteBuffer>>(controller.getStaticConf().getInQueueSize());
        if(controller.getStaticConf().peerreviewEnabled()) {
            // Instantiate a new PeerReviewTransport object 
            // which holds a controller, inQueue, and replica
            Environment env;
            env = RecordLayer.generateEnvironment();
            env.getParameters().setInt("loglevel", rice.environment.logging.Logger.SEVERE);
            Material.buildCryptoMaterial(env, controller);
            serversConn = new PeerReviewTransport(controller, inQueue, prQueue, replica, env, isPRcopy);
            pr = ((PeerReviewTransport)serversConn).getPR();
            

        } else {
            serversConn = new ServersCommunicationLayer(controller, inQueue, replica);
        }

        //******* EDUARDO BEGIN **************//
        if (!isPRcopy)
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        //******* EDUARDO END **************//
    }

    //******* EDUARDO BEGIN **************//
    public void joinViewReceived() {
        serversConn.joinViewReceived();
    }

    public void updateServersConnections() {
        this.serversConn.updateConnections();
        if (clientsConn == null) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        }

    }

    //******* EDUARDO END **************//
    public void setAcceptor(Acceptor acceptor) {
        messageHandler.setAcceptor(acceptor);
    }

    public void setTOMLayer(TOMLayer tomLayer) {
        messageHandler.setTOMLayer(tomLayer);
    }

    public void setRequestReceiver(RequestReceiver requestReceiver) {
        if (clientsConn == null) {
            clientsConn = CommunicationSystemServerSideFactory.getCommunicationSystemServerSide(controller);
        }
        clientsConn.setRequestReceiver(requestReceiver);
    }

    /**
     * Thread method responsible for receiving messages sent by other servers.
     */
    @Override
    public void run() {
        if (isPRcopy) return ;
        long count = 0;
        if(controller.getStaticConf().peerreviewEnabled()) {
            while (doWork) {
                try {
                    if (count % 1000 == 0 && count > 0) {
                        logger.debug("After " + count + " messages, prQueue size=" + prQueue.size());
                    }
    
                    Pair<HandleImpl, ByteBuffer> buf = prQueue.poll(MESSAGE_WAIT_TIME, TimeUnit.MILLISECONDS);
    
                    if (buf != null) {
                        logger.debug("<-- receiving, msg:");
                        //System.out.println("<-- receiving, msg:");
                        try {
                            pr.messageReceived(buf.getLeft(), buf.getRight(), null);
                        } catch (IOException e) {
                            logger.error("Error processing message",e);
                        }
                        count++;
                    } else {                
                        messageHandler.verifyPending();               
                    }
    
                } catch (InterruptedException e) {
                    
                    logger.error("Error processing message",e);
                }
            }
        } else {
            while (doWork) {
                try {
                    if (count % 1000 == 0 && count > 0) {
                        logger.debug("After " + count + " messages, inQueue size=" + inQueue.size());
                    }
    
                    SystemMessage sm = inQueue.poll(MESSAGE_WAIT_TIME, TimeUnit.MILLISECONDS);
    
                    if (sm != null) {
                        logger.debug("<-- receiving, msg:" + sm);
                        messageHandler.processData(sm);
                        count++;
                    } else {                
                        messageHandler.verifyPending();               
                    }
    
                } catch (InterruptedException e) {
                    
                    logger.error("Error processing message",e);
                }
            }
        }
        
        logger.info("ServerCommunicationSystem stopped.");

    }

    /**
     * Send a message to target processes. If the message is an instance of 
     * TOMMessage, it is sent to the clients, otherwise it is set to the
     * servers.
     *
     * @param targets the target receivers of the message
     * @param sm the message to be sent
     */
    public void send(int[] targets, SystemMessage sm) {
        if (targets == null || targets.length == 0) { 
            return;
        }
        if (sm instanceof TOMMessage) {
            clientsConn.send(targets, (TOMMessage) sm, false);
        } else {
        	logger.debug("--> sending message from: {} -> {}" + sm.getSender(), targets);
            serversConn.send(targets, sm, true);
        }
    }

    public ServersCommunicationLayer getServersConn() {
        return serversConn;
    }
    
    public PeerReviewTransport getPRServersConn() {
        if(serversConn instanceof PeerReviewTransport)
            return (PeerReviewTransport)serversConn;
        return null;
    }
    public CommunicationSystemServerSide getClientsConn() {
        return clientsConn;
    }
    
    @Override
    public String toString() {
        return serversConn.toString();
    }
    
    public void shutdown() {
        
        logger.info("Shutting down communication layer");
        
        this.doWork = false;        
        clientsConn.shutdown();
        serversConn.shutdown();
    }
    
    public SecretKey getSecretKey(int id) {
		return serversConn.getSecretKey(id);
	}

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }
}
