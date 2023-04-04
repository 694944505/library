package bftsmart.twins;


import bftsmart.demo.microbenchmarks.ThroughputLatencyClient;
import bftsmart.reconfiguration.util.Configuration;
import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.CommandsInfo;
import bftsmart.tom.server.defaultservices.DefaultRecoverable;
import bftsmart.tom.util.Storage;
import bftsmart.tom.util.TOMUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple server that just acknowledge the reception of a request.
 */
public final class Twins extends DefaultRecoverable{
    
    private int interval;
    private byte[] reply;
    private float maxTp = -1;
    private boolean context;
    private int signed;
    private int id;
    
    private byte[] state;
    
    private int iterations = 0;
    private long throughputMeasurementStartTime = System.currentTimeMillis();
            
    private Storage totalLatency = null;
    private Storage consensusLatency = null;
    private Storage preConsLatency = null;
    private Storage posConsLatency = null;
    private Storage proposeLatency = null;
    private Storage writeLatency = null;
    private Storage acceptLatency = null;
    
    private Storage batchSize = null;
    
    private ServiceReplica replica;
    
    private RandomAccessFile randomAccessFile = null;
    private FileChannel channel = null;

    public Twins(int id, int interval, int replySize, int stateSize, boolean context, int signed, Generator g, boolean hasTwins) {
        System.out.println("Starting Twins" + id +"...");
        this.id = id;
        this.interval = interval;
        this.context = context;
        this.signed = signed;
        
        this.reply = new byte[replySize];
        
        for (int i = 0; i < replySize ;i++)
            reply[i] = (byte) i;
        
        this.state = new byte[stateSize];
        
        for (int i = 0; i < stateSize ;i++)
            state[i] = (byte) i;

        totalLatency = new Storage(interval);
        consensusLatency = new Storage(interval);
        preConsLatency = new Storage(interval);
        posConsLatency = new Storage(interval);
        proposeLatency = new Storage(interval);
        writeLatency = new Storage(interval);
        acceptLatency = new Storage(interval);
        
        batchSize = new Storage(interval);
        
        replica = new ServiceReplica(id, this, this, g, hasTwins);
       
        try{
            Thread.sleep(10000);
        } catch(Exception e){

        }
        replica.initLeader();
        replica.getTOMLayer().randomPropose();
    }


    
    @Override
    public byte[][] appExecuteBatch(byte[][] commands, MessageContext[] msgCtxs, boolean fromConsensus) {
        try{
            Thread.sleep(100);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //System.out.println("delivering batch");
        replica.getTOMLayer().randomPropose();
        batchSize.store(commands.length);
                
        byte[][] replies = new byte[commands.length][];
        
                    
        return replies;
    }
    
    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return execute(command,msgCtx);
    }
    
    public byte[] execute(byte[] command, MessageContext msgCtx) {
        
        ByteBuffer buffer = ByteBuffer.wrap(command);
        int l = buffer.getInt();
        byte[] request = new byte[l];
        buffer.get(request);
        l = buffer.getInt();
        byte[] signature = new byte[l];
        
        buffer.get(signature);
        Signature eng;
        
        try {
            
            if (signed > 0) {
                
                if (signed == 1) {
                    
                    eng = TOMUtil.getSigEngine();
                    eng.initVerify(replica.getReplicaContext().getStaticConfiguration().getPublicKey());
                } else {
                
                    eng = Signature.getInstance("SHA256withECDSA", "SunEC");
                    Base64.Decoder b64 = Base64.getDecoder();
                    CertificateFactory kf = CertificateFactory.getInstance("X.509");
                
                    byte[] cert = b64.decode(ThroughputLatencyClient.pubKey);
                    InputStream certstream = new ByteArrayInputStream (cert);
                
                    eng.initVerify(kf.generateCertificate(certstream));
                    
                }
                eng.update(request);
                if (!eng.verify(signature)) {
                    
                    System.out.println("Client sent invalid signature!");
                    System.exit(0);
                }
            }
            
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | CertificateException ex) {
            ex.printStackTrace();
            System.exit(0);
        } catch (NoSuchProviderException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        
        boolean readOnly = false;
        
        iterations++;

        if (msgCtx != null && msgCtx.getFirstInBatch() != null) {
            

            readOnly = msgCtx.readOnly;
                    
            msgCtx.getFirstInBatch().executedTime = System.nanoTime();
                        
            totalLatency.store(msgCtx.getFirstInBatch().executedTime - msgCtx.getFirstInBatch().receptionTime);

            if (readOnly == false) {

                consensusLatency.store(msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().consensusStartTime);
                long temp = msgCtx.getFirstInBatch().consensusStartTime - msgCtx.getFirstInBatch().receptionTime;
                preConsLatency.store(temp > 0 ? temp : 0);
                posConsLatency.store(msgCtx.getFirstInBatch().executedTime - msgCtx.getFirstInBatch().decisionTime);            
                proposeLatency.store(msgCtx.getFirstInBatch().writeSentTime - msgCtx.getFirstInBatch().consensusStartTime);
                writeLatency.store(msgCtx.getFirstInBatch().acceptSentTime - msgCtx.getFirstInBatch().writeSentTime);
                acceptLatency.store(msgCtx.getFirstInBatch().decisionTime - msgCtx.getFirstInBatch().acceptSentTime);
                

            } else {
            
           
                consensusLatency.store(0);
                preConsLatency.store(0);
                posConsLatency.store(0);            
                proposeLatency.store(0);
                writeLatency.store(0);
                acceptLatency.store(0);
                
                
            }
            
        } else {
            
            
                consensusLatency.store(0);
                preConsLatency.store(0);
                posConsLatency.store(0);            
                proposeLatency.store(0);
                writeLatency.store(0);
                acceptLatency.store(0);
                
               
        }
        
        float tp = -1;
        

        return reply;
    }

    public static void main(String[] args){
        /*if(args.length < 4) {
            System.out.println("Usage: ... Twins <number of replicas> <number of twins> <number of partitions> <number of views> <context?>");
            System.exit(-1);
        }
        int replicaNumber =Integer.parseInt(args[0]);
        int twinsNumber = Integer.parseInt(args[1]);
        int partitionsNumber = Integer.parseInt(args[2]);
        int viewsNumber = Integer.parseInt(args[3]);
        boolean context = Boolean.parseBoolean(args[4]);*/
        int replicaNumber = 4;
        int twinsNumber = 2;
        int partitionsNumber = 2;
        int viewsNumber = 12;
        boolean context = true;

        Configuration.configFileName = "twins.config";
        Configuration.hostsFileName = "twinshosts.config";
        Generator g = new Generator(new Settings(replicaNumber, twinsNumber, partitionsNumber, viewsNumber), LoggerFactory.getLogger("twins_test"));
        g.Shuffle(0);
        g.setQuik();
        
        int s = 0;
        for (NodeID node: g.getNodes()){
            new Thread(() -> {
                try {
                    System.out.println("Starting replica " + node);
                    new Twins(node.getNetworkID(), 0, 0, 0, context, s, g, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        for (NodeID node: g.getTwins()){
            new Thread(() -> {
                try {
                    System.out.println("Starting replica " + node);
                    new Twins(node.getNetworkID(), 0, 0, 0, context, s, g, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
               
    }

    @Override
    public void installSnapshot(byte[] state) {
        //nothing
    }

    @Override
    public byte[] getSnapshot() {
        return this.state;
    }

   
}

