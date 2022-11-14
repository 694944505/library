package bftsmart.accountability;

import bftsmart.consensus.messages.ConsensusMessage;
import bftsmart.consensus.messages.MessageFactory;
import bftsmart.consensus.Decision;
import bftsmart.tom.leaderchange.CertifiedDecision;
import bftsmart.tom.leaderchange.LCManager;
import java.security.SignedObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;


import org.bouncycastle.cms.DefaultCMSSignatureEncryptionAlgorithmFinder;

public class Accountability {
    private Logger logger;
    private boolean test = true;
    // <cid, decision>
    private final HashMap<Integer, CertifiedDecision> decisionMap = new HashMap<>();
    // <cid, proof>
    private final HashMap<Integer, List<ConsensusMessage>> checkMap = new HashMap<>();
    // <view number, view change messages>
    private final HashMap<Integer, HashSet<SignedObject>> LCMap =new HashMap<>();
    private final Lock checkLock = new ReentrantLock();

    private final MessageFactory factory;
    public Accountability(MessageFactory factory, Logger logger) {
        this.factory = factory;
        this.logger = logger;
    }

    /*
     * This method is called when a consensus instance is decided
     * @param cid consensus instance id
     * @param decision the decision of the consensus instance
     * @return conflict set message if there is a conflict, null otherwise
     */
     
    public Pair<ConsensusMessage, Vector<Integer>> addDecision(CertifiedDecision decision) {
        Vector<Integer> targets = new Vector<>();
        checkLock.lock();
        try {
            //System.out.println("addDecision: " + decision.getCID() + " " + checkMap.keySet());
            decisionMap.put(decision.getCID(), (CertifiedDecision) decision/* .clone() */);
            List<ConsensusMessage> lst = checkMap.get(decision.getCID());
            if (lst != null) {
                for (ConsensusMessage msg : lst) {
                    // TODO remove test
                    if (test||!Arrays.equals(msg.getValue(), decision.getDecision())) {
                        targets.add(msg.getSender());
                    }
                }
                checkMap.remove(decision.getCID());
            }
        } finally {
            checkLock.unlock();
        }
        ConsensusMessage conflictMessage = factory.createConflict(decision.getCID(), 0,
							decision.getDecision(), decision.getReg());
		conflictMessage.setProof(decision.getConsMessages());
        conflictMessage.setLCset(LCMap.get(decision.getReg()));
        if(targets.size()>0){
            return Pair.of(conflictMessage, targets);
        }
        else{
            return null;
        }
    }

    /*
     * This method is called when a check message is received
     * @param msg check message
     * @return conflict set message if there is a conflict, null otherwise
     */
    public Pair<ConsensusMessage, Vector<Integer>> addCheck(ConsensusMessage msg) {
        checkLock.lock();
        try {
            //System.out.println("addCheck: " + msg.getNumber() + " " + decisionMap.keySet());
            CertifiedDecision decision = decisionMap.get(msg.getNumber());
            if (decision != null) {
                if (test||!Arrays.equals(msg.getValue(), decision.getDecision())) {
                    ConsensusMessage conflictMessage = factory.createConflict(decision.getCID(), 0, decision.getDecision(),
					decision.getReg());
                    conflictMessage.setProof(decision.getConsMessages());
                    conflictMessage.setLCset(LCMap.get(decision.getReg()));
                    return Pair.of(conflictMessage, new Vector<>(Arrays.asList(msg.getSender())));
                }
            }
            if (checkMap.get(msg.getNumber()) == null) {
                checkMap.put(msg.getNumber(), new LinkedList<ConsensusMessage>());
            }
            checkMap.get(msg.getNumber()).add(msg);
        } finally {
            checkLock.unlock();
        }
        return null;
    }

    /*
     * get the decision which conflicts with the check message
     * @param msg check message
     * @return conflict decision if there is a conflict, null otherwise
     */
     
    public CertifiedDecision getCheck(ConsensusMessage msg) {
        // checkLock.lock();
        try {
            CertifiedDecision decision = decisionMap.get(msg.getNumber());
            if (decision != null) {
                if (test||!Arrays.equals(msg.getValue(), decision.getDecision())) {
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
        checkLock.lock();
        try {
            LCMap.put(reg, LC);
        } finally {
            checkLock.unlock();
        }
    }

}
