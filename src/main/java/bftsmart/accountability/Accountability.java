package bftsmart.accountability;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.reconfiguration.ServerViewController;
import bftsmart.communication.ServerCommunicationSystem;
import bftsmart.consensus.Decision;
import bftsmart.consensus.Epoch;
import bftsmart.tom.core.ExecutionManager;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.leaderchange.LCManager;
import bftsmart.tom.util.TOMUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignedObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;




public class Accountability {
    private Logger logger;
    private boolean test = false;
    // <cid, decision>
    private final HashMap<Integer, CertifiedDecision> myDecisionMap = new HashMap<>();
    // <cid, proof>
    private final HashMap<Integer, HashSet<ConsensusMessage>> otherDecisionMap = new HashMap<>(); 
    private final HashMap<Integer, HashSet<SignedObject>> LCMap =new HashMap<>();
    private final Lock checkLock = new ReentrantLock();

    private final MessageFactory factory;
    private final ServerCommunicationSystem communication;
    private int detectFrom = 0;
    private int detecTo = 0;
    private ServerViewController controller;
    private ExecutorService proofExecutor;
    private PrivateKey privKey;
    public Accountability(MessageFactory factory, ServerCommunicationSystem communication, ServerViewController controller, ExecutorService proofExecutor, Logger logger) {
        this.factory = factory;
        this.logger = logger;
        this.communication = communication;
        this.controller = controller;
        this.proofExecutor = proofExecutor;
        this.privKey = controller.getStaticConf().getPrivateKey();
    }

    /*
     * This method is called when a consensus instance is decided
     * @param cid consensus instance id
     * @param decision the decision of the consensus instance
     * @return conflict set message if there is a conflict, null otherwise
     */
     
    public void addDecision(CertifiedDecision decision, Epoch epoch) {
        Vector<ConsensusMessage> confilDecisions = new Vector<>();
        checkLock.lock();
        try {
            //System.out.println("addDecision: " + decision.getCID() + " " + checkMap.keySet());
            myDecisionMap.put(decision.getCID(), (CertifiedDecision) decision/* .clone() */);
            HashSet<ConsensusMessage> otherDecision = otherDecisionMap.get(decision.getCID());
            if (otherDecision != null) {
                for(ConsensusMessage msg : otherDecision){
                    if(test || !Arrays.equals(msg.getValue(), decision.getDecision())){
                        confilDecisions.add(msg);
                    }
                }
            }
        } finally {
            checkLock.unlock();
        }
        for (ConsensusMessage msg : confilDecisions) {
            checkConflict(decision, msg);
        }
        
        // send check message
        byte [] praentValue = getParentValue(decision);
        detectFrom = detecTo;
        detecTo = controller.getStaticConf().getFaultDetectServerBound(decision.getCID());
        proofExecutor.submit(() -> {
            ConsensusMessage check = factory.createCheck(decision.getCID(), epoch.getTimestamp(), epoch.propValueHash, praentValue, decision.getReg());
            // Create a cryptographic proof for this CHECK message
            logger.debug(
                    "Creating cryptographic proof for the correct CHECK message from consensus " + decision.getCID());
            insertProof(check);

            communication.send(this.controller.getCurrentViewCheckers(detectFrom, detecTo), check);

        });
        
    }
    

    private void checkConflict(CertifiedDecision decision, ConsensusMessage msg){
        if (test || Arrays.equals(msg.getParentValue(), getParentValue(decision))) {
            ConsensusMessage conflictMessage = factory.createConflict(decision.getCID(), 0, decision.getDecision(),
                    decision.getReg());
            conflictMessage.setProof(decision.getConsMessages());
            conflictMessage.setLCset(LCMap.get(decision.getReg()));
            communication.send(new int[]{msg.getSender()}, conflictMessage);
        } else {
            // send check message
            CertifiedDecision lastDecision = myDecisionMap.get(decision.getCID()-1);
            byte [] praentValue = getParentValue(lastDecision);
            proofExecutor.submit(() -> {
                ConsensusMessage check = factory.createCheck(lastDecision.getCID(), 0, lastDecision.getDecision(), praentValue, lastDecision.getReg());
                // Create a cryptographic proof for this CHECK message
                logger.debug(
                        "Creating cryptographic proof for the correct CHECK message from consensus " + decision.getCID());
                insertProof(check);
    
                communication.send(new int[]{msg.getSender()}, check);
    
            });
        }
    }
    private byte[] getParentValue(CertifiedDecision decision) {
        if (decision.getCID() == 0) {
            return new byte[0];
        }
        CertifiedDecision lastDecision = myDecisionMap.get(decision.getCID()-1);
        if (lastDecision == null) {
            logger.error("last decision is null");
            return new byte[0];
        }
        return lastDecision.getDecision();
    }
    private void insertProof(ConsensusMessage cm) {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream(248);
		try {
			ObjectOutputStream obj = new ObjectOutputStream(bOut);
			obj.writeInt(cm.getNumber());
            obj.write(cm.getValue());
            obj.write(cm.getParentValue());
			obj.flush();
			bOut.flush();
		} catch (IOException ex) {
			logger.error("Failed to serialize consensus message", ex);
		}

		byte[] data = bOut.toByteArray();

		// Always sign a consensus proof.
		byte[] signature = TOMUtil.signMessage(privKey, data);
		cm.setProof(signature);
	}
    private boolean hasValidProof(ConsensusMessage msg) {
        if (!(msg.getProof() instanceof byte[])) return false;
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(248);
        try {
            ObjectOutputStream obj = new ObjectOutputStream(bOut);
			obj.writeInt(msg.getNumber());
            obj.write(msg.getValue());
            obj.write(msg.getParentValue());
			obj.flush();
			bOut.flush();
        } catch (IOException ex) {
            logger.error("Could not serialize message",ex);
        }

        byte[] data = bOut.toByteArray();
        byte[] signature = (byte[]) msg.getProof();
        PublicKey pubKey = controller.getStaticConf().getPublicKey(msg.getSender());
        return TOMUtil.verifySignature(pubKey, data, signature);
    }
    /*
     * This method is called when a check message is received
     * @param msg check message
     * @return conflict set message if there is a conflict, null otherwise
     */
    public void addCheck(ConsensusMessage msg) {
        checkLock.lock();
        try {
            if (!hasValidProof(msg)) {
                logger.error("Invalid proof for check message from " + msg.getSender());
                return;
            }
            HashSet<ConsensusMessage> otherDecision = otherDecisionMap.get(msg.getNumber());
            if (otherDecision == null) {
                otherDecision = new HashSet<>();
                otherDecisionMap.put(msg.getNumber(), otherDecision);
            } 

            if(otherDecision.add(msg)){
                int count = 0;
                for (ConsensusMessage m : otherDecision) {
                    if (test || !Arrays.equals(msg.getValue(), m.getValue())) {
                        if (++count == otherDecision.size()) {
                            break;
                        }
                        communication.send(new int[]{m.getSender()}, msg);
                        communication.send(new int[]{msg.getSender()}, m);
                    }
                }
            }
            CertifiedDecision decision = myDecisionMap.get(msg.getNumber());

            if (decision != null) {
                if (test || !Arrays.equals(msg.getValue(), decision.getDecision())) {
                    checkConflict(decision, msg);
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        } finally {
            checkLock.unlock();
        }
    }

    /*
     * get the decision which conflicts with the check message
     * @param msg check message
     * @return conflict decision if there is a conflict, null otherwise
     */
     
    public CertifiedDecision getCheck(ConsensusMessage msg) {
        // checkLock.lock();
        try {
            CertifiedDecision decision = myDecisionMap.get(msg.getNumber());
            if (decision != null) {
                if (test || !Arrays.equals(msg.getValue(), decision.getDecision())) {
                    return decision;
                }
            }
        } finally {
            // checkLock.unlock();
        }
        return null;
    }

    /*
     * get the view change messages of a view
     * @param view view number
     * @return view change messages of the view
     */
    public HashSet<SignedObject> getLCset(int reg){
        return LCMap.get(reg);
    }

    /*
     * This method is called when a view change message is received
     * @param msg view change message
     *
     */
    public void addLC(int reg, HashSet<SignedObject> LC) {
        System.out.println("addLC: " + reg + " " );
        checkLock.lock();
        try {
            LCMap.put(reg, LC);
        } finally {
            checkLock.unlock();
        }
    }

}
